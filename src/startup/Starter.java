/**
 * 
 */
package startup;

import animals.EmbodiedIndividual;
import animals.Individual;
import communication.Map;
import communication.MyLog;
import visualization.Display;

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
		
		String dname = "sensor cost high";
		Display d = new Display(dname);
		int lightLimit = 30;//30
		int of = 10;
		//worldmap		
		Map map = new Map(Constants.GridMax,d);
		
		//initialize map (do it from file!!)
		for(int i=0; i<lightLimit; i++){
			for(int j=0; j<lightLimit; j++){
				int id = map.incrementGlobalID();
				//mlog.say("id "+id);
				Individual l = new EmbodiedIndividual(i+of,j+of,id,0,0, -1);//IndividualV1(i+of,j+of,id,0,0, -1);// EmbodiedIndividual(i+of,j+of,id,0,0, -1);
				//mlog.say("id "+l.getID());
				map.addIndividual(i+of, j+of, l);
				d.addComponent(l);		
			}
		}
		
//
//		of = lightLimit +of+ 8;
//		for(int i=0; i<lightLimit; i++){
//			for(int j=0; j<lightLimit; j++){
//				int id = map.incrementGlobalID();
//				//mlog.say("id "+id);
//				Individual l = new Individual(i+of,j+of,id,0,0, -1);
//				//mlog.say("id "+l.getID());
//				map.addIndividual(i+of, j+of, l);
//				d.addComponent(l);		
//			}
//		}
		
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
				
				//mlog.say("runs "+run);
				
				update();
				
				try {
					Thread.sleep(10);
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
					boolean hasLight = false;
					if((i<15) &(j<15)) hasLight = true; //useless?
					map.updateCell(i, j, hasLight);
				}
			}
			map.updateMoved();
		}
		
		public void kill(){
			run = false;
			
		}
	}

}
