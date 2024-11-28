package animals;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedList;

import startup.Constants;
import visualization.GraphicalComponent;

import communication.MyLog;

import static java.lang.Math.*;

public class EmbodiedIndividual implements GraphicalComponent, Individual{
	MyLog mlog = new MyLog("embodied ind",true);
	
	//cell
	/** 1 = completely transparent*/
	double cellTransparency = 1;
	
	//general
	public int speed = 0;
	//death by being eaten
	int eaten_by = -1;//1 = true;

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
		
		speed = 0;
		maxEnergy = 4;
		energy = 3;
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
	EmbodiedIndividual(EmbodiedIndividual in, int glID, int date, double cst_mut_factor, int cst_speed_max,
					   double cst_light_birth_dst, double birth_dst, int cst_grid_max, int cst_energy_max
	){
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
			position[0] = (in.position[0]+i*cst_light_birth_dst*Constants.uniformDouble());
			position[1] = (in.position[1]+j*cst_light_birth_dst*Constants.uniformDouble());
		} else {
			position[0] = (in.position[0]+i*birth_dst*Constants.uniformDouble());
			position[1] = (in.position[1]+j*birth_dst*Constants.uniformDouble());
		}
		
		for(int k=0;k<2;k++){
			if(position[k]<0) position[k]=0;
			if(position[k]>=cst_grid_max-1) position[k] = cst_grid_max-2;
		}
		
		if(generateBool()){

			double minMut = Constants.uniformDouble(-2, 2);
			
			double plus = Constants.uniformDouble(-cst_mut_factor, cst_mut_factor);
			
			//do this after too
			properties[0] = speed;
			properties[1] = maxEnergy;
			properties[2] = kidEnergy;
			properties[3] = getNKids();
			properties[4] =  death;
			properties[5] = matForKids;

			//maybe make this a mutable value!
			double bias = 0.6;
			if(generateBool(bias)){
				speed = (int)(speed+minMut);
				// todo speed = check()
				if(speed<0) speed = 0;
				if(speed>cst_speed_max) speed = cst_speed_max;
				//break;
			}
			if(generateBool(bias)){
				maxEnergy = (int)(maxEnergy+minMut);
				if(maxEnergy<0){
					maxEnergy = 0;
				}else if (maxEnergy>cst_energy_max) {
					maxEnergy = cst_energy_max;
				}
			}
			if(generateBool(bias)){
				kidEnergy = (int) (kidEnergy+minMut);
				if(kidEnergy<0) kidEnergy = 0;
			}
			if(generateBool(bias)){
				matForKids = (int) (matForKids+minMut);
				if(matForKids<0) matForKids = 0;
			}

			if(generateBool(bias)){
				//create or modify sensor
				if(plus>0){
					int prop = (int) (Constants.uniformDouble(0, nPhysicalProperties-1)+0.5);//-1
					//sensor exists for this property?
					ArrayList<Node> props = sensors.root.getChildren();
					ArrayList<Node> senses = props.get(prop).getChildren();
					//modify
					if(generateBool()){
						//tree nodes: root-> 3properties -> detectionValue -> action
						//          0          id              value            id
						boolean hasSensors = !senses.isEmpty();
						if(hasSensors){
							//get random sensor
							int s = (int) (Constants.uniformDouble(0, senses.size()-1)+0.5);
							Node sensor = senses.get(s);	
							// int value = (int) (sensor.data *Constants.uniformDouble(-Constants.MutFactor, Constants.MutFactor));
							int value = (int) max(0, (sensor.data + Constants.uniformDouble(-3, 3)));
							sensor.data = value;
						}
					} else {
						//create sensor. 
						Node value = new Node();
						
						value.data = (int) Constants.uniformDouble(0, cst_energy_max);
						Node action = new Node();
						action.data = (int) (Constants.uniformDouble(0, Constants.ActionTypes-1)+0.5);
						value.addChild(action);
						
						sensors.root.getChildren().get(prop).addChild(value);
					}							
				}else{
					//tree nodes: root-> 3properties -> detectionValue -> action
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
				int n = (int) (getNKids()*(plus+1)+minMut+0.5);
				if(n<0) n=0;
				setNKids(n);
			}
			if(generateBool(bias)){
				death = (int) (death*(1+plus)+minMut+0.5);
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
		/* 		int red = (int) (loud*255 +0.5);
		int green = (int) (smelly*255 +0.5);
		int blue = (int) (warm*255 +0.5);
		 */
		luminosity = check(death/(double)maxEnergy,0,1);
		warm = check(hasSensors()/(double)(nKids+0.01), 0, 1);
		loud = check(matForKids/(double)(maxEnergy+0.01), 0, 1);
		smelly = check(kidEnergy/(double)(death+0.01), 0, 1);
		electric = check(nKids/(double)(speed+0.01), 0, 1);
	}
	
	private double check(double val, double low, double high) {
		if(val<low) val = low;
		if(val>high) val = high;
		return val;
	}
	
			
	private void makeColor() {
		/*int red = (hasSensors()-(nPhysicalProperties-1))*256/(2*10);
		if(red>255) red = 255; if(red<0) red =0;
		//red = 255-red;
		int green = maxEnergy*255/70;//20
		//green = 255-green;
		if(green>255) green = 255; if(green<0) green =0;
		int blue = kidEnergy*255/10;//13
		if(blue>255) blue = 255; if(green<0) green =0;
		//blue = 255 - blue;*/
		int green = (int) (min(1,(speed*1.0/100))*255 +0.5);
		double d = min(1, (death*1.0/200));
		int blue = (int) (d*255 +0.5);
		d = min(1, (maxEnergy*1.0/200));
		int red = (int) (d*255 +0.5);
		color = new Color(red,green,blue);
	}


	
	public boolean update(LinkedList<Individual> babies, int date, double transparency, double cst_mut_factor, int cst_speed_max,
						  double cst_light_birth_dst, double cst_birth_dst, int cst_grid_max, int cst_energy_max, double cst_speed_cost,
						  double cst_sensor_cost, int cst_free_energy, double cst_energy_cost_factor, double cst_step_cost
	){
		life= life+1;
		cellTransparency = transparency;
		double effect = 0.5;
		
		//remove energy due to sensors
		double se = sensors.root.getChildCount() - (nPhysicalProperties);
		se = se/2;
		//mlog.say("se "+se);
		if(!isLight){
			energy = energy -
					abs(Math.pow(se,1.2)*cst_sensor_cost)//*0.1
					- abs(cst_step_cost);///*maxEnergy);
			if(life >= death){
				energy = -1;
			}
			
			if(energy>(maxEnergy)){
				energy = maxEnergy;
			}
		} else {
			//free energy into light
			energy = energy + cst_free_energy;
		}
		
		if(energy>0 && energy>=matForKids){
			//add children to the map		
			int n = 0;
			if(isLight){
				while(energy-kidEnergy>0){//matForKids
					EmbodiedIndividual baby = new EmbodiedIndividual(this, -1, date, cst_mut_factor, cst_speed_max,
							cst_light_birth_dst, cst_birth_dst, cst_grid_max, cst_energy_max);
					babies.add(baby);
					energy = energy-kidEnergy;
					n++;
				}
			}else{
				while( (n<getNKids())){//
					EmbodiedIndividual baby = new EmbodiedIndividual(this, -1, date, cst_mut_factor, cst_speed_max,
							cst_light_birth_dst, cst_birth_dst, cst_grid_max, cst_energy_max);
					babies.add(baby);
					energy = energy-kidEnergy;//*(1-transparency*effect);
					n++;

					if(energy<kidEnergy){
						energy = -1;
						break;
					}

				}
			}
			borderColor = Color.BLUE;
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
		Graphics2D g2d = (Graphics2D) g;
		int x = (int) (position[0]*gridStep +0.5);
        int y = (int) (position[1]*gridStep +0.5);
        int size = 8;

		if(!parentIsLight){
	        Color c = color; //new Color(color.getRed()/255.0f, color.getGreen()/255.0f, color.getBlue()/255.0f, (float)(cellTransparency));
	        g2d.setColor(c);
	        if(isLight){
	        	g2d.drawRect(x, y, size,size);
	        }else{
	        	g2d.fillRect(x, y, size,size);
	        }      
	        g2d.setColor(borderColor);
	        g2d.drawRect(x, y, size,size);
		}
		if(isLight){
			borderColor = Color.black;
			Color c = new Color(borderColor.getRed()/255.0f, borderColor.getGreen()/255.0f, borderColor.getBlue()/255.0f, (float)(cellTransparency));
	        g2d.setColor(c);
	        g2d.setColor(c);
	        g2d.fillRect(x, y, size,size);
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
	
		n = (sensors.root.getChildCount()-nPhysicalProperties)/2;
		
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
				+ death + ","+ matForKids ;//+ ","+ luminosity + ","+ warm + ","+ loud +","+ smelly + ","+ electric + "," + eaten_by + "\n";
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
	
	public void setEatenBy(int eaten) {
		this.eaten_by = eaten;
	}


	public void setCellTransparency(double cellTransparency) {
		this.cellTransparency = cellTransparency;
	}

}
