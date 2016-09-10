package animals;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedList;

import startup.Constants;
import visualization.GraphicalComponent;

import communication.MyLog;

public class IndividualV1 implements GraphicalComponent, Individual{
	MyLog mlog = new MyLog("individual",true);
	
	//general
	public int speed = 0;
	//int lifespan;
	int maxEnergy;
	//energy level transmitted to offspring
	int haveKids = 2;//(int)(Constants.uniformDouble(0,4)-2+0.5)+2;
	//nr of kids
	private int nKids =  2;//(int)(Constants.uniformDouble(0,3)-1+0.5)+1;
	//sensors: map of property value to action
	public int death =  (int)(Constants.uniformDouble(0,4)-2+0.5)+20;//50
	//
	public Tree sensors;
	int nProperties = 6; //sum of above
	public int[] properties = new int[nProperties-1];
	//energy lost per tour
	//double stepCost = 0.3;
	//for data writing
	int ID;
	int parentID;
	private int firstAncestorID;
	//if parent is light, won't be written down (data too big)
	public boolean parentIsLight = false;
	int birthDate;
	//life span
	int life = 0;
	
	//particular
	public double energy;
	public double[] position = new double[2];
	public Color color;
	public Color borderColor = Color.white;
	//for eternal light cells
	public boolean isLight = false;	

	
	/**
	 * creates "light" at specified postion
	 * @param x
	 * @param y
	 * @param glID id to give to this individual
	 * @param ancestor id of the 1st ancestor
	 * @param date in-simulation time
	 * @param parent parent id
	 */
	public IndividualV1(double x, double y, int glID, int ancestor, int date, int parent){
		position[0] = x;
		position[1] = y;
		
		ID = glID;	
		firstAncestorID = ancestor;
		parentID = parent;
		//mlog.say("id " + ID);
		birthDate = date;
		
		speed = 1; //(int)(Constants.uniformDouble(0,1)+0.5)+1;
		//lifespan = 1;
		maxEnergy = 2;// (int)(Constants.uniformDouble(0,4)-2+0.5)+2;
		energy = 3;//+(int)(Constants.uniformDouble(0,2)+0.5);//(unbiasing)//maxEnergy;
		isLight = true; mlog.say("--------------- IS LIGHT");
		
		properties[0] = speed;
		properties[1] = maxEnergy;
		properties[2] = haveKids;
		properties[3] = getNKids();
		properties[4] =  death;
		
		sensors = new Tree(0);//root is not important
		for(int i=0; i<(nProperties-1);i++){
			Node prop = new Node();
			prop.data = i;
			sensors.root.addChild(prop);
		}

		makeColor();		
	}
	
	/**
	 * clones with mutations
	 * @param in individual to be cloned
	 * @param glID id of this one
	 * @param date birth date
	 */
	IndividualV1(IndividualV1 in, int glID, int date){
		copy(in);
		if(in.isLight) parentIsLight = true;
		
		ID = glID;
		firstAncestorID = in.getAncestor();
		//if your ancestor is the sun, then ou're the new first ancestor
		if(firstAncestorID<0){
			mlog.say("EEEEERRRROOOR");
		}

		if((firstAncestorID==0) && (ID!=-1)){
			firstAncestorID = ID;
		}

		energy = in.haveKids;
		birthDate = date;
		parentID = in.ID;
		
		//copy sensor map
		sensors = in.sensors.copy();
		//spawn at different postion
		//random close position
		int i=1, j=1;
		if(generateBool()){
			i = -1;
		}
		if(generateBool()){
			j = -1;
		}		
		if(parentIsLight){
			position[0] = (in.position[0]+i*Constants.LightBirthDistance*Constants.uniformDouble());
			position[1] = (in.position[1]+j*Constants.LightBirthDistance*Constants.uniformDouble());
		} else {
			position[0] = (in.position[0]+i*Constants.BirthDistance*Constants.uniformDouble());
			position[1] = (in.position[1]+j*Constants.BirthDistance*Constants.uniformDouble());
		}
		
		for(int k=0;k<2;k++){
			if(position[k]<0) position[k]=0;
			if(position[k]>=Constants.GridMax-1) position[k] = Constants.GridMax-2;
		}
		
		if(generateBool()){

			int mut = (int) (Constants.uniformDouble(0, nProperties-1)+0.5);
			int plus = 1;
			if(generateBool()){
				plus = -1;
			}
			
			//do this after too
			properties[0] = speed;
			properties[1] = maxEnergy;
			properties[2] = haveKids;
			properties[3] = getNKids();
			properties[4] =  death;

			//maybe make this a mutable value!
			double bias = 0.3;
			//TODO don't need switch anymore?
			//switch(mut){
				//case 0:{
				if(generateBool(bias)){
					speed += plus;//speed + plus*0.7;
					if(speed<0) speed = 0;
					if(speed>Constants.speedMax) speed = Constants.speedMax;
					//break;
				}
				//case 1:{
				if(generateBool(bias)){
					maxEnergy += plus;
					if(maxEnergy<0) maxEnergy = 0;
					//break;
				}
				//case 2:{
				if(generateBool(bias)){
					haveKids+=plus;
					if(haveKids<0) haveKids = 0;
					//break;
				}
				//case 3:{		
				if(generateBool(bias)){
					//create or modify sensor
					if(plus>0){
						//property (-1 = size; -2 = size - sensormap)
						int prop = (int) (Constants.uniformDouble(0, nProperties-2)+0.5);
						//sensor exists for this property?
						ArrayList<Node> props = sensors.root.getChildren();
						ArrayList<Node> senses = props.get(prop).getChildren();
						//mlog.say("**** create or modify ");
						//modify
						if(generateBool()){
							//mlog.say("**** modify ");

							//tree nodes: root-> 3properties -> detectionValue -> action
							//          0          id              value            id
							boolean hasSensors = !senses.isEmpty();
							if(hasSensors){
								
								//get random sensor
								int s = (int) (Constants.uniformDouble(0, senses.size()-1)+0.5);
								Node sensor = senses.get(s);	
								sensor.data = sensor.data + (int)(Constants.uniformDouble(0, 1)+0.5)-1;
								mlog.say("---- sensor " + prop + " data "+ sensor.data);
							}
						} else {
							//create sensor. 
							Node value = new Node();
							//bias it to be like self
							int selfValue = properties[prop];
							if(generateBool()){//cannibal
								value.data = selfValue; //(int) (selfValue + Constants.uniformDouble(0, 4)+0.5)-2;		
							} else {//0 to 4
								value.data = (int) (Constants.uniformDouble(0, 4)+0.5);		
							}
							mlog.say("*** sensor " + prop + " data "+ value.data);
							Node action = new Node();
							action.data = (int) (Constants.uniformDouble(0, Constants.ActionTypes-1)+0.5);
							value.addChild(action);
							
							//props.get(prop).addChilde(value, value.getChildCount());
							sensors.root.getChildren().get(prop).addChild(value);
						}							
					}else{
						//or delete sensor
						//tree nodes: root-> 3properties -> detectionValue -> action
						//property (-1 = size; -2 = size - sensormap)
						int prop = (int) (Constants.uniformDouble(0, nProperties-2)+0.5);
						//sensor exists for this property?
						ArrayList<Node> props = sensors.root.getChildren();
						ArrayList<Node> sensors = props.get(prop).getChildren();
						boolean hasSensors = !sensors.isEmpty();
						if(hasSensors){
							int sens = (int) (Constants.uniformDouble(0, sensors.size()-1)+0.5);
							props.get(prop).removeChild(sens);
						}
						
					}
				//	break;
				}
				//case 4:{
				if(generateBool(bias)){
					setNKids(getNKids() + plus);
					if(getNKids()<0) setNKids(0);
					//break;
				}
				//case 5:{
				if(generateBool(bias)){
					death = death + plus;
					if(death<0) death = 0;
					//break;
				}
			//}
		}

		properties[0] = speed;
		properties[1] = maxEnergy;
		properties[2] = haveKids;
		properties[3] = getNKids();
		properties[4] =  death;

		
		makeColor();
	}
	
	private void makeColor() {
		int red = (hasSensors()-(nProperties-1))*256/(2*10);
		if(red>255) red = 255; if(red<0) red =0;
		//red = 255-red;
		int green = maxEnergy*255/70;//20
		//green = 255-green;
		if(green>255) green = 255; if(green<0) green =0;
		int blue = haveKids*255/10;//13
		if(blue>255) blue = 255; if(green<0) green =0;
		//blue = 255 - blue;
		color = new Color(red,green,blue);
	}

	/**
	 * creates from parents
	 * @param p1
	 * @param p2
	 */
//	Individual(Individual p1, Individual p2){
//		
//	}
	
	public boolean update(LinkedList<Individual> babies, int date, double t){
		life= life+1;
		
//if(color == Color.black) mlog.say("energy before "+energy);
		//if(color == Color.black) mlog.say("energy before "+energy);
		
		//remove energy due to sensors
		double se = sensors.root.getChildCount() - (nProperties-1);
		//mlog.say("se "+se);
		if(!isLight){
			energy = energy - se*Constants.SensorCost - Constants.StepCost;	//6*0.2//3*10
			if(life == death){
				energy = -1;
			}
		//energy = energy ;	
		} else {
			//free energy into light
			energy = energy + Constants.FreeEnergy;//0.4
		}
	
		if((energy>=maxEnergy) & (energy>=haveKids)){
			//add children to the map		
			int n = 0;
			if(isLight){
				while(energy-haveKids>0){
					IndividualV1 baby = new IndividualV1(this, -1, date);
					babies.add(baby);
					energy = energy-haveKids;
					n++;
				}
			}else{
				while((energy-haveKids>=0) & (n<getNKids())){
					IndividualV1 baby = new IndividualV1(this, -1, date);
					babies.add(baby);
					energy = energy-haveKids;
					
					n++;
				}
			}
			borderColor = Color.green;
		}	
				
		if(energy<=0){
			return false;
		}
	
		return true;
	}

	
	private void copy(IndividualV1 in){
		this.color = in.color;
		this.position = in.position.clone();
		this.maxEnergy = in.maxEnergy;
		this.energy = maxEnergy; //TODO change
		//this.lifespan = in.lifespan;
		this.haveKids = in.haveKids;
		this.speed = in.speed;
		this.setNKids(in.getNKids());
		this.death = in.death;
		
		this.sensors = in.sensors.copy();
	}
	
	public void draw(Graphics g, int gridStep) {
		
		//draw a yellow square .
		Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);
        
        int x = (int) (position[0]*gridStep +0.5);
        int y = (int) (position[1]*gridStep +0.5);
        int size = 8;
        if(isLight){
        	g2d.drawRect(x, y, size,size);
        }else{
        	g2d.fillRect(x, y, size,size);
        }      
        g2d.setColor(borderColor);
        g2d.drawRect(x, y, size,size);
	}
	
	/**
	 * 
	 * @param bias between 0 and 1; probability to return true.
	 * @return
	 */
	private boolean generateBool(double bias){
		boolean b = false;
		if(Constants.uniformDouble()>bias){
			b = true;
		}
		
		return b;
	}
	
	private boolean generateBool(){
		boolean b = false;
		if(Constants.uniformDouble()>0.5){
			b = true;
		}
		
		return b;
	}
	
	public int getID(){
		return ID;
	}
	
	public void setID(int id){
		ID = id;
		if(firstAncestorID == 0) firstAncestorID=id;
	}
	
	public int getParentID(){
		return parentID;
	}
	
	public int getBirth(){
		return birthDate;
	}
	
	public int getLifeSpan(){
		return life;
	}

	public double getSpeed(){
		return speed;
	}
	
	public int getMaxEnergy(){
		return maxEnergy;
	}
	
	public int getKidEnergy(){
		return haveKids;
	}
	
	public int getAncestor(){
		return firstAncestorID;
	}
	
	public int hasSensors(){
		int n = 0; 
	
		n = sensors.root.getChildCount();
		
		return n;
	}

	public int getNKids() {
		return nKids;
	}

	public void setNKids(int nKids) {
		this.nKids = nKids;
	}

	public boolean isLight() {
		return isLight;
	}

	public double[] getPosition() {
		return position;
	}

	public Tree getSensors() {
		return sensors;
	}

	public double getEnergy() {
		return energy;
	}

	public void setEnergy(double energy) {
		this.energy = energy;
	}

	public void setBorderColor(Color color) {
		borderColor = color;
	}

	public boolean parentIsLight() {
		return parentIsLight;
	}

	public String stringDesc() {
		String description =  ID +","+parentID+","+birthDate+","+life+","
				+ speed+","+maxEnergy+","+ getKidEnergy()+","
				+ hasSensors() +","+ getAncestor() + "," + getNKids() + ","
				+ death + "\n";
		return description;
	}

	/**
	 * irrelevant to this implementation
	 */
	public double getLuminosity() {
		return 0;
	}

	public int[] getProperties() {
		return properties;
	}
	
	public void setPosition(double[] position2) {
		position[0] = position2[0];
		position[1] = position2[1];
	}

	public double getWarm() {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getLoud() {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getSmelly() {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getElectric() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setEaten(int i) {
		// TODO Auto-generated method stub
		
	}

	public void setCellTransparency(double transparency) {
		// TODO Auto-generated method stub
		
	}
}
