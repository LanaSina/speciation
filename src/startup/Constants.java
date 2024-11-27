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
	public static Random rand = new Random(5);

	public static boolean draw = true;
	/** percentage of elements drawn*/
	public static double draw_coarse = 1;
	
	/** equivalent to refresh rate */
	public static int refresh_rate = 20;//500


	
	/** grid limits*/
	public static final int GridMax = 50;// 10+40+10;//20+20+10+(security)10 
	public static final int GridStep = 20;
	
	/** actions*/
	public static final int ActEat = 0;
	public static final int ActMate = 1;
	
	public static final int MoveUp = 2;
	public static final int MoveDown = 3;
	public static final int MoveRigh = 4;
	public static final int MoveLeft = 5;
	
	public static final int MoreTransparency = 6;
	public static final int LessTransparency = 7;
	public static final int MoreDensity = 8;
	public static final int LessDensity = 9;
	
	public static final int ActionTypes = 10;



	
	// factors on property values
	/** mutation factor*/
	public static final double MutFactor = 0.01;
	/** speed*/
	public static final double SpeedFactor = 0.02;//0.02
	/** absolute max value*/
	public static final int SpeedMax= 100;
	/** absolute max value*/
	public static final int EnergyMax = 1000;
	/** coarse graining of property values*/
	public static final int PropGrain = 100;

	public static final double SpeedCost= 0.02;//0.2
	/** cost of being alive */
	public static final double StepCost = 0.1;//0.002
	/** cost of having sensors*/
	public static final double SensorCost = 0.2;//0.2;
	/**cost of making sensory errors, ratio of prey energy*/
	public static final double ErrorCost = 0.02;
	/** energy iput into system */
	public static final double FreeEnergy =3;//5
	
	/** how far the kids are from the parents*/
	public static final double BirthDistance = 1.5;
	/** how far the kids are from the parents*/
	public static final double LightBirthDistance = 1.5;

	
	public static final int EnergyTypes = 2;
	
	/** folder where data file will be recorded*/
	public static String DataPath = "../new_data";
	public static double ActionCost = 0.5;
	/** files*/
	public static final String SummaryFileName = "SummaryIndividuals";
	public static boolean Save = true;
	public static boolean SavePredation = true; // header and data do not match


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
	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    double randomNum = min + (max - min) * rand.nextDouble();
	    //rand.nextInt(max - min) + 1)

	    return randomNum;
	}
}

