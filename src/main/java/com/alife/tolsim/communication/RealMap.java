package com.alife.tolsim.communication;

import com.alife.tolsim.animals.*;
import com.alife.tolsim.startup.Constants;
import com.alife.tolsim.visualization.Display;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.abs;


public class RealMap extends Map {

	/** Predation recording. */
	FileWriter predationWriter;
	String predationFileName;

	/** Population recording */
	FileWriter populationWriter;
	String populationFileName;

	/**graphic panel*/
	Display d;
	/** 2D map is made of cells, in each cell there are creatures;*/
	Cell[][] map;
	/** width and length are the same*/
	int size;
	int cst_grid_max;

	//for updates
	//for new ones
	ArrayList<EmbodiedIndividual> babies;
	//for dead ones
	java.util.Map<Long, EmbodiedIndividual> remove;
	//for moved ones
	ArrayList<EmbodiedIndividual> moving;
	ArrayList<Double> newPositions;

	public RealMap(int mapSize,
				   Display d,
				   String myDataFolderName,
				   String summaryFileName,
				   String predationFileName,
				   String snapshotFileName,
				   String sensorsFileName,
				   String populationFileName) {
		super(myDataFolderName, summaryFileName, snapshotFileName, sensorsFileName);
		this.predationFileName = predationFileName;
		this.populationFileName = populationFileName;
		this.d = d;
		size = mapSize;
		//create map
		map = new Cell[size][size];
		//create cells
		for(int i=0;i<size;i++){
			for(int j=0;j<size;j++){
				map[i][j] = new Cell();
			}
		}
		//for new ones
		babies = new ArrayList<>();
		//for dead ones
		remove = new HashMap<>();
		//for moved ones
		moving = new ArrayList<>();
		newPositions = new ArrayList<>();

		// read configuration file
		Properties properties = new Properties();
		InputStream propsFile;
		try {
			propsFile = getClass().getClassLoader().getResourceAsStream("config.properties");
			properties.load(propsFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		cst_mut_factor = Double.parseDouble(properties.getProperty("mut_factor"));
		cst_speed_factor = Double.parseDouble(properties.getProperty("speed_factor"));
		cst_speed_max = Integer.parseInt(properties.getProperty("speed_max"));
		cst_light_birth_dst = Double.parseDouble(properties.getProperty("light_birth_dst"));
		cst_birth_dst = Double.parseDouble(properties.getProperty("birth_dst"));
		cst_grid_max= Integer.parseInt(properties.getProperty("grid_max"));
		cst_energy_max = Integer.parseInt(properties.getProperty("energy_max"));
		cst_speed_cost = Double.parseDouble(properties.getProperty("speed_cost"));
		cst_sensor_cost = Double.parseDouble(properties.getProperty("sensor_cost"));
		cst_error_cost = Double.parseDouble(properties.getProperty("error_cost"));
		cst_free_energy = Integer.parseInt(properties.getProperty("free_energy"));
		cst_sensor_precision = Integer.parseInt(properties.getProperty("sensor_precision"));
		cst_max_number_actions = Integer.parseInt(properties.getProperty("max_number_actions"));
		cst_energy_cost_factor = Double.parseDouble(properties.getProperty("energy_cost_factor"));
		cst_step_cost = Double.parseDouble(properties.getProperty("step_cost"));
	}

	public void setupLogFiles(){
		// individuals info
		FileBuilder fb = new FileBuilder(dataFolderName, summaryFileName + "_" + time);
		summaryWriter = fb.getFileWriter();
		fb = null;

		//csv file header
			/*
			String description =  ID +","+parentID+","+birthDate+","+life+","
				+ speed+","+maxEnergy+","+ getKidEnergy()+","
				+ hasSensors() +","+ getAncestor() + "," + getNKids() + ","
				+ death + ","+ matForKids ;
			 */
		String str = "ID,pos_x,pos_y,isLight,parent,created,lifeSpan,speed,maxEnergy," +
				"kidEnergy,sensors,ancestor,nkids,pgmDeath,matForKids,energy,parentIsLight"+"\n";
		try {
			summaryWriter.append(str);
			summaryWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// predation info
		FileBuilder fb_predation = new FileBuilder(dataFolderName, predationFileName+ "_" + time);
		predationWriter = fb_predation.getFileWriter();
		fb_predation = null;

			/*
			String description =  ID +","+parentID+","+birthDate+","+life+","
				+ speed+","+maxEnergy+","+ getKidEnergy()+","
				+ hasSensors() +","+ getAncestor() + "," + getNKids() + ","
				+ death + ","+ matForKids ;
			 */
		String header_predation = "t, pred_id, pred_pos_x, pred_pos_y, pred_isLight," +
				"pred_parent, pred_created, pred_lifeSpan," +
				"pred_speed, pred_maxEnergy, pred_kidEnergy, pred_sensors," +
				"pred_ancestor, pred_nkids, pred_pgmDeath, pred_matForKids, pred_energy, pred_parentIsLight" +
				"prey_id, prey_pos_x, prey_pos_y, prey_isLight," +
				"prey_parent, prey_created, prey_lifeSpan," +
				"prey_speed, prey_maxEnergy, prey_kidEnergy, prey_sensors," +
				"prey_ancestor, prey_nkids, prey_pgmDeath, prey_matForKids, prey_energy, prey_parentIsLight" +
				"\n";

		try {
			predationWriter.append(header_predation);
			predationWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// population count (we use one single file during the whole run)
		if (populationWriter == null) {
			FileBuilder fb_population = new FileBuilder(dataFolderName, populationFileName);
			populationWriter = fb_population.getFileWriter();
			String headerPopulation = "t,population\r\n";
			try {
				populationWriter.append(headerPopulation);
				populationWriter.flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void addIndividual(int x, int y, EmbodiedIndividual i){
		Cell c = map[x][y];
		synchronized (i) {
			synchronized (c) {
				i.setCellTransparency(c.transparency);
			}
		}
		synchronized (c) {
			c.creatures.put(i.getID(), i);
		}
	}

	public void removeIndividual(EmbodiedIndividual i) {
		synchronized (i) {
			double[] position = i.getPosition();
			int x = (int)(position[0] +0.5);
			int y = (int)(position[1] +0.5);
			Cell c = map[x][y];
			synchronized (c) {
				c.creatures.remove(i.getID());
			}
		}
	}

	/**
	 * @param x2 new x
	 * @param y2 nex y
	 * @param i individual
	 */
	public void updatePosition(int x2, int y2, EmbodiedIndividual i){
		removeIndividual(i);
		addIndividual(x2,y2,i);
	}

	/**
	 * <p>
	 *      Updates the creatures present on the cell specified by the coordinates.
	 *      Creatures may make babies, move, interact with other creatures present on the cell.
	 *      They can lose or gain energy.
	 *      If their energy reaches 0 or less, they die.
	 * </p>
	 * <p>
	 *      Creatures can only interact with one another if they are on the same cell.
	 *      The more creatures there are on the cell, the more likely two given creatures are to interact with each other.
	 *      (If the number of creatures on the cell is sufficiently low, then no interactions will even occur.)
	 * </p>
	 * <p>
	 *      Interactions are limited to predation.
	 *      If a creature detects another creature with at least one similar characteristic (e.g., size), it may attempt to
	 *      eat it.
	 * </p>
	 *
	 * @param x the abscissa of the cell to update
	 * @param y the ordinate of the cell to update
	 */
	public void updateCell(int x, int y){
		
		Cell c = map[x][y];
		int size = c.creatures.size();

		if(size==0){
			return;
		}

		List<EmbodiedIndividual> shuffledCreatures = new LinkedList<>(c.creatures.values());
		Collections.shuffle(shuffledCreatures, Constants.rand);

		for (EmbodiedIndividual creature : shuffledCreatures) {

            boolean alive = creature.update(babies, time, c.transparency, cst_mut_factor, cst_speed_max,
					cst_light_birth_dst, cst_birth_dst, cst_grid_max, cst_energy_max, cst_speed_cost,
					cst_sensor_cost, cst_free_energy, cst_energy_cost_factor, cst_step_cost
			);

            if(!alive){
            	remove.put(creature.getID(), creature);
				continue;
            }

            double[] position = creature.getPosition();
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
			for (int k : sChildren.keySet()) {
				// property
				// sensed values
				Node propValues = sChildren.get(k);
				for (int valueSensed : propValues.getChildren().keySet()) {
					int action = propValues.getChildren().get(valueSensed);
					//interactions with other creatures
					if (action < 2) {
						//iterate creatures on this cell
						for (EmbodiedIndividual otherCreature : c.creatures.values()) {
							double p = 1 * 3 / (double) c.creatures.size();
							if (Constants.uniformDouble() > p) {
								continue;
							}
							if (remove.containsKey(otherCreature.getID()) | (otherCreature.isLight())) {
								continue;
							}
							//creature can't interact on itself
							if (otherCreature == creature) {
								continue;
							}

							double ind_prop = otherCreature.getProperties()[k];

							if ((valueSensed >= ind_prop - 5) && (valueSensed <= ind_prop + 5)) {
								tryEat(creature, otherCreature);
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

	private void tryEat(EmbodiedIndividual predator, EmbodiedIndividual prey) {

		double ok = predator.getEnergy() - prey.getEnergy();
		if(ok>=0){
			//give energy to predator
			if(prey.getEnergy() > 0){
				double energy = predator.getEnergy() + prey.getEnergy();
				predator.setEnergy(energy);
				//record prey as dead
				prey.setEnergy(0);
				prey.setEatenBy(predator.getID());
				predator.setBorderColor(Color.black);
			}
			// only save successful predation
			if(Constants.Save && Constants.uniformDouble() <= Constants.SaveCoarse){
				/*
					String header_predation = "t, pred_id, pos[0], pos[1], pred_is_light," +
					"pred_lifeSpan, pred_speed, pred_maxEnergy, pred_kidEnergy," +
					"pred_sensors, pred_nkids, pred_pgmDeath, pred_matForKids,energy, parentIsLight " +
					 prey_id +  pos[0], pos[1] +prey_islight +
					"prey_lifeSpan, prey_speed, prey_maxEnergy, prey_kidEnergy, prey_sensors, prey_ancestor, prey_nkids," +
					"prey_pgmDeath, prey_matForKids, energy, parentIsLight\n";
				 */
				String str = time + "," + predator.stringDesc() + "," + prey.stringDesc() + "\n";
				try {
					predationWriter.append(str);
					predationWriter.flush();
				} catch (IOException e) {
					throw new RuntimeException(e);
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
	 * <p>
	 *      Returns the number of new creatures born during a time step.
	 * </p>
	 * <p>
	 *      This method only works between a call to <code>updateCell()</code> and a call to <code>applyChanges()</code>,
	 *      because this is the only moment where the new creatures are gathered in a single data structure (i.e.
	 *      <code>babies</code>) and can be counted.
	 * </p>
	 *
	 * @return the number of new creatures born during a time step
	 */
	public int getNbOfBirths() {
		return babies.size();
	}

	/**
	 * <p>
	 *      Returns the number of creatures that died during a time step.
	 * </p>
	 * <p>
	 *      This method only works between a call to <code>updateCell()</code> and a call to <code>applyChanges()</code>,
	 *      because this is the only moment where the creatures that just died are gathered in a single data structure (i.e.
	 *      <code>remove</code>) and can be counted.
	 * </p>
	 *
	 * @return the number of creatures that just died during a time step
	 */
	public int getNbOfDeaths() {
		return remove.size();
	}


	public void applyChanges(){
		Thread[] theThreads = new Thread[Constants.NbThreads];

		// remove dead
		List<EmbodiedIndividual> removeAsList = new ArrayList<>(remove.values());
		for (int threadNumber = 0; threadNumber < Constants.NbThreads; threadNumber++) {
			theThreads[threadNumber] = new Thread(new ThreadRemoveDead(this, removeAsList, threadNumber));
			theThreads[threadNumber].start();
		}
		for (int threadNumber = 0; threadNumber < Constants.NbThreads; threadNumber++) {
            try {
                theThreads[threadNumber].join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

		//update moved
		for (int threadNumber = 0; threadNumber < Constants.NbThreads; threadNumber++) {
			theThreads[threadNumber] = new Thread(new ThreadUpdateMoving(this, moving, threadNumber));
			theThreads[threadNumber].start();
		}
		for (int threadNumber = 0; threadNumber < Constants.NbThreads; threadNumber++) {
			try {
				theThreads[threadNumber].join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		//add babies
		for (int threadNumber = 0; threadNumber < Constants.NbThreads; threadNumber++) {
			theThreads[threadNumber] = new Thread(new ThreadAddBabies(this, babies, threadNumber));
			theThreads[threadNumber].start();
		}
		for (int threadNumber = 0; threadNumber < Constants.NbThreads; threadNumber++) {
			try {
				theThreads[threadNumber].join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		//clear
		//for new ones
		babies.clear();
		//for dead ones
		remove.clear();
		//for moved ones
		moving.clear();
		newPositions.clear();
	}

	/**
	 * <p>
	 *     Saves the number of agents currently alive in this map.
	 * </p>
	 * <p>
	 *     Time complexity: &Theta;(n) where n is the population.
	 * </p>
	 */
	public void savePopulation() {
		try {
			String str = time + "," + getAllIndividuals().size() + "\r\n";
			populationWriter.append(str);
			populationWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException("Failed to write the population", e);
		}
	}

	void saveCreatures(String dataFolderName, String filePath){
		//open file for creatures
		FileBuilder fb = new FileBuilder(dataFolderName, filePath);
		FileWriter stateWriter = fb.getFileWriter();
		fb = null;


		try {
			// save time
			String str = "time\n"+ time + "\n";
			stateWriter.append(str);
			stateWriter.flush();
			//csv file header
			/*
			"ID,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor,nkids,pgmDeath,matForKids"+"\n";
			 */
			str = "x,y,ID,pos_x,pos_y,isLight,parent,created,lifeSpan,speed,maxEnergy,kidEnergy," +
					"sensors,ancestor,nkids,pgmDeath,matForKids,energy,parentIsLight"+"\n";

			stateWriter.append(str);
			stateWriter.flush();

			for(int x=0; x<map.length;x++) {
				for (int y = 0; y < map[0].length; y++) {
					Cell c = map[x][y];
					int size = c.creatures.size();

					if(size==0){
						continue;
					}
					// mlog.say("snapshot " + x + " " + y + " " + size);
					//debugstr = debugstr + size + " ";

					for (EmbodiedIndividual creature : c.creatures.values()){
						String astr = x + "," + y + "," + creature.stringDesc() + "\n";
						stateWriter.append(astr);
						stateWriter.flush();
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * <p>
	 *      Saves the current state of the map.
	 * </p>
	 * <p>
	 *      Creates a new directory of format <code>dd_HH_mm</code> and puts in it :
	 *      <ul>
	 *          <li>a snapshot file (which contains information about the creatures)</li>
	 *          <li>a sensors file</li>
	 *      </ul>
	 * </p>
	 *
	 * @param dataFolderName the directory of the current simulation
	 */
	public void saveState(String dataFolderName) {
		//snapshot time
		DateFormat dateFormat = new SimpleDateFormat("dd_HH_mm_ss");
		Date date = new Date();
		String strDate = dateFormat.format(date);

		File theDir = new File(dataFolderName+"/"+strDate);
		// if the directory does not exist, create it
		if (!theDir.exists()) {
			mlog.say("creating directory: " + dataFolderName);
			try{
				theDir.mkdir();
				System.out.println("DIR created");
			}
			catch(SecurityException e){
				throw new RuntimeException(e);
			}
		}


		String filePath = strDate + "/" + snapshotFileName;
		saveCreatures(dataFolderName, filePath);
		// save sensors
		filePath = strDate + "/" + sensorsFileName;
		saveSensors(dataFolderName, filePath);

		String savedAt = dataFolderName + filePath;
		mlog.say("Saved at " + savedAt);
	}

	void saveSensors(String dataFolderName, String filePath){
		//open file for creatures
		FileBuilder fb = new FileBuilder(dataFolderName, filePath);
		FileWriter stateWriter = fb.getFileWriter();
		fb = null;

		//csv file header
		StringBuilder str = new StringBuilder("creatureID,sensorId,sensorValue,action" + "\n");
		//String debugstr = "";
		try {
			stateWriter.append(str.toString());
			stateWriter.flush();

            for (Cell[] cells : map) {
                for (int y = 0; y < map[0].length; y++) {
                    Cell c = cells[y];
                    int size = c.creatures.size();

                    if (size == 0) {
                        continue;
                    }

                    for (IndividualWithProperties creature : c.creatures.values()) {
                        if (creature.hasSensors() == 0)
                            continue;

                        str = new StringBuilder();
                        //tree nodes: root-> n properties -> detectionValue -> action
                        Tree sensors = creature.getSensors();
                        HashMap<Integer, Node> sensorProps = sensors.properties;
                        for (int prop : sensorProps.keySet()) {
                            Node detectionValuesNode = sensorProps.get(prop);
                            HashMap<Integer, Integer> detectionValues = detectionValuesNode.getChildren();
                            for (int sensedValue : detectionValues.keySet()) {
                                int action = detectionValues.get(sensedValue);
                                // "creatureID,property,sensorValue,action"+"\n";
                                str.append(creature.getID()).append(",").append(prop).append(",").append(sensedValue).append(",").append(action).append("\n");
                            }
                        }
                        stateWriter.append(str.toString());
                        stateWriter.flush();
                    }
                }
            }
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns all individuals of this map.
	 *
	 * @return all individuals of this map
	 */
	public ArrayList<EmbodiedIndividual> getAllIndividuals() {
		ArrayList<EmbodiedIndividual> res = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				Cell cell = map[i][j];
                res.addAll(cell.creatures.values());
			}
		}
		return res;
	}

	/**
	 * Returns all individuals of this map that are non-light and whose parent is a non-light.
	 *
	 * @return all individuals of this map that are non-light and whose parent is a non-light
	 */
	public List<IndividualWithProperties> getAllEvolvedIndividuals() {
		List<IndividualWithProperties> res = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				Cell cell = map[i][j];
				List<IndividualWithProperties> evolvedIndividuals = cell.creatures.values().stream()
						.filter(individual -> !individual.isLight() && !individual.parentIsLight())
						.collect(Collectors.toList());
				res.addAll(evolvedIndividuals);
			}
		}
		return res;
	}

	public void kill(){
		if (d != null)
			d.dispose();
	}

	@Override
	protected MyLog createMyLog() {
		return new MyLog("map", true);
	}


	/**
	 * Iterates on all individuals on this map.
	 *
	 * @return an iterator on all individuals in this map
	 */
	@Override
	public Iterator<IndividualWithProperties> iterator() {
		return new Itr();
	}


	private class Itr implements Iterator<IndividualWithProperties> {
		/** The x index of the current cell. */
		private int x = 0;
		/** The y index of the current cell. */
		private int y = 0;
		/** The index of the current individual in the current cell. */
		private int cellIndex = 0;
		/** A list that contains the individuals of the current cell. */
		private List<EmbodiedIndividual> creaturesOnCurrentCell;
		/** This field prevents to unnecessarily recompute <code>creaturesOnCurrentCell</code>. */
		private boolean creaturesOnCurrentCellAreCached = false;
		/** The next element to consume. */
		private EmbodiedIndividual nextElement;

		@Override
		public boolean hasNext() {
			while (x < RealMap.this.map.length) {
				while (y < RealMap.this.map[0].length) { // by using 0 as the index, we assume that the map is a rectangle
					if (!creaturesOnCurrentCellAreCached) {
						creaturesOnCurrentCell = getListOfCreaturesOnCell(x, y);
						creaturesOnCurrentCellAreCached = true;
					}
					if (cellIndex < creaturesOnCurrentCell.size()) {
						nextElement = creaturesOnCurrentCell.get(cellIndex);
						return true;
					}
					creaturesOnCurrentCellAreCached = false;
					cellIndex = 0;
					y++;
				}
				y = 0;
				x++;
			}
			return false;
		}

		@Override
		public EmbodiedIndividual next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			cellIndex++;
			return nextElement;
		}

		private List<EmbodiedIndividual> getListOfCreaturesOnCell(int x, int y) {
			return RealMap.this.map[x][y].creatures.values().stream().toList();
		}
	}

	public Display getDisplay() {
		return d;
	}
}
