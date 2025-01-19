package animals;

import java.awt.Color;
import java.util.LinkedList;

import visualization.GraphicalComponent;

public interface Individual extends GraphicalComponent {

	public double[] position = null;

	public boolean isLight();
	public double[] getPosition();
	public double getSpeed();
	public Tree getSensors();
	public double getEnergy();
	public void setEnergy(double energy);
	public void setBorderColor(Color color);
	public boolean parentIsLight();
	public String stringDesc();
	public void setID(int globalID);
	public int[] getProperties();
	public void setPosition(double[] position2);
	public void setEatenBy(int i);
	public void setCellTransparency(double transparency);
	public int getID();
}
