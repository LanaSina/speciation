package communication;

import animals.IndividualWithProperties;
import startup.Constants;

import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.abs;

public abstract class Map {
	MyLog mlog = createMyLog();



	/** global var: id & number of animals until now*/
	final AtomicInteger globalID = new AtomicInteger(0);
	/** data recording*/
	FileWriter summaryWriter;
	FileWriter predationWriter;
	String summaryFileName;
	String predationFileName;
	String snapshotFileName;
	String sensorsFileName;
	/** simulation time*/
	int time = 0;
	String dataFolderName;

	// global contstants
	double cst_mut_factor;
	double cst_speed_factor;
	int cst_speed_max;
	double cst_light_birth_dst;
	double cst_birth_dst;
	int cst_energy_max;
	double cst_speed_cost;
	double cst_sensor_cost;
	double cst_error_cost;
	int cst_free_energy;
	int cst_sensor_precision;
	int cst_max_number_actions;
	double cst_energy_cost_factor;
	double cst_step_cost;



	public Map(String myDataFolderName,
			   String summaryFileName,
			   String predationFileName,
			   String snapshotFileName,
			   String sensorsFileName) {
		this.dataFolderName = myDataFolderName;
		this.summaryFileName = summaryFileName;
		this.predationFileName = predationFileName;
		this.snapshotFileName = snapshotFileName;
		this.sensorsFileName = sensorsFileName;
	}

	public int incrementAndGetGlobalID(){
		return globalID.incrementAndGet();
	}

	public void setGlobalId(int i) {
		globalID.set(i);
	}

	public int getTime() {
		return time;
	}

	public void setTime(int t) {
		time = t;
	}
	
	/**
	 * shifts a number so values closest to max are 1
	 * @param m between 0..1
	 * @return
	 */
	protected double shiftMax(double val, double m) {
		double s = 1- abs(m-val);//triangular
		s = checkZero(s, m-0.2, m+0.2);
		return s;
	}
	
	//sets at 0 if out of bounds
	protected double checkZero(double val, double low, double high) {
		if(val<low) val = 0;
		if(val>high) val = 0;
		return val;
	}
	
	//set at bounds
	protected double check(double val, double low, double high) {
		if(val<low) val = low;
		if(val>high) val = high;
		return val;
	}
	
	protected boolean generateBool(){
		boolean b = false;
		if(Constants.uniformDouble()>0.5){
			b = true;
		}
		
		return b;
	}

	/**
	 * Returns all individuals of this map that are non-light and whose parent is a non-light.
	 *
	 * @return all individuals of this map that are non-light and whose parent is a non-light
	 */
	public abstract  List<IndividualWithProperties> getAllEvolvedIndividuals();

	protected abstract MyLog createMyLog();

}
