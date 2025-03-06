package oee_analysis;

import animals.IndividualWithProperties;
import communication.FileBuilder;
import communication.Map;

import java.io.FileWriter;
import java.util.HashMap;

/** A class regrouping common code between OeeAnalyzerStep1 and OeeAnalyzerStep2. */
public abstract class OeeAnalyzer {

    /** The writer used to write in the file. */
    protected final FileWriter summaryWriter;

    /**
     * Builds an OEE analyzer.
     *
     * @param dataFolderName the name of the folder within which the simulation data is registered
     * @param fileName the name of the analysis file that will be generated within the data folder
     */
    public OeeAnalyzer(String dataFolderName, String fileName) {
        // initialize the file
        FileBuilder fb = new FileBuilder(dataFolderName, fileName);
        summaryWriter = fb.getFileWriter();
    }

    /**
     * Creates a hash map used to store the statistics.
     *
     * @return the created hash map
     */
    protected static HashMap<String, HashMap<Integer, Integer>> initHashMap() {
        HashMap<String, HashMap<Integer, Integer>> res = new HashMap<>();
        res.put("speed", new HashMap<>());
        res.put("maxEnergy", new HashMap<>());
        res.put("kidEnergy", new HashMap<>());
        res.put("nKids", new HashMap<>());
        res.put("death", new HashMap<>());
        res.put("matForKids", new HashMap<>());
        return res;
    }

    /**
     * Computes statistics at the current time step and write them in the analysis file.
     */
    public abstract void update();


    /**
     * Updates deltas for the given map.
     *
     * @param deltas the deltas to update
     * @param map the associated map
     */
    protected static void updateDeltas(HashMap<String, HashMap<Integer, Integer>> deltas, Map map) {
        // reset all deltas to 0
        for (String property : deltas.keySet())
            deltas.get(property).replaceAll((k, v) -> 0);
        // set to 1 deltas associated to components that are present
        for (IndividualWithProperties individual : map.getAllEvolvedIndividuals()) {
            deltas.get("speed").put(individual.speed, 1);
            deltas.get("maxEnergy").put(individual.getMaxEnergy(), 1);
            deltas.get("kidEnergy").put(individual.getKidEnergy(), 1);
            deltas.get("nKids").put(individual.getNKids(), 1);
            deltas.get("death").put(individual.death, 1);
            deltas.get("matForKids").put(individual.getMatForKids(), 1);
        }
    }

    /**
     * Updates the accumulations of activity of the given deltas since t=0.
     *
     * @param accumulations the accumulations of deltas to update
     * @param deltas the new deltas
     */
    protected static void updateAccumulations(HashMap<String, HashMap<Integer, Integer>> accumulations, HashMap<String, HashMap<Integer, Integer>> deltas) {
        for (String property : deltas.keySet()) {
            for (int key : deltas.get(property).keySet()) {
                if (!accumulations.get(property).containsKey(key))
                    accumulations.get(property).put(key, 0);
                accumulations.get(property).compute(key, (k, v) -> v + deltas.get(property).get(key));
            }
        }
    }

    /**
     * Updates the cumulative evolutionary activities.
     *
     * @param cumulativeActivities the cumulative evolutionary activities to update
     * @param accumulations the accumulations of deltas
     * @param deltas the deltas
     */
    protected static void updateCumulativeActivities(HashMap<String, HashMap<Integer, Integer>> cumulativeActivities, HashMap<String, HashMap<Integer, Integer>> accumulations, HashMap<String, HashMap<Integer, Integer>> deltas) {
        for (String property : deltas.keySet()) {
            for (int key : deltas.get(property).keySet()) {
                if (deltas.get(property).get(key) == 0)
                    cumulativeActivities.get(property).put(key, 0);
                else
                    cumulativeActivities.get(property).put(key, accumulations.get(property).get(key));
            }
        }
    }

    /**
     * Computes the total cumulative evolutionary activity.
     *
     * @param cumulativeActivities the cumulative evolutionary activities
     * @return the total cumulative evolutionary activity
     */
    protected static int computeTotalCumulativeActivity(HashMap<String, HashMap<Integer, Integer>> cumulativeActivities) {
        int totalCumulativeActivity = 0;
        for (String property : cumulativeActivities.keySet())
            totalCumulativeActivity += cumulativeActivities.get(property).values().stream().reduce(0, Integer::sum);
        return totalCumulativeActivity;
    }

}
