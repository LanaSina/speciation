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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * @author lana
 * This class is the main class.
 *
 */
public class Starter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		MyLog mlog = new MyLog("starter",true);

		//get current date
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
		Date date = new Date();
		String strDate = dateFormat.format(date);
		String dataFolderName = Constants.DataPath + "/" + strDate + "/";

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


		// read configuration file
		Properties properties = new Properties();
		FileInputStream propsFile = null;
		try {
			propsFile = new FileInputStream("src/config.properties");
			properties.load(propsFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String dname = properties.getProperty("sim_name");
		int cst_grid_max= Integer.parseInt(properties.getProperty("grid_max"));

		Display d = new Display(dname);
		int lightLimit = 30;//30
		//islands
		double coarse = 0.002;
		int of = 10;
		
		int isle_size = 10;
		
		
		//worldmap		
		Map map = new Map(Constants.GridMax,d, dataFolderName);
		
		//initialize map (do it from file!!)
		for(int i=0; i<lightLimit; i++){
			for(int j=0; j<lightLimit; j++){
				int x = i+of;
				int y = j+of;
								
				/*if(Constants.uniformDouble()<coarse){
					
					/for(int k=0; k<isle_size; k++){
						for(int ll=0; ll<isle_size; ll++){
							int x = i+k+of;
							int y = j+ll+of;
							if(x<Constants.GridMax & y<Constants.GridMax){*/
								int id = map.incrementGlobalID();
								Individual l = new EmbodiedIndividual(x,y,id,0,0, -1);//IndividualV1(i+of,j+of,id,0,0, -1);// EmbodiedIndividual(i+of,j+of,id,0,0, -1);
								map.addIndividual(x, y, l);
								d.addComponent(l);	
							/*}
						}
					}
					
				}*/
				
			}
		}

		LifeRunnable life = new LifeRunnable(map);
		new Thread(life).start();
	}	
	
	
	public static class LifeRunnable implements Runnable{

		boolean run = true;
		MyLog mlog = new MyLog("lifeRunnable",true);

		//map
		Map map;
		int mapSize = Constants.GridMax;
		
		public LifeRunnable(Map map){
			//this.d = d;
			this.map = map;
		}
		
		public void run() {
			
			while(run){
				update();
				
				try {
					Thread.sleep(1);
							} catch (InterruptedException e) {
					e.printStackTrace();
				}		
			}
			
			mlog.say("dies");
			
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
	}

}
