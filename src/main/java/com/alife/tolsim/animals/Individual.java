package com.alife.tolsim.animals;


public interface Individual {

	double[] position = null;

	boolean isLight();
	double getSpeed();
	Tree getSensors();
	double getEnergy();
	void setEnergy(double energy);
	boolean parentIsLight();
	String stringDesc();
	void setID(int globalID);
	int[] getProperties();
	void setEatenBy(int i);
	int getID();
}
