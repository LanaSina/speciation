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
	public double getLuminosity();
	public int[] getProperties();
	public void setPosition(double[] position2);

	public double getWarm();
	public double getLoud();
	public double getSmelly();
	public double getElectric();
	public void setEatenBy(int i);
	public boolean update(LinkedList<Individual> babies, int time, double transparency);
	public void setCellTransparency(double transparency);
}
