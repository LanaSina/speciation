/**
 * 
 */
package startup;

import animals.EmbodiedIndividual;
import animals.Individual;
import communication.Map;
import communication.MyLog;
import visualization.Display;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

/**
 * @author lana
 * This class is the main class.
 *
 */
public class Starter {
	static String dataFolderName;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
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

		LifeRunnable life = new LifeRunnable();
		Display d = new Display(dname, life);
		int lightLimit = 30;//30
		int of = 10;

		//worldmap		
		Map map = new Map(cst_grid_max,d, dataFolderName);
		
		//initialize map (do it from file!!)
		for(int i=0; i<lightLimit; i++){
			for(int j=0; j<lightLimit; j++){
				int x = i+of;
				int y = j+of;

				int id = map.incrementGlobalID();
				Individual l = new EmbodiedIndividual(x,y,id,0,0, -1);
				map.addIndividual(x, y, l);
				d.addComponent(l);
			}
		}

		life.setMap(map);
		new Thread(life).start();
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
	
	public static class LifeRunnable implements Runnable{

		MyLog mlog = new MyLog("lifeRunnable",true);
		boolean run = true;
		public boolean running = true;

		//map
		Map map = null;
		int mapSize = Constants.GridMax;
		
		public LifeRunnable(){
		}

		public void setMap(Map map){
			this.map = map;
		}
		
		public void run() {
			
			while(run){
				if(running) {
					update();

					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				

			}
			
			mlog.say("dies");
		}

		public String save(){
			running = false;
			String fileName =  map.saveSate(dataFolderName, Constants.SnapshotFileName);
			String savedAt = dataFolderName + fileName;
			running = true;

			return savedAt;
		}

		
		/** updates each individual and each cell of the map */
		void update(){
			for(int i=0; i<mapSize;i++){
				for(int j=0; j<mapSize;j++){
					map.updateCell(i, j);
				}
			}
			map.updateMoved();
		}
		
		public void kill(){
			run = false;
		}

		public void load(File directory) {
			// read properties
			String target = directory.getAbsolutePath()+"/config.properties";
			//copy them
			Path copyTo = Paths.get(dataFolderName+"config.properties");
			try {
				Files.copy(Paths.get(target), copyTo, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			mlog.say("properties copied to " + dataFolderName);

			Properties properties = loadProperties(target);

			//worldmap
			String dname = properties.getProperty("sim_name");
			int cst_grid_max= Integer.parseInt(properties.getProperty("grid_max"));
			Display d = new Display(dname, this);
			int lightLimit = 30;//30
			int of = 10;

			Map map = new Map(cst_grid_max,d, dataFolderName);

			// read creatures
			target = directory.getAbsolutePath()+"/"+Constants.SummaryFileName;

			// read line by line
			Scanner sc = null;
			try {
				sc = new Scanner(new File(target));
				// header
				// String str = "ID,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor,nkids,pgmDeath,matForKids"+"\n";
				sc.nextLine();
				sc.useDelimiter(",");   //sets the delimiter pattern
				while (sc.hasNext()){
					int id = sc.nextInt();

					EmbodiedIndividual individual = new EmbodiedIndividual(id);
					individual.setParentID(sc.nextInt());
					individual.setBirthDate(sc.nextInt());
					// lifeSpan
					sc.nextInt();
					individual.setSpeed(sc.nextInt());
					individual.setMaxEnergy(sc.nextInt());
					individual.setKidEnergy(sc.nextInt());
					individual.setSensors(); //uh oh

				}
				sc.close();  //closes the scanner
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

		//initialize map (do it from file!!)
			for(int i=0; i<lightLimit; i++){
				for(int j=0; j<lightLimit; j++){
					int x = i+of;
					int y = j+of;

					int id = map.incrementGlobalID();
					Individual l = new EmbodiedIndividual(x,y,id,0,0, -1);
					map.addIndividual(x, y, l);
					d.addComponent(l);
				}
			}

			this.setMap(map);
		}
	}

}
