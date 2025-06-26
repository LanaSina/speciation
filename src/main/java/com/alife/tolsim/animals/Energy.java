package com.alife.tolsim.animals;


public class Energy {
	public int type = -1;
	//default distance decay constant
	int dissip_dist=0;
	
	/**
	 * 
	 * @param t type
	 * @param dist distance decay constant
	 */
	public Energy(int t, int dist){
		type = t;
		dissip_dist = dist;
	}
	
	/**
	 * 
	 * @param distanceRatio a percentage (0..infinite)
	 * @param curve ratio aussi??
	 * @return
	 */
	public double[] dissipation(double distanceRatio, double curve){
		double[] cells = new double[dissip_dist];
		//
		return cells;
	}
	
	public double[] decay(double[] cells){
		
		return cells;
	}
}
