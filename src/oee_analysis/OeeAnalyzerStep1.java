package oee_analysis;

import communication.RealMap;

import java.io.IOException;
import java.util.HashMap;

/**
 * A class for doing step 1 of the Channon procedure.
 */
public class OeeAnalyzerStep1 extends OeeAnalyzer {

    /** The map to analyze. */
    private final RealMap map;

    /** The deltas of the real model. */
    public HashMap<String, HashMap<Integer, Integer>> deltas;
    /** Accumulations of deltas since t=0. Intermediate values to calculate the cumulative evolutionary activities. */
    public HashMap<String, HashMap<Integer, Integer>> accumulations;
    /** The cumulative evolutionary activities (a_i's). */
    public HashMap<String, HashMap<Integer, Integer>> cumulativeActivities;
    /** The total cumulative evolutionary activity (A_cum). */
    public int totalCumulativeActivity;

    /**
     * Builds an analyzer for doing the step 1 of Channon's procedure.
     *
     * @param map the map to analyze
     * @param dataFolderName the name of the folder within which the simulation data is registered
     */
    public OeeAnalyzerStep1(RealMap map, String dataFolderName) {
        super(dataFolderName, "Step1Stats");
        // write the file's header
        String str = "t, A_cum" + "\n";
        try {
            summaryWriter.append(str);
        } catch (IOException e) {
            throw new RuntimeException();
        }
        // initialize the map
        this.map = map;
        // initialize the statistics
        this.deltas = initHashMap();
        this.accumulations = initHashMap();
        this.cumulativeActivities = initHashMap();
    }

    @Override
    public void update() {
        updateDeltas(deltas, map);
        updateAccumulations(accumulations, deltas);
        updateCumulativeActivities(cumulativeActivities, accumulations, deltas);
        totalCumulativeActivity = computeTotalCumulativeActivity(cumulativeActivities);
        try {
            String line = map.getTime() + "," + totalCumulativeActivity + "\n";
            summaryWriter.append(line);
            summaryWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

}
