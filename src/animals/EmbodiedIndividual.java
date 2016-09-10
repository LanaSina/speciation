package animals;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedList;

import startup.Constants;
import visualization.GraphicalComponent;

import communication.MyLog;

public class EmbodiedIndividual implements GraphicalComponent, Individual{
	MyLog mlog = new MyLog("embodied ind",true);
	
	//cell
	/** 1 = completely transparent*/
	double cellTransparency = 1;
	
	//general
	public int speed = 0;
	//total cost per unit
	double speedCost = 3;
	//death by being eaten
	int eaten = 0;//1 = true;

	//int lifespan;
	int maxEnergy;
	//energy level transmitted to offspring
	int kidEnergy = 2;
	//level of energy at which to have kids
	int matForKids = 4;
	//nr of kids
	private int nKids =  2;
	//sensors: map of property value to action
	public int death =  (int)(Constants.uniformDouble(0,4)-2+0.5)+20;//50
	
	
	//detectable properties (0..1)
	/** how visible*/
	double luminosity;
	double warm;
	double loud;
	double smelly;
	double electric;
	
	//will be replaced by cell properties sensors
	/** a tree with properties->pair(value,action)*/
	public Tree sensors;

	int nProperties = 7;//sum of above
	int nPhysicalProperties = 5;
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
	private double[] position = new double[2];
	public Color color;
	public Color borderColor = Color.white;
	//for eternal light cells
	private boolean isLight = false;
	
	
	/**
	 * creates "light" at specified postion
	 * @param x
	 * @param y
	 * @param glID id to give to this individual
	 * @param ancestor id of the 1st ancestor
	 * @param date in-simulation time
	 * @param parent parent id
	 */
	public EmbodiedIndividual(double x, double y, int glID, int ancestor, int date, int parent){
		position[0] = x;
		position[1] = y;
		
		ID = glID;	
		firstAncestorID = ancestor;
		parentID = parent;
		//mlog.say("id " + ID);
		birthDate = date;
		
		speed = 0; //1
		//lifespan = 1;
		maxEnergy = 4;
		energy = 3;//+(int)(Constants.uniformDouble(0,2)+0.5);//(unbiasing)//maxEnergy;
		isLight = true;
		//mlog.say("--------------- IS LIGHT");
		
		properties[0] = speed;
		properties[1] = maxEnergy;
		properties[2] = kidEnergy;
		properties[3] = getNKids();
		properties[4] =  death;
		properties[5] = matForKids;
		
		sensors = new Tree(0);//root is not important
		for(int i=0; i<(nPhysicalProperties);i++){
			Node prop = new Node();
			prop.data = i;
			sensors.root.addChild(prop);
		}

		makeColor();	
		makePhysics();
	}
	
	/**
	 * clones with mutations
	 * @param in individual to be cloned
	 * @param glID id of this one
	 * @param date birth date
	 */
	EmbodiedIndividual(EmbodiedIndividual in, int glID, int date){
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

		energy = in.kidEnergy;
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

			/*int plus = 1;
			if(generateBool()){
				plus = -1;
			}*/
			
			int plus = (int)(Constants.uniformDouble(-3, 3)+0.5);
			
			//do this after too
			properties[0] = speed;
			properties[1] = maxEnergy;
			properties[2] = kidEnergy;
			properties[3] = getNKids();
			properties[4] =  death;
			properties[5] = matForKids;

			//maybe make this a mutable value!
			double bias = 0.3;
			if(generateBool(bias)){
				speed += plus;//speed + plus*0.7;
				if(speed<0) speed = 0;
				if(speed>Constants.speedMax) speed = Constants.speedMax;
				//break;
			}
			if(generateBool(bias)){
				maxEnergy += plus;
				if(maxEnergy<0){
					maxEnergy = 0;
				}else if (maxEnergy>Constants.energyMax) {
					maxEnergy = Constants.energyMax;
				}
			}
			if(generateBool(bias)){
				kidEnergy+=plus;
				if(kidEnergy<0) kidEnergy = 0;
			}
			if(generateBool(bias)){
				matForKids+=plus;
				if(matForKids<0) matForKids = 0;
			}
			if(generateBool(bias)){
				//create or modify sensor
				if(plus>0){
					int prop = (int) (Constants.uniformDouble(0, nPhysicalProperties-1)+0.5);
					//mlog.say("****  prop " + prop);
					//sensor exists for this property?
					ArrayList<Node> props = sensors.root.getChildren();
					ArrayList<Node> senses = props.get(prop).getChildren();
					//mlog.say(" senses "+ senses.size());
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
							int value = (int) (sensor.data + Constants.uniformDouble(-2, 2));
							if(value<0){
								value = 0;
							}else if (value>Constants.propGrain) {
								value = Constants.propGrain;
							}
							sensor.data = value;
							//mlog.say("---- sensor " + prop + " data "+ sensor.data);
						}
					} else {
						//create sensor. 
						//mlog.say("**** create ");
						Node value = new Node();
						//bias it to be like self
						/*int selfValue = properties[prop];
						if(generateBool()){//cannibal
							value.data = selfValue; //(int) (selfValue + Constants.uniformDouble(0, 4)+0.5)-2;		
						} else {//0 to 4
							value.data = (int) (Constants.uniformDouble(0, 4)+0.5);		
						}*/
						
						value.data = (int) Constants.uniformDouble(0, 10);
						
						//mlog.say("*** sensor " + prop + " data "+ value.data);
						Node action = new Node();
						action.data = (int) (Constants.uniformDouble(0, Constants.ActionTypes-1)+0.5);
						value.addChild(action);
						
						sensors.root.getChildren().get(prop).addChild(value);
						//mlog.say("c " + sensors.root.getChildren().get(prop).getChildren().size());

					}							
				}else{
					//mlog.say("**** delete ");
					//or delete sensor
					//tree nodes: root-> 3properties -> detectionValue -> action
					//property (-1 = size; -2 = size - sensormap)
					int prop = (int) (Constants.uniformDouble(0, nPhysicalProperties-1)+0.5);
					//sensor exists for this property?
					ArrayList<Node> props = sensors.root.getChildren();
					ArrayList<Node> sensors = props.get(prop).getChildren();
					boolean hasSensors = !sensors.isEmpty();
					if(hasSensors){
						int sens = (int) (Constants.uniformDouble(0, sensors.size()-1)+0.5);
						props.get(prop).removeChild(sens);
					}
					
				}
			}
			if(generateBool(bias)){
				setNKids(getNKids() + plus);
				if(getNKids()<0) setNKids(0);
			}
			if(generateBool(bias)){
				death = death + plus;
				if(death<0) death = 0;
			}
		}

		properties[0] = speed;
		properties[1] = maxEnergy;
		properties[2] = kidEnergy;
		properties[3] = getNKids();
		properties[4] =  death;
		properties[5] = matForKids;
		
		makePhysics();
		makeColor();
	}
	
	/** calculate luminosity etc with a random formula*/
	private void makePhysics() {
		double midSpeed = Math.abs(speed-(Constants.speedMax/2));
		luminosity = (matForKids+midSpeed)/(double)(maxEnergy+ (Constants.speedMax/2));//(maxEnergy+midSpeed)/(Constants.energyMax + (Constants.speedMax/2));
		warm = (speed+energy)/(double)(Constants.speedMax+maxEnergy);
		if(maxEnergy>=kidEnergy && maxEnergy>0){
			loud = kidEnergy/(double)(1+maxEnergy);
		}else{
			loud = 1;
		}
		smelly = (kidEnergy+matForKids)/(double)(death+1+maxEnergy);
		electric = warm*smelly;
	}
	
			
	private void makeColor() {
		int red = (hasSensors()-(nPhysicalProperties-1))*256/(2*10);
		if(red>255) red = 255; if(red<0) red =0;
		//red = 255-red;
		int green = maxEnergy*255/70;//20
		//green = 255-green;
		if(green>255) green = 255; if(green<0) green =0;
		int blue = kidEnergy*255/10;//13
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
	
	public boolean update(LinkedList<Individual> babies, int date, double transparency){
		life= life+1;
		cellTransparency = transparency;
		
		//remove energy due to sensors
		double se = sensors.root.getChildCount() - (nPhysicalProperties-1);
		//mlog.say("se "+se);
		if(!isLight){
			energy = energy - se*Constants.SensorCost - Constants.StepCost*maxEnergy;	//6*0.2//3*10
			//mlog.say("energy "+energy);
			if(life == death){
				energy = -1;
			}
			//kill the immortals with 0 kids
			if(generateBool(0.005)){
				energy = -1;
			}
			if(energy>maxEnergy){
				energy=maxEnergy;
			}
		} else {
			//free energy into light
			energy = energy + Constants.FreeEnergy;
		}
		
		if(energy>0 && energy>=matForKids){//(energy>=maxEnergy) & 
			//add children to the map		
			int n = 0;
			if(isLight){
				while(energy-kidEnergy>0){//matForKids
					EmbodiedIndividual baby = new EmbodiedIndividual(this, -1, date);
					babies.add(baby);
					energy = energy-kidEnergy;
					n++;
				}
			}else{
				while((energy>kidEnergy)& (n<getNKids())){//
					EmbodiedIndividual baby = new EmbodiedIndividual(this, -1, date);
					babies.add(baby);
					energy = energy-kidEnergy;
					n++;
				}
				if(n<getNKids()){
					energy =-1;
				}
			}
			borderColor = Color.green;
		}	
				
		if(energy<=0){
			return false;
		}
	
		return true;
	}

	
	private void copy(EmbodiedIndividual in){
		this.color = in.color;
		this.position = in.position.clone();
		this.maxEnergy = in.maxEnergy;
		this.energy = kidEnergy;
		//this.lifespan = in.lifespan;
		this.kidEnergy = in.kidEnergy;
		this.speed = in.speed;
		this.matForKids = in.matForKids;
		this.setNKids(in.getNKids());
		this.death = in.death;
		this.cellTransparency = in.cellTransparency;
		
		this.sensors = in.sensors.copy();
	}
	
	public void draw(Graphics g, int gridStep) {
		if(!parentIsLight){
		
			//draw a yellow square .
			Graphics2D g2d = (Graphics2D) g;
	        Color c = new Color(color.getRed()/255.0f, color.getGreen()/255.0f, color.getBlue()/255.0f, (float)(cellTransparency));
	        g2d.setColor(c);
	        
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
	}
	
	/**
	 * 
	 * @param bias between 0 and 1; probability to return true.
	 * @return
	 */
	private boolean generateBool(double bias){
		boolean b = false;
		if(Constants.uniformDouble()<bias){
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
		return kidEnergy;
	}
	
	public int getAncestor(){
		return firstAncestorID;
	}
	
	public int hasSensors(){
		int n = 0; 
	
		n = sensors.root.getChildCount();
		
		return n;
	}
	
	public double getLuminosity() {
		return luminosity;
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

	public void setBorderColor(Color color) {
		borderColor = color;
	}

	public boolean parentIsLight() {
		return parentIsLight;
	}

	/**
	 * @return a csv string description of this creature
	 */
	public String stringDesc() {
		
		String description =  ID +","+parentID+","+birthDate+","+life+","
				+ speed+","+maxEnergy+","+ getKidEnergy()+","
				+ hasSensors() +","+ getAncestor() + "," + getNKids() + ","
				+ death + ","+ matForKids + ","+ luminosity + ","+ warm + ","+ loud +","+ smelly + ","+ electric + "," + eaten + "\n";
		return description;
	}
	
	public int[] getProperties() {
		return properties;
	}

	public void setPosition(double[] position2) {
		position[0] = position2[0];
		position[1] = position2[1];
	}

	public double getWarm() {
		return warm;
	}

	public double getLoud() {
		return loud;
	}

	public double getSmelly() {
		return smelly;
	}

	public double getElectric() {
		return electric;
	}
	
	
	public int getDeath() {
		return death;
	}

	public void setDeath(int death) {
		this.death = death;
	}

	public Tree getSensors() {
		return sensors;
	}

	public void setSensors(Tree sensors) {
		this.sensors = sensors;
	}

	public double getEnergy() {
		return energy;
	}

	public void setEnergy(double energy) {
		if(energy>maxEnergy){
			energy = maxEnergy;
		}
		this.energy = energy;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}
	
	public void setEaten(int eaten) {
		this.eaten = eaten;
	}


	public void setCellTransparency(double cellTransparency) {
		this.cellTransparency = cellTransparency;
	}

}
