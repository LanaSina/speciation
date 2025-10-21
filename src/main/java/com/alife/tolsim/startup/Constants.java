/**
 *
 */
package com.alife.tolsim.startup;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author lana
 * Class containing all the constants variable and generic functions.
 */
public class Constants {

	/** Folder where data will be recorded. */
	public static String DataPath = "/Users/hyoyeon/Desktop/Career/Sony/simulation";
	public static final String SummaryFileName = "SummaryIndividuals";
	public static String SnapshotFileName = "snapshot";
	public static String SensorsFileName = "sensors";
	public static String PredationFileName = "predation";
	public static String RealMapDeltasFileName = "RealMapDeltas";
	/** File where the population (i.e. number of individuals) of the real model will be recorded. */
	public static String PopulationFilename = "Population";
	public static final String ShadowModelSummaryFileName = "ShadowModel_SummaryIndividuals";
	public static String ShadowModelSnapshotFileName = "ShadowModel_Snapshot";
	public static String ShadowModelSensorsFileName = "ShadowModel_Sensors";
	public static String ShadowMapDeltasFileName = "ShadowMapDeltas";
	// NB: there is no predation recording for the shadow model because there is no predation in it, only random deaths

	/** Controls whether to save the data about individuals and predation. */
	public static boolean Save = ConfigLoader.getBoolean("save", true);
	/** Percentage of individuals/predations saved. */
	public static double SaveCoarse = ConfigLoader.getDouble("save.coarse", 0.01);
	/** This field is the number of time steps during which to save information about dead individuals and predation before switching to news files. */
	public static int SaveEvery = ConfigLoader.getInt("save.every", 5000);

	/** This field is the number of time steps between two backups of the alive individuals and of the sensors.
	 * Backups allow to stop the program then later resume it at the same point. */
	public static int BackupEvery = ConfigLoader.getInt("backup.every", 100000);

	/** Controls whether the RealMap is displayed. (NB: the ShadowMap is never displayed.) */
	public static final boolean EnableDisplay = ConfigLoader.getBoolean("enable.display", true);
	/** Percentage of elements drawn. */
	public static double DrawCoarse = ConfigLoader.getDouble("draw.coarse", 1);
	/** Number of time steps between two image repaints. */
	public static int RefreshImageEvery = ConfigLoader.getInt("refresh.image.every", 1);

	/** Controls whether to run the shadow model in parallel to the real model. */
	public static final boolean RunShadowModel = ConfigLoader.getBoolean("run.shadowmodel", false);

	/** Controls whether shadow model data is saved. */
//	public static final boolean SaveShadowModel = ConfigLoader.getBoolean("save.shadowmodel", false);


	// Grid limits
	public static final int GridMax = ConfigLoader.getInt("grid.max", 50);
	public static final int GridStep = ConfigLoader.getInt("grid.step", 20);

	public static final int ActionTypes = ConfigLoader.getInt("action.types", 10);// todo reduce to 2

	/** Number of threads to use in <code>RealMap.applyChanges()</code>. */
	public static final int NbThreads = ConfigLoader.getInt("nb.threads", 16);

}
