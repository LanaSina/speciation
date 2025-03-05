package oee_analysis;

import animals.IndividualWithProperties;
import communication.FileBuilder;
import communication.Map;
import communication.ShadowMap;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class for analyzing the open-endedness of the simulator.
 */
public class OeeAnalyzerStep2 {

    private final Map map;
    private final ShadowMap shadowMap;

    /** The delta_i's of the real model. */
    public HashMap<String, HashMap<Integer, Integer>> realDeltas;
    /** The delta_i's of the shadow model. */
    public HashMap<String, HashMap<Integer, Integer>> shadowDeltas;
    /** The normalized delta_i's. */
    public HashMap<String, HashMap<Integer, Integer>> normalizedDeltas;
    /** Accumulations of delta_i's since t=0. Intermediate values to calculate the a_i. */
    public HashMap<String, HashMap<Integer, Integer>> accumulations;
    /** The a_i's. */
    public HashMap<String, HashMap<Integer, Integer>> cumulativeActivities;
    /** A_cum */
    public int totalCumulativeActivity;

    private final FileWriter summaryWriter;

    public OeeAnalyzerStep2(Map map, ShadowMap shadowMap, String dataFolderName) {
        this.map = map;
        this.shadowMap = shadowMap;
        // initialize the statistics
        this.realDeltas = new HashMap<>();
        this.shadowDeltas = new HashMap<>();
        this.normalizedDeltas = new HashMap<>();
        this.accumulations = new HashMap<>();
        this.cumulativeActivities = new HashMap<>();
        ArrayList<HashMap<String, HashMap<Integer, Integer>>> list = new ArrayList<>();
        list.add(this.realDeltas);
        list.add(this.shadowDeltas);
        list.add(this.normalizedDeltas);
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
        FileBuilder fb = new FileBuilder(dataFolderName, "NormalizedTotalCumulativeEvolutionaryActivity");
        summaryWriter = fb.getFileWriter();
        String str = "t, AN_cum" + "\n";
        try {
            summaryWriter.append(str);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public void update() {
        updateDeltas(realDeltas, map);
        updateDeltas(shadowDeltas, shadowMap);
        updateNormalizedDeltas();
        updateAccumulations(normalizedDeltas);
        updateActivities(normalizedDeltas);
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
     * Computes delta_i for the given map.
     */
    private void updateDeltas(HashMap<String, HashMap<Integer, Integer>> deltas, Map map) {
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
     * Computes the normalized deltas.
     */
    private void updateNormalizedDeltas() {
        for (String property : normalizedDeltas.keySet()) {
            Set<Integer> unionOfKeys = Stream.concat(realDeltas.get(property).keySet().stream(), shadowDeltas.get(property).keySet().stream())
                                       .collect(Collectors.toSet());
            for (int key : unionOfKeys) {
                if (!realDeltas.get(property).containsKey(key))
                    realDeltas.get(property).put(key, 0);
                else if (!shadowDeltas.get(property).containsKey(key))
                    shadowDeltas.get(property).put(key, 0);
                normalizedDeltas.get(property).put(key, realDeltas.get(property).get(key) - shadowDeltas.get(property).get(key));
            }
        }
    }

    /**
     * Computes the accumulations of delta_i since t=0.
     * <br>
     */
    private void updateAccumulations(HashMap<String, HashMap<Integer, Integer>> deltas) {
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
    private void updateActivities(HashMap<String, HashMap<Integer, Integer>> deltas) {
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
