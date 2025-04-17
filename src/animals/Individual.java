package animals;

import java.awt.Color;
import java.util.LinkedList;

import visualization.GraphicalComponent;

public interface Individual {

	public double[] position = null;

	public boolean isLight();
	public double getSpeed();
	public Tree getSensors();
	public double getEnergy();
	public void setEnergy(double energy);
	public boolean parentIsLight();
	public String stringDesc();
	public void setID(int globalID);
	public int[] getProperties();
	public void setEatenBy(int i);
	public int getID();
}
