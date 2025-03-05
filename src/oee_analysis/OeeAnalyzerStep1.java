package oee_analysis;

import animals.IndividualWithProperties;
import communication.FileBuilder;
import communication.Map;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A class for analyzing the open-endedness of the simulator.
 */
public class OeeAnalyzerStep1 {

    private final Map map;

    /** The delta_i's. */
    public HashMap<String, HashMap<Integer, Integer>> deltas;
    /** Accumulations of delta_i's since t=0. Intermediate values to calculate the a_i. */
    public HashMap<String, HashMap<Integer, Integer>> accumulations;
    /** The a_i's. */
    public HashMap<String, HashMap<Integer, Integer>> cumulativeActivities;
    /** A_cum */
    public int totalCumulativeActivity;

    private final FileWriter summaryWriter;

    public OeeAnalyzerStep1(Map map, String dataFolderName) {
        this.map = map;
        // initialize the statistics
        this.deltas = new HashMap<>();
        this.accumulations = new HashMap<>();
        this.cumulativeActivities = new HashMap<>();
        ArrayList<HashMap<String, HashMap<Integer, Integer>>> list = new ArrayList<>();
        list.add(this.deltas);
        list.add(this.accumulations);
        list.add(this.cumulativeActivities);
        for (HashMap<String, HashMap<Integer, Integer>> thing : list) {
            thing.put("speed", new HashMap<>());
            thing.put("maxEnergy", new HashMap<>());
            thing.put("kidEnergy", new HashMap<>());
            thing.put("nKids", new HashMap<>());
            thing.put("death", new HashMap<>());
            thing.put("matForKids", new HashMap<>());
        }
        // initialize the file
        FileBuilder fb = new FileBuilder(dataFolderName, "TotalCumulativeEvolutionaryActivity");
        summaryWriter = fb.getFileWriter();
        String str = "t, A_cum" + "\n";
        try {
            summaryWriter.append(str);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public void update() {
        updateDeltas();
        updateAccumulations();
        updateActivities();
        updateTotalActivity();
        try {
            String line = map.getTime() + "," + totalCumulativeActivity + "\n";
            summaryWriter.append(line);
            summaryWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    /**
     * Computes delta_i.
     */
    private void updateDeltas() {
        // reset the deltas
        for (String property : deltas.keySet())
            deltas.get(property).replaceAll((k, v) -> 0);
        // set the deltas
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
     * Computes the accumulations of delta_i since t=0.
     * <br>
     */
    private void updateAccumulations() {
        for (String property : deltas.keySet()) {
            for (int key : deltas.get(property).keySet()) {
                if (!accumulations.get(property).containsKey(key))
                    accumulations.get(property).put(key, 0);
                accumulations.get(property).compute(key, (k, v) -> v + deltas.get(property).get(key));
            }
        }
    }

    /**
     * Computes a_i.
     */
    private void updateActivities() {
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
     * Computes A_cum.
     */
    private void updateTotalActivity() {
        totalCumulativeActivity = 0;
        for (String property : cumulativeActivities.keySet())
            totalCumulativeActivity += cumulativeActivities.get(property).values().stream().reduce(0, Integer::sum);
    }

}
