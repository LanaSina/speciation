package communication;

import animals.EmbodiedIndividual;
import animals.Individual;
import animals.Node;
import animals.Tree;
import startup.Constants;
import visualization.Display;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.abs;

public class RealMap extends Map {


	public RealMap(int mapSize,
				   Display d,
				   String myDataFolderName,
				   String summaryFileName,
				   String predationFileName,
				   String snapshotFileName,
				   String sensorsFileName) {
		super(mapSize, d, myDataFolderName, summaryFileName, predationFileName, snapshotFileName, sensorsFileName);
	}

	/**
	 * Updates the creatures present on the cell specified by the coordinates.
	 * Creatures may make babies, move, interact with other creatures present on the cell.
	 * They can lose or gain energy.
	 * If their energy reaches 0 or less, they die.
	 * </br>
	 * Creatures can only interact with one another if they are on the same cell.
	 * The more creatures there are on the cell, the more likely two given creatures are to interact with each other.
	 * (If the number of creatures on the cell is sufficiently low, then no interactions will even occur.)
	 * </br>
	 * Interactions are limited to predation.
	 * If a creature detects another creature with at least one similar characteristic (e.g., size), it may attempt to
	 * eat it.
	 *
	 * @param x the abscissa of the cell to update
	 * @param y the ordinate of the cell to update
	 */
	@Override
	public void updateCell(int x, int y){
		
		Cell c = map[x][y];
		int size = c.creatures.size();

		if(size==0){
			return;
		}

		List<Integer> shuffled_creatures_arr = IntStream.range(0, size).boxed().collect(Collectors.toList());
		Collections.shuffle(shuffled_creatures_arr, Constants.rand);

		for (int temp_i = 0; temp_i < size; temp_i++) {
			int i = shuffled_creatures_arr.get(temp_i);

			EmbodiedIndividual creature = (EmbodiedIndividual) c.creatures.get(i);
            boolean alive = creature.update(babies, time, c.transparency, cst_mut_factor, cst_speed_max,
					cst_light_birth_dst, cst_birth_dst, cst_grid_max, cst_energy_max, cst_speed_cost,
					cst_sensor_cost, cst_free_energy, cst_energy_cost_factor, cst_step_cost
			);
            double[] position = creature.getPosition();

            if(!alive){
            	remove.add(creature);
            } else{
            	double[] np = new double[2];
	            boolean moved = false;
				double speed = creature.getSpeed();

            	if(!creature.isLight()){
	            	//move
	        		for(int j=0;j<2;j++){
	        			if(generateBool()){
	        				np[j]= position[j]+(speed*cst_speed_factor);
	        			}else{
	        				np[j] = position[j]-(speed*cst_speed_factor);
	        			}
	        			if(np[j]<0) np[j]=0;
	        			if(np[j]>=cst_grid_max-1) np[j] = cst_grid_max-2;
	        			if(np[j] != position[j]){
	        				moved = true;
	        			}
	        		}
            	}
            	
        		Tree sensors = creature.getSensors();
        		
        		//interactions between creatures
                //iterate on properties
                HashMap<Integer, Node> sChildren = sensors.properties;//.getChildren();
				for (Iterator<Integer> propIt = sChildren.keySet().iterator(); propIt.hasNext();){
                	// property
					int k = propIt.next();
					// sensed values
                	Node propValues = sChildren.get(k);
                	for (Iterator<Integer> valuesIt = propValues.getChildren().keySet().iterator(); valuesIt.hasNext();){
						int valueSensed = valuesIt.next();
						int action = propValues.getChildren().get(valueSensed);
						//interactions with other creatures
						if (action < 2) {
							//iterate creatures on this cell
							for (int m = 0; m < c.creatures.size(); m++) {
								double p = 1 * 3 / (double) c.creatures.size();
								if (Constants.uniformDouble() > p) {
									continue;
								}

								Individual cr2 = c.creatures.get(m);
								if (remove.contains(c.creatures.get(m)) | (cr2.isLight())) {
									continue;
								}
								//creature can't interact on itself
								if (m == i) {
									continue;
								}

								double ind_prop = cr2.getProperties()[k];

								if ((valueSensed >= ind_prop - 5) && (valueSensed <= ind_prop + 5)) {
									tryEat(creature, cr2);
								}
							}
						}
                	}
                }
        		
        		if(moved){
        			newPositions.add(np[0]);
        			newPositions.add(np[1]);
        			moving.add(creature);
        			//costs energy
        			if(!creature.isLight()){
        				double energy = creature.getEnergy() - speed*cst_speed_cost;// - numberActions*Constants.ActionCost;
        				creature.setEnergy(energy);
        			}
        		}
            }    
        }
	}

	private void tryEat(Individual predator, Individual prey) {

		double ok = predator.getEnergy() - prey.getEnergy();
		if(ok>=0){
			//give energy to predator
			double e = prey.getEnergy();
			if(e>0){
				double energy = predator.getEnergy() + e;
				predator.setEnergy(energy);
				//record prey as dead
				prey.setEnergy(0);
				prey.setEatenBy(predator.getID());
				predator.setBorderColor(Color.black);
			}
			// only save successful predation
			if(Constants.Save){
				// reduce file size
				if(Constants.uniformDouble()<1){ //0.01
					EmbodiedIndividual ei_prey = (EmbodiedIndividual) prey;
					EmbodiedIndividual ei_pred = (EmbodiedIndividual) predator;
					/*
						String header_predation = "t, pred_id, pos[0], pos[1], pred_is_light," +
						"pred_lifeSpan, pred_speed, pred_maxEnergy, pred_kidEnergy," +
						"pred_sensors, pred_nkids, pred_pgmDeath, pred_matForKids,energy, parentIsLight " +
						 prey_id +  pos[0], pos[1] +prey_islight +
						"prey_lifeSpan, prey_speed, prey_maxEnergy, prey_kidEnergy, prey_sensors, prey_ancestor, prey_nkids," +
						"prey_pgmDeath, prey_matForKids, energy, parentIsLight\n";
					 */
					String str = time + "," + ei_pred.stringDesc() + "," + ei_prey.stringDesc() + "\n";
					try {
						predationWriter.append(str);
						predationWriter.flush();
					} catch (IOException ep) {
						// TODO Auto-generated catch block
						ep.printStackTrace();
					}
				}
			}
		} else if(ok<=0){
			double ePred = predator.getEnergy();
			double ePrey = prey.getEnergy();
			//wound predator
			double energy = ePred-abs(ePrey*cst_error_cost);
			predator.setEnergy(energy);
			//wound prey
			energy = ePrey-abs(ePred*cst_error_cost);//*3
			prey.setEnergy(energy);
			predator.setBorderColor(Color.red);
			prey.setBorderColor(Color.gray);
		}
	}

	/**
	 * Returns the number of new creatures born during a time step.
	 * </br>
	 * This method only works between a call to <code>updateCell()</code> and a call to <code>updateMoved()</code>,
	 * because this is the only moment where the new creatures are gathered in a single data structure (i.e.
	 * <code>babies</code>) and can be counted.
	 *
	 * @return the number of new creatures born during a time step
	 */
	public int getNbOfBirths() {
		return babies.size();
	}

	/**
	 * Returns the number of creatures that died during a time step.
	 * </br>
	 * This method only works between a call to <code>updateCell()</code> and a call to <code>updateMoved()</code>,
	 * because this is the only moment where the creatures that just died are gathered in a single data structure (i.e.
	 * <code>remove</code>) and can be counted.
	 *
	 * @return the number of creatures that just died during a time step
	 */
	public int getNbOfDeaths() {
		return remove.size();
	}

	@Override
	protected MyLog createMyLog() {
		return new MyLog("map", true);
	}
}
