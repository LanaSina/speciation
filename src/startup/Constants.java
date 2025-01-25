/**
 * 
 */
package startup;

import java.util.List;
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

	/**
	 * Equivalent of <code>.indexOf()</code> but uses <code>==</code> comparison instead of <code>.equals()</code>.
	 *
	 * @param list   the list to scan
	 * @param target the item to look for
	 * @param <T>    the type of elements held in the list
	 * @return the index of the item in the list, or -1 if the item is not in the list
	 */
	public static <T> int indexOfByReference(List<T> list, T target) {
		if (list == null)
			return -1;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == target)
				return i;
		}
		return -1;
	}

	/**
	 * Equivalent of <code>.contains()</code> but uses <code>==</code> comparison instead of <code>.equals()</code>.
	 *
	 * @param list   the list to scan
	 * @param target the item to look for
	 * @param <T>    the type of elements held in the list
	 * @return the index of the item in the list, or -1 if the item is not in the list
	 */
	public static <T> boolean containsByReference(List<T> list, T target) {
		if (list == null)
			return false;
		for (T element : list) {
			if (element == target)
				return true;
		}
		return false;
	}
}

