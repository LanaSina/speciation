/**
 * 
 */
package com.alife.tolsim.startup;

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
	/** File where the population (i.e. number of individuals) of the real model will be recorded. */
	public static String PopulationFilename = "Population";
	public static final String ShadowModelSummaryFileName = "ShadowModel_SummaryIndividuals";
	public static String ShadowModelSnapshotFileName = "ShadowModel_Snapshot";
	public static String ShadowModelSensorsFileName = "ShadowModel_Sensors";
	public static String ShadowMapDeltasFileName = "ShadowMapDeltas";
	// NB: there is no predation recording for the shadow model because there is no predation in it, only random deaths

	/** Controls whether to save the data about individuals and predation. */
	public static boolean Save = false;
    /** Percentage of individuals/predations saved. */
    public static double SaveCoarse = 0.01;
    /** This field is the number of time steps during which to save information about dead individuals and predation before switching to news files. */
    public static int SaveEvery = 5000;

	/** This field is the number of time steps between two backups of the alive individuals and of the sensors.
	 * Backups allow to stop the program then later resume it at the same point. */
	public static int BackupEvery = 100000;

	/** Controls whether the RealMap is displayed. (NB: the ShadowMap is never displayed.) */
	public static final boolean EnableDisplay = false;
	/** Percentage of elements drawn. */
	public static double DrawCoarse = 1;
	/** Number of time steps between two image repaints. */
	public static int RefreshImageEvery = 1;

	/** Controls whether to run the shadow model in parallel to the real model. */
	public static final boolean RunShadowModel = true;

	// Grid limits
	public static final int GridMax = 50;
	public static final int GridStep = 20;

	public static final int ActionTypes = 10;// todo reduce to 2

	/** Number of threads to use in <code>RealMap.applyChanges()</code>. */
	public static final int NbThreads = 16;

}

