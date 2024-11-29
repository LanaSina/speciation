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
	public boolean update(LinkedList<Individual> babies, int date, double transparency, double cst_mut_factor, int cst_speed_max,
						  double cst_light_birth_dst, double cst_birth_dst, int cst_grid_max, int cst_energy_max, double cst_speed_cost,
						  double cst_sensor_cost, int cst_free_energy, double cst_energy_cost_factor, double cst_step_cost
	);
	public void setCellTransparency(double transparency);
	public int getID();
}
