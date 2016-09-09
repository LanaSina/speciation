package communication;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import animals.Individual;
import animals.IndividualV1;
import animals.Node;
import animals.Tree;
import startup.Constants;
import visualization.Display;

/**
 * a map with moving "light"
 * @author lana
 *
 */
public class NicheMap {
	MyLog mlog = new MyLog("map",true);
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
	
	public NicheMap(int mapSize, Display d){
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
		String str = "ID,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor\n";
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
		int x = (int)(i.position[0] +0.5);
		int y = (int)(i.position[1] +0.5);
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
	 * @param hasLight is there eternal light on this cell or not?
	 */
	public void updateCell(int x, int y, boolean hasLight){
		
		Cell c = map[x][y];
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
        				np[j] = position[j]-(creature.getEnergy()*Constants.SpeedFactor);
        			}
        			if(np[j]<0) np[j]=0;
        			if(np[j]>=Constants.GridMax-1) np[j] = Constants.GridMax-2;
        			if(((int)(np[j]+0.5) != (int)(position[j]+0.5))){
        				moved = true;
        			}
        		}
        		
        		//interactions between creatures
        		Tree sensors = creature.getSensors();
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
                		//iterate creatures on this cell
                		for(int m=0; m<c.creatures.size();m++){
                			if(remove.contains(c.creatures.get(m))){
                				continue;
                			}
                			//creature can't interact on itself
                			if(m==i){
                				continue;
                			}
                			
                			//TODO
                			/*if(value==c.creatures.get(m).properties[k]){
                				mlog.say("eat; property " + prop.data + " value "+value);
                				//record interaction
                				interacting.add(i);
                				interactedOn.add(m);
                				//get random action
                				ArrayList<Node> actions = pChildren.get(l).getChildren();
    							int ia = (int) (Constants.uniformDouble(0, actions.size()-1)+0.5);
    							int a = actions.get(ia).data;
                				interaction.add(a);
                			}*/
                		}
                	}
                }
        		
        		if(moved){
        			newPositions.add(np[0]);
        			newPositions.add(np[1]);
        			moving.add(creature);
        			//costs energy
        			double energy = creature.getEnergy()- (creature.getSpeed()*Constants.SpeedFactor*0.3);
        			creature.setEnergy(energy);
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
				//mlog.say("eat; ");
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
						//mlog.say("got "+e);
						double energy = c.creatures.get(predator).getEnergy() + e;
						c.creatures.get(predator).setEnergy(energy); 
						//mlog.say("total "+ c.creatures.get(predator).energy);
						//record prey as dead
						c.creatures.get(prey).setEnergy(0); //if(c.creatures.get(prey).color == Color.black) mlog.say("predator confusion 1");
						c.creatures.get(predator).setBorderColor(Color.black);
					}
				} else{
					//wound predator
					double energy = c.creatures.get(predator).getEnergy()-c.creatures.get(prey).getEnergy();
					c.creatures.get(predator).setEnergy(energy);
					//mlog.say("died "+ok);
					c.creatures.get(predator).setBorderColor(Color.red);
					//mlog.say("wounded "+ c.creatures.get(predator).energy);
				}
				
				//mlog.say("total "+ c.creatures.get(predator).energy);
			}
		}
		
//		if(!hadLight & (c.hasLight>0)){
//			globalID++;
//			int plus = 0;
//			double nx = 0;
//			double ny = 0;
//			if(generateBool()){
//				plus = 1;
//			} else {
//				plus = -1;
//			}
//			if(generateBool()){
//				nx = x+ plus*Constants.uniformDouble()*Constants.NicheSpeed;
//			} else {
//				ny = y + plus*Constants.uniformDouble()*Constants.NicheSpeed;
//			}
//			
//			nx = x +12.7;
//			if(nx<0) nx =0; if(nx>Constants.GridMax) nx=Constants.GridMax;
//			if(ny<0) ny =0; if(ny>Constants.GridMax) ny=Constants.GridMax;
//			
//			Individual l = new Individual(nx,ny,globalID,globalID,time, -1);
//			addIndividual((int)(nx+0.5), (int)(ny+0.5), l);
//			d.addComponent(l);		
//		}
		

	}
	
	public void updateMoved(){
		time++;
		
		//update dead
		for(int i=0; i<remove.size();i++){
			Individual creature = remove.get(i);
			if(!creature.isLight()){
				//write down info
				// "ID,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor\n";
				String str = creature.stringDesc();
				//mlog.say("parent "+creature.getParentID()+" self "+creature.getID());
				try {
					summaryWriter.append(str);
					summaryWriter.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				//replace by other light
				globalID++;
				int plus = 0;
				double nx = 0;
				double ny = 0;
				if(generateBool()){
					plus = 1;
				} else {
					plus = -1;
				}
				if(generateBool()){
					nx = creature.position[0]+ plus*Constants.uniformDouble()*Constants.NicheSpeed;
				} else {
					ny = creature.position[1] + plus*Constants.uniformDouble()*Constants.NicheSpeed;
				}
				
				nx = creature.position[0] +2.7;
				if(nx<0) nx =0; if(nx>=Constants.GridMax-1) nx=Constants.GridMax-2;
				if(ny<0) ny =0; if(ny>=Constants.GridMax-1) ny=Constants.GridMax-2;
				
				Individual l = new IndividualV1(nx,ny,globalID,globalID,time, -1);// Individual(nx,ny,globalID,globalID,time, -1);
				addIndividual((int)(nx+0.5), (int)(ny+0.5), l);
				d.addComponent(l);		
				
				mlog.say("************* replace light at "+ nx + " "+ny);
			}

			//remove from display
	    	d.removeComponent(creature);
	    	//remove from map
	    	removeIndividual(creature);
		}		
				
		//update moved
		for (int i = 0; i < moving.size(); i++) {
			Individual creature = moving.get(i);
			//new x,y	
			int nx = (int) (newPositions.get(i*2)+0.5);
			int ny = (int) (newPositions.get(i*2+1)+0.5);
			updatePosition(nx,ny,creature);
			creature.position[0] = newPositions.get(i*2);
			creature.position[1] = newPositions.get(i*2+1);  			
        }
		
		//add new babies
		for (int i = 0; i < babies.size(); i++) {
			Individual baby = babies.get(i);
			globalID++;
			baby.setID(globalID);
			//mlog.say("added to map");
			int nx = (int) (baby.position[0]+0.5);
			int ny = (int) (baby.position[1]+0.5);
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
	
	private class Cell{
		//int hasLight = 0;
		/** all creatures on this cell*/
		LinkedList<Individual> creatures;
		
		public Cell(){
			creatures = new LinkedList<Individual>();	
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
