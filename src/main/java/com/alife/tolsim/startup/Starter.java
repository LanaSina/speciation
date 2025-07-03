/**
 * 
 */
package com.alife.tolsim.startup;

import com.alife.tolsim.animals.EmbodiedIndividual;
import com.alife.tolsim.animals.Node;
import com.alife.tolsim.animals.Tree;
import com.alife.tolsim.communication.Map;
import com.alife.tolsim.communication.MyLog;
import com.alife.tolsim.communication.RealMap;
import com.alife.tolsim.communication.ShadowMap;
import com.alife.tolsim.oee_analysis.DeltasSaver;
import com.alife.tolsim.visualization.Display;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.*;



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
		Properties properties = new Properties();
		try (InputStream in = Starter.class.getClassLoader().getResourceAsStream("config.properties")) {
			if (in == null) {
				throw new FileNotFoundException("config.properties not found in classpath");
			}

			// copy the file into the target folder
			Path target = Paths.get(dataFolderName + "config.properties");
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			mlog.say("properties copied to " + dataFolderName);

			// reload the copied file for properties reading
			try (InputStream copiedIn = Files.newInputStream(target)) {
				properties.load(copiedIn);
			}

		} catch (IOException e) {
			throw new RuntimeException("Failed to copy/load config.properties", e);
		}

		String dname = properties.getProperty("sim_name");
		int cst_grid_max = Integer.parseInt(properties.getProperty("grid_max"));
		final int shadowModelResetEvery = Integer.parseInt(properties.getProperty("shadow_model_reset_every"));
		final int deltasSavedEvery = Integer.parseInt(properties.getProperty("deltas_saved_every"));


		LifeRunnable life = Constants.RunShadowModel ? new LifeRunnableWithShadow(deltasSavedEvery, shadowModelResetEvery) : new LifeRunnable(deltasSavedEvery);
		Display d = Constants.EnableDisplay ? new Display(dname, life, dataFolderName) : null;
		int lightLimit = 30;//30
		int of = 10;

		//worldmap		
		RealMap map = new RealMap(cst_grid_max, d, dataFolderName, Constants.SummaryFileName, Constants.PredationFileName, Constants.SnapshotFileName, Constants.SensorsFileName, Constants.PopulationFilename);

		//initialize map (do it from file!!)
		for(int i=0; i<lightLimit; i++){
			for(int j=0; j<lightLimit; j++){
				int x = i+of;
				int y = j+of;

				long id = map.incrementAndGetGlobalID();
				EmbodiedIndividual l = new EmbodiedIndividual(x,y,id,0,0, -1);
				map.addIndividual(x, y, l);
				if (d != null)
					d.addComponent(l);
			}
		}

		System.out.println("Saving of information about individuals" + (Constants.Save ? "enabled" : " DISABLED") + ".");
		life.setRealMap(map);
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

		/** Gap between two times the time is printed (in number of time steps). */
		protected static final int TIME_PRINTING_GAP = 100;

		/** Logger */
		MyLog mlog = new MyLog("lifeRunnable", true);
		/** This field is true while the runnable stays alive. */
		boolean run = true;
		/** This field is true while the runnable isn't paused. */
		public boolean running = true;

		/** The list of maps in this LifeRunnable. */
		protected List<Map> theMaps = new ArrayList<>();

		/** The real map. */
		RealMap realMap;
		/** The real map's size. */
		int realMapSize = Constants.GridMax;
		/** An object that serves to save the deltas of the map. */
		DeltasSaver realMapDeltasSaver;

		/** The number of time steps between two saves of the deltas. */
		final int deltasSavedEvery;
		/** The list of deltas savers. */
		protected List<DeltasSaver> theDeltasSavers = new ArrayList<>();


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
		 * @param realMap the map to set
		 */
		public void setRealMap(RealMap realMap) {
			this.realMap = realMap;
			theMaps.add(realMap);
			if (Constants.Save)
				this.realMap.setupLogFiles();
			this.realMapDeltasSaver = new DeltasSaver(realMap, dataFolderName, Constants.RealMapDeltasFileName + ".csv");
			theDeltasSavers.add(realMapDeltasSaver);
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
				if (realMap.getTime() == stopAt + 1) // '+ 1' in order to save the last time step
					this.kill();
			}
			mlog.say("dies");
		}

		protected void update() {
			// log time
			if (realMap.getTime() % TIME_PRINTING_GAP == 0)
				mlog.say("time = " + realMap.getTime());
			// save stuff about the map(s)
			saveDeltasAndOtherInformation();
			// actually update the map(s)
			updateMaps();
			// increment time
			for (Map map : theMaps) map.incrementTime();
		}

		/**
		 * Saves population and (maybe) saves deltas and (maybe) saves other information.
		 */
		protected void saveDeltasAndOtherInformation() {
			if (Constants.Save) {
				if (realMap.getTime() % Constants.SaveEvery == 0) {
					for (Map map : theMaps) map.setupLogFiles();
					save();
				}
				realMap.savePopulation();
			}
			if (realMap.getTime() % deltasSavedEvery == 0)
				updateDeltasSavers();
		}

		/** (Maybe) saves information about dead individuals, predation, alive individuals, the sensors. */
        public void save() {
			mlog.say("backup all logs");
			if (realMap.getDisplay() != null) {
				realMap.getDisplay().pauseProcedure(true);
				realMap.getDisplay().getSaveButton().setText("Saving...");
				realMap.getDisplay().screenshot();
			}
			saveMapsStates();
			if (realMap.getDisplay() != null) {
				realMap.getDisplay().getSaveButton().setText("Save");
				realMap.getDisplay().pauseProcedure(false);
			}
		}

		/** Generates a snapshot file and a sensors file for each map. */
		public void saveMapsStates() {
			for (Map map : theMaps)
				map.saveState(dataFolderName);
        }

		/** Saves the deltas of the map(s). */
		protected void updateDeltasSavers() {
			int threadCount = theDeltasSavers.size();
			ExecutorService executor = Executors.newFixedThreadPool(threadCount);

			for (DeltasSaver deltasSaver : theDeltasSavers)
				executor.submit(deltasSaver::update);

			// prevents new tasks to be submitted
			executor.shutdown();

			try {
				// wait for all threads to terminate
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
					// if time is out, force stop
					executor.shutdownNow();
					if (!executor.awaitTermination(30, TimeUnit.SECONDS))
						System.err.println("The threads pool didn't terminate cleanly.");
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
				System.err.println("Run interrupted: " + e.getMessage());
			}
		}


		/**
		 * <p>
		 * 		Updates each individual on the map(s).
		 * </p>
		 * <p>
		 *     TODO: make this method more generic to allow any number of maps
		 * </p>
		 */
		protected void updateMaps() {
			for (int i = 0; i < realMapSize; i++) {
				for (int j = 0; j < realMapSize; j++) {
					realMap.updateCell(i, j);
				}
			}
			realMap.applyChanges();
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
			realMap.kill();
			//worldmap
			String dname = properties.getProperty("sim_name");
			int cst_grid_max = Integer.parseInt(properties.getProperty("grid_max"));
			Display d = new Display(dname, this, dataFolderName);
			realMap = new RealMap(cst_grid_max, d, dataFolderName, Constants.SummaryFileName, Constants.PredationFileName, Constants.SnapshotFileName, Constants.SensorsFileName, Constants.PopulationFilename);

			// read creatures
			target = directory.getAbsolutePath() + "/" + Constants.SnapshotFileName + ".csv";
			// save all creatures by id
			HashMap<Integer, EmbodiedIndividual> individualMap = new HashMap<>();
			// read line by line
			Scanner sc;
			String[] lineArray;
			String line;
			int maxId = -1;
			try {
				sc = new Scanner(new File(target));
				// time header
				sc.nextLine();
				// time value
				line = sc.nextLine();
				int time = Integer.parseInt(line);
				realMap.setTime(time);
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
					realMap.addIndividual(x, y, individual);
					if (d != null)
						d.addComponent(individual);
				}
				sc.close();  //closes the scanner
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

			realMap.setGlobalId(maxId + 1);
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
		protected DeltasSaver shadowMapDeltasSaver;
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
		 * @param realMap the map to set
		 */
		@Override
		public void setRealMap(RealMap realMap) {
			super.setRealMap(realMap);
			this.shadowMap = new ShadowMap(realMap, Constants.ShadowModelSummaryFileName, Constants.ShadowModelSnapshotFileName, Constants.ShadowModelSensorsFileName);
			theMaps.add(shadowMap);
			if (Constants.Save)
				this.shadowMap.setupLogFiles();
			this.shadowMapDeltasSaver = new DeltasSaver(shadowMap, dataFolderName, Constants.ShadowMapDeltasFileName + ".csv");
			theDeltasSavers.add(shadowMapDeltasSaver);
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
		 * <p>
		 * 		Updates each individual on the real map and on the shadow map.
		 * </p>
		 */
		@Override
		protected void updateMaps() {
			// maybe reset the shadow model
			if (realMap.getTime() % shadowModelResetEvery == 0)
				shadowMap.reset();
			// compute changes on the real map
			for (int i = 0; i < realMapSize; i++) {
				for (int j = 0; j < realMapSize; j++) {
					realMap.updateCell(i, j);
				}
			}
			// compute changes on the shadow map
			shadowMap.incrementAgeOfAllIndividuals();
			shadowMap.createRandomIndividuals(realMap.getNbOfBirths());
			shadowMap.removeRandomIndividuals(realMap.getNbOfDeaths());
			// apply the changes
			Thread realMapThread = new Thread(() -> realMap.applyChanges());
			Thread shadowMapThread = new Thread(() -> shadowMap.applyChanges());
			realMapThread.start();
			shadowMapThread.start();
			try {
				realMapThread.join();
				shadowMapThread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}


		@Override
		public void load(File directory) {
			// TODO: this overriding method is probably necessary, because in this class, there there is a shadow model.
		}
	}

}
