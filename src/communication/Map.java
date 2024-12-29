package communication;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import animals.*;
import startup.Constants;
import visualization.Display;

import static java.lang.Math.abs;

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
	String dataFolderName = null;

	// global contstants
	double cst_mut_factor;
	double cst_speed_factor;
	int cst_speed_max;
	double cst_light_birth_dst;
	double cst_birth_dst;
	int cst_grid_max;
	int cst_energy_max;
	double cst_speed_cost;
	double cst_sensor_cost;
	double cst_error_cost;
	int cst_free_energy;
	int cst_sensor_precision;
	int cst_max_number_actions;
	double cst_energy_cost_factor;
	double cst_step_cost;
	
	//for updates
	//for new ones
	LinkedList<Individual> babies;
	//for dead ones
	LinkedList<Individual> remove;
	//for moved ones
	LinkedList<Individual> moving;
	LinkedList<Double> newPositions;
	
	public Map(int mapSize, Display d, String myDataFolderName){
		this.d  = d;
		this.dataFolderName = myDataFolderName;

		// read configuration file
		Properties properties = new Properties();
		FileInputStream propsFile = null;
		try {
			propsFile = new FileInputStream("src/config.properties");
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
			setupLogFiles();
		}
	}


	private void setupLogFiles(){
		// individuals info
		FileBuilder fb = new FileBuilder(dataFolderName, Constants.SummaryFileName + "_" + time);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// predation info
		FileBuilder fb_predation = new FileBuilder(dataFolderName, Constants.PredationFileName+ "_" + time);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		int size = c.creatures.size();

		if(size==0){
			return;
		}

		List<Integer> shuffled_creatures_arr = IntStream.range(0, size).boxed().collect(Collectors.toList());
		Collections.shuffle(shuffled_creatures_arr, Constants.rand);

		for (int temp_i = 0; temp_i < size; temp_i++) {
			int i = shuffled_creatures_arr.get(temp_i);

			Individual creature = c.creatures.get(i);
            boolean alive = creature.update(babies, time, c.transparency, cst_mut_factor, cst_speed_max,
					cst_light_birth_dst, cst_birth_dst, cst_grid_max, cst_energy_max, cst_speed_cost,
					cst_sensor_cost, cst_free_energy, cst_energy_cost_factor, cst_step_cost
			);
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

	public void updateMoved(){
		time++;
		if (time%1000==0){
			mlog.say("step " + time);
		}

		if(Constants.Save && (time%5000 == 0)){
			mlog.say("backup all logs");
			d.pauseProcedure(true);
			d.screenshot();
			setupLogFiles();
			d.saveProcedure();
			d.pauseProcedure(false);
		}
		
		//update dead
		for(int i=0; i<remove.size();i++){
			Individual creature = remove.get(i);
			if(Constants.Save) {
				if(Constants.uniformDouble()<1) { //0.01
					if (!creature.isLight() & !creature.parentIsLight()) {
						//write down info
						// "ID,pred_pos_x, pred_pos_y,isLight,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor, parentIsLight\n";
						String str = creature.stringDesc() + "\n";
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

	public String saveSate(String dataFolderName) {
		//snapshot time
		DateFormat dateFormat = new SimpleDateFormat("dd_HH_mm");
		Date date = new Date();
		String strDate = dateFormat.format(date);

		File theDir = new File(dataFolderName+"/"+strDate);
		// if the directory does not exist, create it
		if (!theDir.exists()) {
			mlog.say("creating directory: " + dataFolderName);
			boolean result = false;
			try{
				theDir.mkdir();
				result = true;
			}
			catch(SecurityException se){
			}
			if(result) {
				System.out.println("DIR created");
			}
		}

		// move config file (todo: path in constants)
		Path src = Paths.get("src/config.properties");
		Path target = Paths.get(dataFolderName+"/"+strDate+"/config.properties");
		try {
			Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		mlog.say("properties copied to " + dataFolderName+"/"+strDate);

		String filePath = strDate + "/" + Constants.SnapshotFileName;
		saveCreatures(dataFolderName, filePath);
		// save sensors
		filePath = strDate + "/" + Constants.SensorsFileName;
		saveSensors(dataFolderName, filePath);

		return filePath;
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

					for (int id = 0; id<size; id++){
						Individual creature = c.creatures.get(id);
						String astr = x + "," + y + "," + creature.stringDesc() + "\n";
						stateWriter.append(astr);
						stateWriter.flush();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void saveSensors(String dataFolderName, String filePath){
		//open file for creatures
		FileBuilder fb = new FileBuilder(dataFolderName, filePath);
		FileWriter stateWriter = fb.getFileWriter();
		fb = null;

		//csv file header
		String str = "creatureID,sensorId,sensorValue,action"+"\n";
		//String debugstr = "";
		try {
			stateWriter.append(str);
			stateWriter.flush();

			for(int x=0; x<map.length;x++) {
				for (int y = 0; y < map[0].length; y++) {
					Cell c = map[x][y];
					int size = c.creatures.size();

					if(size==0){
						continue;
					}

					for (int id = 0; id<size; id++){
						EmbodiedIndividual creature = (EmbodiedIndividual) c.creatures.get(id);
						if (creature.hasSensors()==0){
							continue;
						}

						str = "";
						//tree nodes: root-> n properties -> detectionValue -> action
						Tree sensors = creature.getSensors();
						HashMap<Integer, Node> sensorProps = sensors.properties;
						for (Iterator<Integer> propIt = sensorProps.keySet().iterator(); propIt.hasNext();){
							int prop = propIt.next();
							Node detectionValuesNode = sensorProps.get(prop);
							HashMap<Integer, Integer> detectionValues = detectionValuesNode.getChildren();
							for (Iterator<Integer> senseIt = detectionValues.keySet().iterator(); senseIt.hasNext();){
								int sensedValue = senseIt.next();
								int action =  detectionValues.get(sensedValue);
								// "creatureID,property,sensorValue,action"+"\n";
								str = str + creature.getID() + "," + prop + "," + sensedValue + "," + action + "\n";
							}
						}
						stateWriter.append(str);
						stateWriter.flush();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setGlobalId(int i) {
		globalID = i;
	}

	public void setTime(int t) {
		time = t;
	}

	/**
	 * a cell on the map
	 * should have own class file.
	 * @author lana
	 *
	 */
	private class Cell {
		//cell's physical properties
		/**
		 * how easy light goes through it (0=does not get out)
		 */
		double transparency = 1;
		/**
		 * how easy it is to move through (1=cannot move)
		 */
		double density = 0;//TODO use. may also change how sound etc travels.

		/**
		 * determined by animals and transparency on this cell
		 */
		double luminosity;
		double sound;
		double smell;
		double temperature;
		double electric;

		/**
		 * all creatures on this cell
		 */
		LinkedList<Individual> creatures;

		public Cell() {
			creatures = new LinkedList<Individual>();
		}

		public double getPhy(int kk) {
			double p = 0;
			switch (kk) {
				case 0: {
					p = transparency;
					break;
				}
				case 1: {
					p = density;
					break;
				}
				default:
					break;
			}
			return p;
		}

		/**
		 * return a property of the cell
		 */
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

	public void kill(){
		d.dispose();
	}


}
