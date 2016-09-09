package communication;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import animals.EmbodiedIndividual;
import animals.Individual;
import animals.Node;
import animals.Tree;
import startup.Constants;
import visualization.Display;

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
		FileBuilder fb = new FileBuilder();
		summaryWriter = fb.getFileWriter();
		fb = null;
		//csv file header
		String str = "ID,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor,nkids,pgmDeath\n";
		//sensors will just be 0 or 1. People who eat others get their own file.
		//Parents and kids have the same ID.
    	try {
			summaryWriter.append(str);
			summaryWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void addIndividual(int x, int y, Individual i){
		Cell c = map[x][y];
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
	 * 
	 * @param x1 old x
	 * @param y1 old y
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
		boolean hadLight = false;
		//interactions: max is everyone interacts with everyone
		ArrayList<Integer> interacting = new ArrayList<Integer>();
		ArrayList<Integer> interactedOn = new ArrayList<Integer>();
		ArrayList<Integer> interaction = new ArrayList<Integer>();
		
		for (int i = 0; i < size; i++) {
			Individual creature = c.creatures.get(i);
            boolean alive = creature.update(babies, time);
            double[] position = creature.getPosition();
            
            if(!alive){
            	remove.add(creature);
            } else{
            	//is light?
            	if(creature.isLight()){
            		hadLight = true;
            	}
            	//move
            	double np[] = new double[2];
            	boolean moved = false;
        		for(int j=0;j<2;j++){
        			if(generateBool()){
//        				if(creature.speed>0)
//        					creature.color = Color.blue;
        				np[j]= position[j]+(creature.getSpeed()*Constants.SpeedFactor);
        			}else{
        				np[j] = position[j]-(creature.getSpeed()*Constants.SpeedFactor);
        			}
        			if(np[j]<0) np[j]=0;
        			if(np[j]>=Constants.GridMax-1) np[j] = Constants.GridMax-2; //something wrong but what
        			if(np[j] != position[j]){
        				moved = true;
        			}
        		}
        		
        		Tree sensors = creature.getSensors();
        		
        		//interactions between creatures
                Node s = sensors.root;
                //iterate on properties
                ArrayList<Node> sChildren = s.getChildren();
                //mlog.say("c " + sChildren.size());
                for(int k=0; k<sChildren.size();k++){
                	//this is the property
                	Node prop = sChildren.get(k);
                	//these are the value-action pairs
                	ArrayList<Node> pChildren = prop.getChildren();
                	//mlog.say("c " + pChildren.size());
                	
                	for(int l=0; l<pChildren.size();l++){
                		int value = pChildren.get(l).data;
                		//iterate creatures on this cell
                		for(int m=0; m<c.creatures.size();m++){
                			Individual cr2 = c.creatures.get(m);
                			if(remove.contains(c.creatures.get(m)) | (cr2.isLight())){
                				continue;
                			}
                			//creature can't interact on itself
                			if(m==i){
                				continue;
                			}
                			
                			//based on direct perception of other creatures properties
                			/*int[] properties = cr2.getProperties();
                			if(value == properties[k]){
                				if(Constants.uniformDouble()>0.6){
                					mlog.say("eat; property " + prop.data + " value "+value);
                				}
                				//record interaction
                				interacting.add(i);
                				interactedOn.add(m);
                				//get random action
                				ArrayList<Node> actions = pChildren.get(l).getChildren();
    							int ia = (int) (Constants.uniformDouble(0, actions.size()-1)+0.5);
    							int a = actions.get(ia).data;
                				interaction.add(a);
                			}//*/
                			
                			//based on perception of cell properties
                			//TODO later will be based on properties gradient, when one individual can affect several cells
                			
                			//value is integer between 0:10
                			double ind_prop = c.getProp(k);
                			
                			if( (value == (int) (Constants.propGrain*ind_prop)) ){
                				/*if(Constants.uniformDouble()<0.001){
                					mlog.say("eat; pro " + k + " value "+value );
                				}*/
                				//record interaction
                				interacting.add(i);
                				interactedOn.add(m);
                				//get random action
                				ArrayList<Node> actions = pChildren.get(l).getChildren();
    							int ia = (int) (Constants.uniformDouble(0, actions.size()-1)+0.5);
    							int a = actions.get(ia).data;
                				interaction.add(a);
                			}//*/

                		}
                	}
                }
        		
        		if(moved){
        			newPositions.add(np[0]);
        			newPositions.add(np[1]);
        			moving.add(creature);
        			//costs energy
        			if(!creature.isLight()){
        				double energy = creature.getEnergy() - creature.getSpeed()*Constants.SpeedCost;//0.15;//0.2//(creature.speed*Constants.SpeedFactor*0.3);//make motion expensive
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
		Collections.shuffle(randomList);
		
		for(int j=0;j<randomList.size();j++){
			int i = randomList.get(j);
			
			//is interaction still valid?
			if((interacting.get(i)<0) || (interactedOn.get(i)<0)){
				continue;
			}
			
			
			//if "eat", delete prey and turn predator to black
			if(interaction.get(i) == Constants.ActEat){
				int predator = interacting.get(i);
				int prey = interactedOn.get(i);
				//are energies compatible with this?
				double ok = c.creatures.get(predator).getEnergy() - c.creatures.get(prey).getEnergy();
				if(ok>=0){					
					//delete prey from arrays
					Collections.replaceAll(interacting, prey,-1);
					Collections.replaceAll(interactedOn, prey,-1);
					//give energy to predator
					double e = c.creatures.get(prey).getEnergy();
					if(e>0){
						double energy = c.creatures.get(predator).getEnergy() + e;
						c.creatures.get(predator).setEnergy(energy); 
						//mlog.say("total "+ c.creatures.get(predator).energy);
						//record prey as dead
						c.creatures.get(prey).setEnergy(0); //if(c.creatures.get(prey).color == Color.black) mlog.say("predator confusion 1");
						
						//change predator color 
//						if((c.creatures.get(predator).color == Color.blue) | (c.creatures.get(predator).color == Color.gray)){
//							c.creatures.get(predator).color = Color.gray;
//						}else{
							c.creatures.get(predator).setBorderColor(Color.black);
//						}
					}
				} else if(ok<=0){
					double ePred = c.creatures.get(predator).getEnergy();
					double ePrey = c.creatures.get(prey).getEnergy();
					//wound predator
					double energy = ePred-ePrey*Constants.ErrorCost/2;
					c.creatures.get(predator).setEnergy(energy);
					//wound prey 
					energy = ePrey-ePred*Constants.ErrorCost;
					c.creatures.get(prey).setEnergy(energy);
					//mlog.say("died "+ok);
					c.creatures.get(predator).setBorderColor(Color.red);
					c.creatures.get(prey).setBorderColor(Color.gray);
					//mlog.say("wounded "+ c.creatures.get(predator).energy);
				}
			}
		}
		
		/*if(!hadLight & hasLight){
			mlog.say("======= not called ?");
			globalID++;
			Individual l = new EmbodiedIndividual(x,y,globalID,globalID,time, -1);
			addIndividual(x, y, l);
			d.addComponent(l);		
		}*/
		

	}


	public void updateMoved(){
		time++;
		
		//update dead
		for(int i=0; i<remove.size();i++){
			Individual creature = remove.get(i);
			if(!creature.isLight() & !creature.parentIsLight()){
				//write down info
				// "ID,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor\n";
				String str = creature.stringDesc();
				//mlog.say(str);
				//mlog.say("parent "+creature.getParentID()+" self "+creature.getID());
				try {
					summaryWriter.append(str);
					summaryWriter.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
			//mlog.say("r "+r);
			
			return r;
		}

		/**
		 * 
		 * @param t transparency
		 * @param d density
		 */
		public void changeProperties(double t, double d){
			transparency+=t;
			density+=d;
			
			if(transparency<0){
				transparency = 0;
			} else if (transparency>1){
				transparency = 1;
			}
			if(density<0){
				density = 0;
			} else if (density>1){
				density = 1;
			}
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
			electric = electric/creatures.size();;
		}
		
		public double getLuminosity(){
			return luminosity;
		}
		
	}
	
	private boolean generateBool(){
		boolean b = false;
		if(Constants.uniformDouble()>0.5){
			b = true;
		}
		
		return b;
	}
}
