package communication;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import animals.*;
import startup.Constants;
import visualization.Display;

import static java.lang.Math.abs;
import static java.lang.Math.min;

public class Map {
	MyLog mlog = new MyLog("map", true);
	/**graphic panel*/
	Display d;
	/** 2D map is made of cells, in each cell there are creatures;*/
	Cell[][] map;
	/** width and length are the same*/
	int size;
	/** global var: id & number of animals until now*/
	int globalID = 0;
	/** data recording*/
	FileWriter summaryWriter;
	FileWriter predationWriter;
	/** simulation time*/
	int time = 0;
	
	
	//for updates
	//for new ones
	LinkedList<Individual> babies;
	//for dead ones
	LinkedList<Individual> remove;
	//for moved ones
	LinkedList<Individual> moving;
	LinkedList<Double> newPositions;
	
	public Map(int mapSize, Display d){
		this.d  = d;
		size = mapSize;		
		//create map
		map = new Cell[size][size];
		//for new ones
		babies = new LinkedList<Individual>();
		//for dead ones
		remove = new LinkedList<Individual>();
		//for moved ones
		moving = new LinkedList<Individual>();
		newPositions = new LinkedList<Double>();
		//create cells
		for(int i=0;i<size;i++){
			for(int j=0;j<size;j++){
				map[i][j] = new Cell();
			}
		}
		
		//writing data
		if(Constants.Save) {
			// individuals info
			FileBuilder fb = new FileBuilder(Constants.SummaryFileName);
			summaryWriter = fb.getFileWriter();
			fb = null;

			//csv file header
			/*
			String description =  ID +","+parentID+","+birthDate+","+life+","
				+ speed+","+maxEnergy+","+ getKidEnergy()+","
				+ hasSensors() +","+ getAncestor() + "," + getNKids() + ","
				+ death + ","+ matForKids ;
			 */
			String str = "ID,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor,nkids,pgmDeath,matForKids"+"\n";
			try {
				summaryWriter.append(str);
				summaryWriter.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(Constants.SavePredation) {
			// predation info
			FileBuilder fb_predation = new FileBuilder("predation");
			predationWriter = fb_predation.getFileWriter();
			fb_predation = null;

			/*
			String description =  ID +","+parentID+","+birthDate+","+life+","
				+ speed+","+maxEnergy+","+ getKidEnergy()+","
				+ hasSensors() +","+ getAncestor() + "," + getNKids() + ","
				+ death + ","+ matForKids ;
			 */
			String header_predation = "t, pred_id," +
					"pred_parent, pred_created, pred_lifeSpan," +
					"pred_speed, pred_maxEnergy, pred_kidEnergy, pred_sensors," +
					"pred_ancestor, pred_nkids, pred_pgmDeath, pred_matForKids," +
					"prey_id,"+
					"prey_parent, prey_created, prey_lifeSpan," +
					"prey_speed, prey_maxEnergy, prey_kidEnergy, prey_sensors," +
					"prey_ancestor, prey_nkids, prey_pgmDeath, prey_matForKids" +
					"\n";


			try {
				predationWriter.append(header_predation);
				predationWriter.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	public void addIndividual(int x, int y, Individual i){
		Cell c = map[x][y];
		i.setCellTransparency(c.transparency);
		c.creatures.add(i);
	}
	
	public void removeIndividual(Individual i){
		double[] position = i.getPosition();
		int x = (int)(position[0] +0.5);
		int y = (int)(position[1] +0.5);
		Cell c = map[x][y];
		int pos = c.creatures.indexOf(i);
		c.creatures.remove(pos);
	}
	
	/**
	 * @param x2 new x
	 * @param y2 nex y
	 * @param i individual
	 */
	public void updatePosition(int x2, int y2, Individual i){
		removeIndividual(i);
		addIndividual(x2,y2,i);		
	}
	
	/**
	 * 
	 * @param x coordinate
	 * @param y coordinate
	 */
	public void updateCell(int x, int y){
		
		Cell c = map[x][y];
		c.calculateValues();
		int size = c.creatures.size();

		if(size==0){
			return;
		}
		
		//if no one here add light if needed
		//interactions: max is everyone interacts with everyone
		ArrayList<Integer> interacting = new ArrayList<Integer>();
		ArrayList<Integer> interactedOn = new ArrayList<Integer>();
		ArrayList<Integer> interaction = new ArrayList<Integer>();

		List<Integer> shuffled_creatures_arr = IntStream.range(0, size).boxed().collect(Collectors.toList());
		Collections.shuffle(shuffled_creatures_arr);

		for (int temp_i = 0; temp_i < size; temp_i++) {
			int i = shuffled_creatures_arr.get(temp_i);

			Individual creature = c.creatures.get(i);
            boolean alive = creature.update(babies, time, c.transparency);
            double[] position = creature.getPosition();

            if(!alive){
            	remove.add(creature);
            } else{
            	double np[] = new double[2];
	            boolean moved = false;
				double speed = creature.getSpeed();

            	if(!creature.isLight()){
	            	//move
	        		for(int j=0;j<2;j++){
	        			if(generateBool()){
	        				np[j]= position[j]+(speed*Constants.SpeedFactor);//(creature.getSpeed()*Constants.SpeedFactor*c.density*c.transparency);
	        			}else{
	        				np[j] = position[j]-(speed*Constants.SpeedFactor);
	        			}
	        			if(np[j]<0) np[j]=0;
	        			if(np[j]>=Constants.GridMax-1) np[j] = Constants.GridMax-2; //something wrong but what
	        			if(np[j] != position[j]){
	        				moved = true;
	        			}
	        		}
            	}
            	
        		Tree sensors = creature.getSensors();
        		
        		//interactions between creatures
                Node s = sensors.root;
                //iterate on properties
                ArrayList<Node> sChildren = s.getChildren();
                for(int k=0; k<sChildren.size();k++){
                	//this is the property
                	Node prop = sChildren.get(k);
                	//these are the value-action pairs
                	ArrayList<Node> pChildren = prop.getChildren();
                	
                	for(int l=0; l<pChildren.size();l++){
                		int value = pChildren.get(l).data;
                		//actions
        				ArrayList<Node> actions = pChildren.get(l).getChildren();
        				for (Iterator<Node> iterator = actions.iterator(); iterator.hasNext();) {
							Node node = (Node) iterator.next();
							//action
							int act = node.data;
							
							//interactions with other creatures
							if(act<2){
								//iterate creatures on this cell
		                		for(int m=0; m<c.creatures.size();m++){
		                			double p = 1*3/(double)c.creatures.size();
		                			if(Constants.uniformDouble()>p){
		                				continue;
		                			}

		                			Individual cr2 = c.creatures.get(m);
		                			if(remove.contains(c.creatures.get(m)) | (cr2.isLight())){
		                				continue;
		                			}
		                			//creature can't interact on itself
		                			if(m==i){
		                				continue;
		                			}

									double ind_prop = cr2.getProperties()[k];
		                			
									if( (value >= ind_prop - 5) && (value <= ind_prop + 5) ){
		                				//record interaction
		                				interacting.add(i);
		                				interactedOn.add(m);
		                				interaction.add(act);
		                			}//*/
		                		}
							} else if (act>5) {
								
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
        				double energy = creature.getEnergy() - speed*Constants.SpeedCost;// - numberActions*Constants.ActionCost;
        				creature.setEnergy(energy);
        			}
        		}
            }    
        }
		
		
		//sort out the interactions (order should be random)
		ArrayList<Integer> randomList = new ArrayList<Integer>();
		for(int i=0;i<interacting.size();i++){
			randomList.add(i);
		}
		Collections.shuffle(randomList, Constants.rand);
		
		for(int j=0;j<randomList.size();j++){
			int i = randomList.get(j);
			
			//is interaction still valid?
			if((interacting.get(i)<0) || (interactedOn.get(i)<0)){
				continue;
			}
			
			//if "eat", delete prey and turn predator to black
			if(interaction.get(i) == Constants.ActEat){
				int predator_id = interacting.get(i);
				int prey_id = interactedOn.get(i);
				tryEat(predator_id, prey_id, c, interacting, interactedOn);
			}
		}
	}

	private void tryEat(int predator_id, int prey_id, Cell c, ArrayList<Integer> interacting, ArrayList<Integer> interactedOn) {
		Individual prey = c.creatures.get(prey_id);
		Individual predator = c.creatures.get(predator_id);

		double ok = predator.getEnergy() - prey.getEnergy();
		if(ok>=0){
			//delete prey from arrays
			Collections.replaceAll(interacting, prey_id,-1);
			Collections.replaceAll(interactedOn, prey_id,-1);
			//give energy to predator
			double e = prey.getEnergy();
			if(e>0){
				double energy = predator.getEnergy() + e;
				predator.setEnergy(energy);
				//mlog.say("total "+ c.creatures.get(predator).energy);
				//record prey as dead
				prey.setEnergy(0); //if(c.creatures.get(prey).color == Color.black) mlog.say("predator confusion 1");
				prey.setEatenBy(predator_id);
				predator.setBorderColor(Color.black);
			}
			// only save successful predation
			if(Constants.SavePredation){
				// reduce file size
				if(Constants.uniformDouble()<0.01){
					EmbodiedIndividual ei_prey = (EmbodiedIndividual) prey;
					EmbodiedIndividual ei_pred = (EmbodiedIndividual) predator;
							/*
								String header_predation = "t, pred_id, prey_id," +
								"pred_lifeSpan, pred_speed, pred_maxEnergy, pred_kidEnergy," +
								"pred_sensors, pred_nkids, pred_pgmDeath, pred_matForKids," +
								"prey_lifeSpan, prey_speed, prey_maxEnergy, prey_kidEnergy, prey_sensors, prey_ancestor, prey_nkids," +
								"prey_pgmDeath, prey_matForKids\n";
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
			double energy = ePred-abs(ePrey*Constants.ErrorCost);
			predator.setEnergy(energy);
			//wound prey
			energy = ePrey-abs(ePred*Constants.ErrorCost);//*3
			prey.setEnergy(energy);
			predator.setBorderColor(Color.red);
			prey.setBorderColor(Color.gray);
		}
	}

	public void updateMoved(){
		time++;
		if (time%1000==0){
			mlog.say("step " + time);
		}
		
		//update dead
		for(int i=0; i<remove.size();i++){
			Individual creature = remove.get(i);
			if(Constants.Save) {
				if(Constants.uniformDouble()<0.01) {
					if (!creature.isLight() & !creature.parentIsLight()) {
						//write down info
						// "ID,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor\n";
						String str = creature.stringDesc() + "\n";
						//mlog.say(str);
						try {
							summaryWriter.append(str);
							summaryWriter.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}

			//remove from display
	    	d.removeComponent(creature);
	    	//remove from map
	    	removeIndividual(creature);
		}		
				
		//update moved
		for (int i = 0; i < moving.size(); i++) {
			Individual creature = moving.get(i);
			double[] position = creature.getPosition();
			//new x,y	
			int nx = (int) (newPositions.get(i*2)+0.5);
			int ny = (int) (newPositions.get(i*2+1)+0.5);
			updatePosition(nx,ny,creature);
			position[0] = newPositions.get(i*2);
			position[1] = newPositions.get(i*2+1);  	
			creature.setPosition(position);
        }
		
		//add new babies
		for (int i = 0; i < babies.size(); i++) {
			Individual baby = babies.get(i);
			double[] position = baby.getPosition();
			
			globalID++;
			baby.setID(globalID);
			//mlog.say("added to map");
			int nx = (int) (position[0]+0.5);
			int ny = (int) (position[1]+0.5);
			addIndividual(nx, ny, baby);
			d.addComponent(baby);
        }		
		
		//clear
		//for new ones
		babies = new LinkedList<Individual>();
		//for dead ones
		remove = new LinkedList<Individual>();
		//for moved ones
		moving = new LinkedList<Individual>();
		newPositions = new LinkedList<Double>();
	}
	
	public int incrementGlobalID(){
		globalID++;
		return globalID;
	}
	
	/**
	 * a cell on the map
	 * should have own class file.
	 * @author lana
	 *
	 */
	private class Cell{
		//cell's physical properties
		/** how easy light goes through it (0=does not get out)*/
		double transparency = 1;
		/** how easy it is to move through (1=cannot move) */
		double density = 0;//TODO use. may also change how sound etc travels.
		
		double ntransparency = 1;
		double ndensity = 0;//TODO use. may also change how sound etc travels.
		
		/** determined by animals and transparency on this cell*/
		double luminosity;
		double sound;
		double smell;
		double temperature;
		double electric;
		
		/** all creatures on this cell*/
		LinkedList<Individual> creatures;
		
		public Cell(){
			creatures = new LinkedList<Individual>();	
		}
		
		public double getPhy(int kk) {
			double p = 0;
			switch (kk) {
			case 0:{
				p = transparency;
				break;
			}
			case 1:{
				p = density;
				break;
			}
			default:
				break;
			}
			return p;
		}

		/** return a property of the cell*/
		public double getProp(int k) {
			double r = 0;
			//todo put all in an array
			switch (k) {
			case 0:
				r = luminosity;
				break;
			case 1:
				r = sound;
				break;
			case 2:
				r = smell;
				break;
			case 3:
				r = temperature;
				break;
			case 4:
				r = electric;
				break;
			default:
				r = 0;
				break;
			}
			return r;
		}

		public void changeProperties(int action) {
			switch (action) {
			case Constants.LessTransparency:{
				changeProperties(-0.01,0);
				break;
			}
			case Constants.MoreTransparency:{
				changeProperties(0.01,0);
				break;
			}
			case Constants.LessDensity:{
				changeProperties(0,-0.01);
				break;
			}
			case Constants.MoreDensity:{
				changeProperties(0,0.01);
				break;
			}
			default:
				break;
			}
		}
		/**
		 * 
		 * @param t transparency
		 * @param d density
		 */
		private void changeProperties(double t, double d){

		}
		
		
		/**
		 * reset animal dependent values to 0
		 * and recalculate them
		 */
		public void calculateValues(){
			//smell and temp could last longer in time
			luminosity = 0;
			sound = 0;
			smell = 0;
			temperature = 0;
			electric = 0;
			
			//ntransparency = transparency;
			//ndensity = density;
			transparency = ntransparency;
			density = ndensity;
			
			//make modular function for this
			for (Iterator<Individual> iterator = creatures.iterator(); iterator.hasNext();) {
				Individual ind = iterator.next();
				luminosity+=ind.getLuminosity();
				sound += ind.getLoud();
				smell += ind.getSmelly();
				temperature += ind.getWarm();
				electric += ind.getElectric();
			}
			

			luminosity = luminosity/creatures.size();
			sound = sound/creatures.size();
			smell = smell/creatures.size();
			temperature = temperature/creatures.size();
			electric = electric/creatures.size();
			
			luminosity = shiftMax(luminosity, transparency);
			sound = shiftMax(sound, density);
			smell = shiftMax(smell, 1-transparency);
			temperature = shiftMax(temperature, 1-density);
			electric = shiftMax(electric, density*transparency);
		}
	}
	
	/**
	 * shifts a number so values closest to max are 1
	 * @param m between 0..1
	 * @return
	 */
	private double shiftMax(double val, double m) {
		double s = 1- abs(m-val);//triangular
		s = checkZero(s, m-0.2, m+0.2);
		return s;
	}
	
	//sets at 0 if out of bounds
	private double checkZero(double val, double low, double high) {
		if(val<low) val = 0;
		if(val>high) val = 0;
		return val;
	}
	
	//set at bounds
	private double check(double val, double low, double high) {
		if(val<low) val = low;
		if(val>high) val = high;
		return val;
	}
	
	private boolean generateBool(){
		boolean b = false;
		if(Constants.uniformDouble()>0.5){
			b = true;
		}
		
		return b;
	}
}
