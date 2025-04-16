package communication;

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

import animals.*;
import startup.Constants;
import visualization.Display;

import static java.lang.Math.abs;

public abstract class Map {
	MyLog mlog = createMyLog();

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
	String summaryFileName;
	String predationFileName;
	String snapshotFileName;
	String sensorsFileName;
	/** simulation time*/
	int time = 0;
	String dataFolderName;

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

	public Map(int mapSize,
			   Display d,
			   String myDataFolderName,
			   String summaryFileName,
			   String predationFileName,
			   String snapshotFileName,
			   String sensorsFileName) {
		this.d = d;
		this.dataFolderName = myDataFolderName;
		this.summaryFileName = summaryFileName;
		this.predationFileName = predationFileName;
		this.snapshotFileName = snapshotFileName;
		this.sensorsFileName = sensorsFileName;

		// read configuration file
		Properties properties = new Properties();
		FileInputStream propsFile;
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
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		int pos = Constants.indexOfByReference(c.creatures, i);
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
	 * Updates the cell that's at the given coordinates.
	 *
	 * @param x the abscissa of the cell to update
	 * @param y the ordinate of the cell to update
	 */
	public abstract void updateCell(int x, int y);

	public void updateMoved(){
		time++;
		if (time%1000==0){
			mlog.say("step " + time);
		}

		if(Constants.Save && (time%5000 == 0)){
			mlog.say("backup all logs");
			if (d != null) {
				d.pauseProcedure(true);
				d.screenshot();
			}
			setupLogFiles();
			if (d != null) {
				d.saveProcedure();
				d.pauseProcedure(false);
			}
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
			if (d != null)
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
			if (d != null)
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
	 * Saves the current state of the map.
	 * </br>
	 * Creates a new directory of format <code>dd_HH_mm</code> and puts in it :
	 * <ul>
	 *     <li>a copy of the config.properties file</li>
	 *     <li>a snapshot file (which contains information about the creatures)</li>
	 *     <li>a sensors file</li>
	 * </ul>
	 *
	 * @param dataFolderName the directory of the current simulation
	 * @return the path to the sensors file
	 */
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

		String filePath = strDate + "/" + snapshotFileName;
		saveCreatures(dataFolderName, filePath);
		// save sensors
		filePath = strDate + "/" + sensorsFileName;
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
						IndividualWithProperties creature = (IndividualWithProperties) c.creatures.get(id);
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

	public int getTime() {
		return time;
	}

	public void setTime(int t) {
		time = t;
	}
	
	/**
	 * shifts a number so values closest to max are 1
	 * @param m between 0..1
	 * @return
	 */
	protected double shiftMax(double val, double m) {
		double s = 1- abs(m-val);//triangular
		s = checkZero(s, m-0.2, m+0.2);
		return s;
	}
	
	//sets at 0 if out of bounds
	protected double checkZero(double val, double low, double high) {
		if(val<low) val = 0;
		if(val>high) val = 0;
		return val;
	}
	
	//set at bounds
	protected double check(double val, double low, double high) {
		if(val<low) val = low;
		if(val>high) val = high;
		return val;
	}
	
	protected boolean generateBool(){
		boolean b = false;
		if(Constants.uniformDouble()>0.5){
			b = true;
		}
		
		return b;
	}

	public void kill(){
		if (d != null)
		    d.dispose();
	}

	public int getNbOfDisplayComponents() {
		return d.getNbOfComponents();
	}

	/**
	 * Returns all individuals of this map that are non-light and whose parent is a non-light.
	 *
	 * @return all individuals of this map that are non-light and whose parent is a non-light
	 */
	public ArrayList<IndividualWithProperties> getAllEvolvedIndividuals() {
		ArrayList<IndividualWithProperties> res = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				Cell cell = map[i][j];
				for (Individual individual : cell.creatures) {
					if (!individual.isLight() && !individual.parentIsLight())
						res.add((IndividualWithProperties) individual);
				}
			}
		}
		return res;
	}

	protected abstract MyLog createMyLog();

}
