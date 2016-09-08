/**
 * 
 */
package startup;

import java.util.Random;

/**
 * @author lana
 * Class containing all the constants variable and generic functions.
 */
public class Constants {
	
	/** grid limits*/
	public static final int GridMax =50;// 10+40+10;//20+20+10+(security)10 
	public static final int GridStep = 20;
	
	/** actions*/
	public static final int ActEat = 0;
	public static final int ActMate = 1;
	
	// factors on property values
	/** speed*/
	public static final double SpeedFactor = 0.2;//
	/** absolute max value*/
	public static final int SpeedMax= 10;
	/** absolute max value*/
	public static final int energyMax = 1000;
	
	public static final double SpeedCost= 0.2;
	/** cost of being alive */
	public static final double StepCost = 0.15;//0.15;//0.3
	/** niche motion */
	public static final double NicheSpeed = 2.0;
	/** cost of having sensors*/
	public static final double SensorCost = 0.5;
	/**cost of making sensory errors, ratio of prey energy*/
	public static final double ErrorCost = 0.2;
	/** energy iput into system */
	public static final double FreeEnergy = 5;//7
	
	/** how far the kids are from the parents*/
	public static final double BirthDistance = 1.7;
	/** how far the kids are from the parents*/
	public static final double LightBirthDistance = 1.7;

	
	public static final int ActionTypes = 2;
	public static final int EnergyTypes = 2;
	
	/** folder where data file will be recorded*/
	public static String DataPath = "/Users/lana/Development/new_OEE_data";
	/** files*/
	public static final String SummaryFileName = "SummaryIndividuals.csv";
	
	
	/**
	 * from http://stackoverflow.com/questions/363681/generating-random-integers-in-a-range-with-java
	 * and http://stackoverflow.com/questions/3680637/how-to-generate-a-random-double-in-a-given-range
	 * Returns a pseudo-random number between min and max, inclusive.
	 * Uniform distribution.
	 * The difference between min and max can be at most
	 * <code>Integer.MAX_VALUE - 1</code>.
	 *
	 * @param min Minimum value
	 * @param max Maximum value.  Must be greater than min.
	 * @return Integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	public static double uniformDouble(double min, double max) {

	    // NOTE: Usually this should be a field rather than a method
	    // variable so that it is not re-seeded every call.
	    Random rand = new Random();

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    double randomNum = min + (max - min) * rand.nextDouble();
	    //rand.nextInt(max - min) + 1)

	    return randomNum;
	}
	
	/**
	 * Uniform distribution between 0 and 1
	 * @return random number
	 */
	public static double uniformDouble() {
		double min = 0;
		double max = 1;
	    // NOTE: Usually this should be a field rather than a method
	    // variable so that it is not re-seeded every call.
	    Random rand = new Random();

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    double randomNum = min + (max - min) * rand.nextDouble();
	    //rand.nextInt(max - min) + 1)

	    return randomNum;
	}
}

