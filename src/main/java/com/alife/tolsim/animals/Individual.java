package com.alife.tolsim.animals;


public interface Individual {

	boolean isLight();
	double getSpeed();
	Tree getSensors();
	double getEnergy();
	void setEnergy(double energy);
	boolean parentIsLight();
	String stringDesc();
	void setID(long globalID);
	int[] getProperties();
	void setEatenBy(long i);
	long getID();
}
