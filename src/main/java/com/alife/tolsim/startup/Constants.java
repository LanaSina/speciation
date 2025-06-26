/**
 * 
 */
package com.alife.tolsim.startup;

import java.util.Random;

/**
 * @author lana
 * Class containing all the constants variable and generic functions.
 */
public class Constants {

	/** Folder where data will be recorded. */
	public static String DataPath = "../new_data";
	public static final String SummaryFileName = "SummaryIndividuals";
	public static String SnapshotFileName = "snapshot";
	public static String SensorsFileName = "sensors";
	public static String PredationFileName = "predation";
	public static String RealMapDeltasFileName = "RealMapDeltas";
	public static final String ShadowModelSummaryFileName = "ShadowModel_SummaryIndividuals";
	public static String ShadowModelSnapshotFileName = "ShadowModel_Snapshot";
	public static String ShadowModelSensorsFileName = "ShadowModel_Sensors";
	public static String ShadowMapDeltasFileName = "ShadowMapDeltas";
	// NB: there is no predation recording for the shadow model because there is no predation in it, only random deaths

	/** Controls whether to save the data about individuals and predation. */
	public static boolean Save = false;

	/** Controls whether the RealMap is displayed. (NB: the ShadowMap is never displayed.) */
	public static final boolean EnableDisplay = false;
	/** Percentage of elements drawn. */
	public static double DrawCoarse = 1;
	/** Equivalent to refresh rate. */
	public static int RefreshRate = 20;//500

	/** Controls whether to run the shadow model in parallel to the real model. */
	public static final boolean RunShadowModel = true;

	// Grid limits
	public static final int GridMax = 50;
	public static final int GridStep = 20;

	public static final int ActionTypes = 10;// todo reduce to 2

	public static Random rand = new Random(5);

	/** Number of threads to use in <code>RealMap.applyChanges()</code>. */
	public static final int NbThreads = 16;


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

