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

	/** Controls whether the RealMap is displayed. (NB: the ShadowMap is never displayed.) */
	public static final boolean ENABLE_DISPLAY = false;
	/** percentage of elements drawn*/
	public static double draw_coarse = 1;
	
	/** equivalent to refresh rate */
	public static int refresh_rate = 20;//500

	/** grid limits*/
	public static final int GridMax = 50;
	public static final int GridStep = 20;
	
	public static final int ActionTypes = 10;// todo reduce to 2

	/** folder where data file will be recorded*/
	public static String DataPath = "../new_data";
	/** files*/
	public static final String SummaryFileName = "SummaryIndividuals";
	public static boolean Save = true;
	public static String SnapshotFileName = "snapshot";
	public static String SensorsFileName = "sensors";
	public static String PredationFileName = "predation";

	/** Number of threads to use in RealMap.updateMoved() */
	public static final int NB_THREADS = 16;

	/** OEE analysis */
	// Run shadow model in parallel to the real model
	public static final boolean RunShadowModel = true;
	public static final String ShadowModelSummaryFileName = "ShadowModel_SummaryIndividuals";
	public static final String ShadowModelPredationFileName = "ShadowModel_Predation";
	public static String ShadowModelSnapshotFileName = "ShadowModel_Snapshot";
	public static String ShadowModelSensorsFileName = "ShadowModel_Sensors";

	/**
     * from <a href="http://stackoverflow.com/questions/363681/generating-random-integers-in-a-range-with-java">this page</a>
     * and <a href="http://stackoverflow.com/questions/3680637/how-to-generate-a-random-double-in-a-given-range">this page</a>
     * Returns a pseudo-random number between min and max, exclusive.
     * Uniform distribution.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, exclusive.
     * @see Random#nextInt(int)
     */
	public static double uniformDouble(double min, double max) {
	    return min + (max - min) * rand.nextDouble();
	}
	
	/**
	 * Uniform distribution between 0 and 1
	 * @return random number
	 */
	public static double uniformDouble() {
	    return rand.nextDouble();
	}
}

