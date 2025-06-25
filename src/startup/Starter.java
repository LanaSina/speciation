/**
 * 
 */
package startup;

import animals.EmbodiedIndividual;
import animals.Node;
import animals.Tree;
import communication.Map;
import communication.MyLog;
import communication.RealMap;
import communication.ShadowMap;
import oee_analysis.DeltasSaver;
import org.apache.commons.cli.*;
import visualization.Display;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;




/**
 * @author lana
 * This class is the main class.
 *
 */
public class Starter {

	static String dataFolderName;
	/** Time step at which to stop the program. -1 means infinite, i.e. never stop. */
	private static int stopAt = -1;


	public static void main(String[] args) {
		if (handleArguments(args)) return;

		MyLog mlog = new MyLog("starter",true);

		//get current date
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
		Date date = new Date();
		String strDate = dateFormat.format(date);
		dataFolderName = Constants.DataPath + "/" + strDate + "/";

		//first create directory
		File theDir = new File(dataFolderName);
		// if the directory does not exist, create it
		if (!theDir.exists()) {
			mlog.say("creating directory: " + dataFolderName);
			boolean result = false;

			try{
				theDir.mkdir();
				result = true;
			}
			catch(SecurityException se){
				//handle it
			}
			if(result) {
				System.out.println("DIR created");
			}
		}

		// move config file (todo: path in constants)
		Path src = Paths.get("src/config.properties");
		Path target = Paths.get(dataFolderName+"config.properties");
		try {
			Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		mlog.say("properties copied to " + dataFolderName);

		Properties properties = loadProperties("src/config.properties");
		String dname = properties.getProperty("sim_name");
		int cst_grid_max= Integer.parseInt(properties.getProperty("grid_max"));
		final int shadowModelResetEvery = Integer.parseInt(properties.getProperty("shadow_model_reset_every"));
		final int deltasSavedEvery = Integer.parseInt(properties.getProperty("deltas_saved_every"));

		LifeRunnable life = Constants.RunShadowModel ? new LifeRunnableWithShadow(deltasSavedEvery, shadowModelResetEvery) : new LifeRunnable(deltasSavedEvery);
		Display d = Constants.ENABLE_DISPLAY ? new Display(dname, life, dataFolderName) : null;
		int lightLimit = 30;//30
		int of = 10;

		//worldmap		
		RealMap map = new RealMap(cst_grid_max, d, dataFolderName, Constants.SummaryFileName, Constants.PredationFileName, Constants.SnapshotFileName, Constants.SensorsFileName);
		if (Constants.Save)
			map.setupLogFiles();

		//initialize map (do it from file!!)
		for(int i=0; i<lightLimit; i++){
			for(int j=0; j<lightLimit; j++){
				int x = i+of;
				int y = j+of;

				int id = map.incrementAndGetGlobalID();
				EmbodiedIndividual l = new EmbodiedIndividual(x,y,id,0,0, -1);
				map.addIndividual(x, y, l);
				if (d != null)
					d.addComponent(l);
			}
		}

		System.out.println("Saving of information about individuals" + (Constants.Save ? "enabled" : " DISABLED") + ".");
		life.setMap(map);
		new Thread(life).start();
	}

	private static boolean handleArguments(String[] args) {
		// define options
		Option optionStopAt = Option.builder("s")
				.longOpt("stop-at")
				.desc("Specifies at which time step to stop the program (a null or negative value implies that the program will never stop). If this option is not provided, the program will run forever.")
				.hasArg()
				.argName("time-step")
				.type(Integer.class)
				.build();
		Options options = new Options();
		options.addOption(optionStopAt);

		// define usage
		String header = "Simulates the tree of life.\r\n\r\n";
		HelpFormatter formatter = new HelpFormatter();

		// parse options
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Error : " + e.getMessage());
			formatter.printHelp("tolsim", header, options, "", true);
			System.exit(1);
			return true;
		}

		// handle options
		if (cmd.hasOption(optionStopAt)) {
			try {
				stopAt = cmd.getParsedOptionValue(optionStopAt);
			} catch (ParseException e) {
				System.err.println("Error : " + e.getMessage());
				formatter.printHelp("tolsim", header, options, "", true);
				System.exit(1);
				return true;
			}
		}

		return false;
	}


	public static Properties loadProperties(String path){
		// read configuration file
		Properties properties = new Properties();
		FileInputStream propsFile = null;
		try {
			propsFile = new FileInputStream(path);
			properties.load(propsFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return(properties);
	}


	/**
	 * Runnable that runs the real map.
	 */
	public static class LifeRunnable implements Runnable {

		/** Logger */
		MyLog mlog = new MyLog("lifeRunnable", true);
		/** This field is true while the runnable stays alive. */
		boolean run = true;
		/** This field is true while the runnable isn't paused. */
		public boolean running = true;
		/** Indicates whether the state of the map(s) must be saved at the next update. */
		boolean doSave = false;

		/** The map. */
		RealMap map = null;
		/** The map's size. */
		int mapSize = Constants.GridMax;
		/** An object that serves to save the deltas of the map. */
		DeltasSaver realMapDeltasSaver;
		/** The number of time steps between two saves of the deltas. */
		final int deltasSavedEvery;


		/**
		 * Builds a runnable that runs the real model.
		 *
		 * @param deltasSavedEvery number of time steps between two saves of the deltas
		 */
		public LifeRunnable(int deltasSavedEvery) {
			this.deltasSavedEvery = deltasSavedEvery;
		}


		/**
		 * Sets this LifeRunnable's map.
		 *
		 * @param map the map to set
		 */
		public void setMap(RealMap map) {
			this.map = map;
			this.realMapDeltasSaver = new DeltasSaver(map, dataFolderName, Constants.RealMapDeltasFileName + ".csv");
		}


		/**
		 * Main loop of the program.
		 */
		public void run() {
			while (run) {
				// is false after PauseProcedure
				if (running) {
					update();
				} else {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
				if (doSave) {
					saveMapsState();
					doSave = false;
				}
				if (map.getTime() == stopAt)
					this.kill();
			}
			mlog.say("dies");
		}


		/**
		 * Saves the state of the map(s).
		 */
		protected void saveMapsState() {
			saveRealMapState();
		}


		/**
		 * Saves the state of the real map.
		 */
		protected final void saveRealMapState() {
			String fileName = map.saveState(dataFolderName);
			String savedAt = dataFolderName + fileName;
			mlog.say("Saved at " + savedAt);
		}


		/**
		 * Ensures that the map(s)'s states will be saved at the next update.
		 */
		public final void save() {
			doSave = true;
		}


		/**
		 * Updates each individual on the map, and perhaps saves the map's deltas.
		 */
		protected void update() {
			for (int i = 0; i < mapSize; i++) {
				for (int j = 0; j < mapSize; j++) {
					map.updateCell(i, j);
				}
			}
			map.applyChanges();
			if (map.getTime() % deltasSavedEvery == 0)
				realMapDeltasSaver.update();
		}


		public void kill() {
			run = false;
		}


		public void load(File directory) {
			// read properties
			String target = directory.getAbsolutePath() + "/config.properties";
			//copy them
			Path copyTo = Paths.get(dataFolderName + "config.properties");
			try {
				Files.copy(Paths.get(target), copyTo, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			mlog.say("properties copied to " + dataFolderName);

			Properties properties = loadProperties(target);

			// kill previous display
			map.kill();
			//worldmap
			String dname = properties.getProperty("sim_name");
			int cst_grid_max = Integer.parseInt(properties.getProperty("grid_max"));
			Display d = new Display(dname, this, dataFolderName);
			map = new RealMap(cst_grid_max, d, dataFolderName, Constants.SummaryFileName, Constants.PredationFileName, Constants.SnapshotFileName, Constants.SensorsFileName);

			// read creatures
			target = directory.getAbsolutePath() + "/" + Constants.SnapshotFileName + ".csv";
			// save all creatures by id
			HashMap<Integer, EmbodiedIndividual> individualMap = new HashMap<>();
			// read line by line
			Scanner sc = null;
			String[] lineArray;
			String line = null;
			int maxId = -1;
			try {
				sc = new Scanner(new File(target));
				// time header
				sc.nextLine();
				// time value
				line = sc.nextLine();
				int time = Integer.parseInt(line);
				map.setTime(time);
				// header
				// x,y,ID,isLight,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor,nkids,pgmDeath,matForKids
				sc.nextLine();
				// sc.useDelimiter(",");   //sets the delimiter pattern
				while (sc.hasNextLine()) {
					line = sc.nextLine();
					lineArray = line.split(",");
					//x and y
					int x = Integer.parseInt(lineArray[0]);
					int y = Integer.parseInt(lineArray[1]);
					int id = Integer.parseInt(lineArray[2]);
					if (id > maxId) {
						maxId = id;
					}
					EmbodiedIndividual individual = new EmbodiedIndividual(id, line);
					individualMap.put(id, individual);
					map.addIndividual(x, y, individual);
					if (d != null)
						d.addComponent(individual);
				}
				sc.close();  //closes the scanner
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

			map.setGlobalId(maxId + 1);
			// set sensors
			// read creatures
			target = directory.getAbsolutePath() + "/" + Constants.SensorsFileName + ".csv";
			// read line by line
			try {
				sc = new Scanner(new File(target));
				//csv file header
				int creatureId = -1;
				// int sensorId = -1;
				Node prop = null;
				EmbodiedIndividual individual = null;
				// Tree sensors = null;
				//String str = "creatureID,sensorId,sensorValue,action"+"\n";
				sc.nextLine();
				line = null;
				while (sc.hasNextLine()) {
					line = sc.nextLine();
					lineArray = line.split(",");
					int pos = 0;

					int newCreatureId = Integer.parseInt(lineArray[pos]);
					pos++;
					if (newCreatureId != creatureId) {
						creatureId = newCreatureId;
						individual = individualMap.get(creatureId);
					}

					int property = Integer.parseInt(lineArray[pos]);
					pos++;
					int value = Integer.parseInt(lineArray[pos]);
					pos++;
					int action = Integer.parseInt(lineArray[pos]);
					Tree sensors = individual.getSensors();
					sensors.addSensor(property, value, action);
				}
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}




	/**
	 * Runnable that runs the real map and the shadow map.
	 */
	public static class LifeRunnableWithShadow extends LifeRunnable {

		/** The shadow map. */
		ShadowMap shadowMap;
		/** An object that serves to save the deltas of the shadow map. */
		private DeltasSaver shadowMapDeltasSaver;
		/** The number of time steps between two shadow model resets. */
		protected int shadowModelResetEvery;


		/**
		 * Builds a runnable that runs the real model and the shadow model.
		 *
		 * @param deltasSavedEvery      number of time steps between two saves of the deltas
		 * @param shadowModelResetEvery number of time steps between two shadow model resets
		 */
		public LifeRunnableWithShadow(int deltasSavedEvery, int shadowModelResetEvery) {
			super(deltasSavedEvery);
			this.shadowModelResetEvery = shadowModelResetEvery;
		}


		/**
		 * Sets this LifeRunnableWithShadow's map and shadow map (initially similar to the real map).
		 *
		 * @param map the map to set
		 */
		@Override
		public void setMap(RealMap map) {
			super.setMap(map);
			this.shadowMap = new ShadowMap(map, Constants.ShadowModelSummaryFileName, Constants.ShadowModelSnapshotFileName, Constants.ShadowModelSensorsFileName);
			if (Constants.Save)
				this.shadowMap.setupLogFiles();
			this.shadowMapDeltasSaver = new DeltasSaver(shadowMap, dataFolderName, Constants.ShadowMapDeltasFileName + ".csv");
		}


		/**
		 * Main loop of the program.
		 */
		@Override
		public void run() {
			System.out.println("Shadow map enabled.");
			super.run();
		}


		/**
		 * Saves the state of the map(s).
		 */
		@Override
		protected void saveMapsState() {
			saveRealMapState();
			saveShadowMapState();
		}


		/**
		 * Saves the state of the shadow map.
		 */
		final protected void saveShadowMapState() {
			String fileName = shadowMap.saveState(dataFolderName);
			String savedAt = dataFolderName + fileName;
			mlog.say("Saved at " + savedAt);
		}


		/**
		 * Updates each individual on the real map and on the shadow map,
		 * and perhaps saves their respective deltas, and perhaps resets
		 * the state of the shadow map to that of the real map.
		 */
		@Override
		protected void update() {
			// compute changes on the real map
			for (int i = 0; i < mapSize; i++) {
				for (int j = 0; j < mapSize; j++) {
					map.updateCell(i, j);
				}
			}
			// compute changes on the shadow map
			shadowMap.incrementAgeOfAllIndividuals();
			shadowMap.createRandomIndividuals(map.getNbOfBirths());
			shadowMap.removeRandomIndividuals(map.getNbOfDeaths());
			// apply the changes, and maybe save the deltas
			Thread realMapThread = new Thread(() -> applyChangesAndMaybeSaveDeltas(map, realMapDeltasSaver));
			Thread shadowMapThread = new Thread(() -> applyChangesAndMaybeSaveDeltas(shadowMap, shadowMapDeltasSaver));
			realMapThread.start();
			shadowMapThread.start();
			try {
				realMapThread.join();
				shadowMapThread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			// maybe reset the shadow model
			if (map.getTime() % shadowModelResetEvery == 0) {
				shadowMap.reset();
			}
		}


		/**
		 * Applies changes for the given map, and perhaps save the map's deltas.
		 *
		 * @param map the map of which to apply the changes
		 * @param deltasSaver the object that serves to save the deltas
		 */
		private void applyChangesAndMaybeSaveDeltas(Map map, DeltasSaver deltasSaver) {
			map.applyChanges();
			if (map.getTime() % deltasSavedEvery == 0)
				deltasSaver.update();
		}


		@Override
		public void load(File directory) {
			// TODO: this overriding method is probably necessary, because in this class, there there is a shadow model.
		}
	}

}
