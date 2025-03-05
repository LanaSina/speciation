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

    /** The deltas of the real model. */
    public HashMap<String, HashMap<Integer, Integer>> deltas_R;
    /** Accumulations of delta_R's since t=0. Intermediate values to calculate the cumulative evolutionary activities of the real model. */
    public HashMap<String, HashMap<Integer, Integer>> accumulations_R;
    /** The cumulative evolutionary activities (a_i's) of the real model. */
    public HashMap<String, HashMap<Integer, Integer>> cumulativeActivities_R;
    /** The deltas of the shadow model. */
    public HashMap<String, HashMap<Integer, Integer>> deltas_S;
    /** The normalized deltas. */
    public HashMap<String, HashMap<Integer, Integer>> deltas_N;
    /** Accumulations of delta_N's since t=0. Intermediate values to calculate the normalized cumulative evolutionary activities. */
    public HashMap<String, HashMap<Integer, Integer>> accumulations_N;
    /** The normalized cumulative evolutionary activities. */
    public HashMap<String, HashMap<Integer, Integer>> cumulativeActivities_N;
    /** The adaptive total cumulative evolutionary activity (A^N_cum). */
    public int totalCumulativeActivity_N;
    /** Component diversity in the real model (D^R), or <quote>the number of components present, in use in the real run</quote>. */
    private int diversity_R;
    /** The adaptive median cumulative evolutionary activity. */
    public int medianCumulativeActivity_N;

    private final FileWriter summaryWriter;

    public OeeAnalyzerStep2(Map map, ShadowMap shadowMap, String dataFolderName) {
        this.map = map;
        this.shadowMap = shadowMap;
        // initialize the statistics
        this.deltas_R = new HashMap<>();
        this.accumulations_R = new HashMap<>();
        this.cumulativeActivities_R = new HashMap<>();
        this.deltas_S = new HashMap<>();
        this.deltas_N = new HashMap<>();
        this.accumulations_N = new HashMap<>();
        this.cumulativeActivities_N = new HashMap<>();
        ArrayList<HashMap<String, HashMap<Integer, Integer>>> list = new ArrayList<>();
        list.add(this.deltas_R);
        list.add(this.accumulations_R);
        list.add(this.cumulativeActivities_R);
        list.add(this.deltas_S);
        list.add(this.deltas_N);
        list.add(this.accumulations_N);
        list.add(this.cumulativeActivities_N);
        for (HashMap<String, HashMap<Integer, Integer>> thing : list) {
            thing.put("speed", new HashMap<>());
            thing.put("maxEnergy", new HashMap<>());
            thing.put("kidEnergy", new HashMap<>());
            thing.put("nKids", new HashMap<>());
            thing.put("death", new HashMap<>());
            thing.put("matForKids", new HashMap<>());
        }
        // initialize the file
        FileBuilder fb = new FileBuilder(dataFolderName, "Step2Stats");
        summaryWriter = fb.getFileWriter();
        String str = "t, AN_cum, AN_cum_median" + "\n";
        try {
            summaryWriter.append(str);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public void update() {
        // compute adaptive total cumulative activity
        updateDeltas(deltas_R, map);
        updateDeltas(deltas_S, shadowMap);
        updateNormalizedDeltas();
        updateAccumulations(accumulations_N, deltas_N);
        updateCumulativeActivities(cumulativeActivities_N, accumulations_N, deltas_N);
        totalCumulativeActivity_N = computeTotalCumulativeActivity(accumulations_N);
        // compute adaptive median cumulative activity
        updateAccumulations(accumulations_R, deltas_R);
        updateCumulativeActivities(cumulativeActivities_R, accumulations_R, deltas_R);
        diversity_R = computeDiversity(cumulativeActivities_R);
        String medianCumulativeActivity_N_str;
        try {
            medianCumulativeActivity_N = totalCumulativeActivity_N / diversity_R;
            medianCumulativeActivity_N_str = Integer.toString(medianCumulativeActivity_N);
        } catch(ArithmeticException e) {
            medianCumulativeActivity_N_str = "N/A";
        }
        try {
            String line = map.getTime() + "," + totalCumulativeActivity_N + "," + medianCumulativeActivity_N_str + "\n";
            summaryWriter.append(line);
            summaryWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private int computeDiversity(HashMap<String, HashMap<Integer, Integer>> cumulativeActivities) {
        int diversity = 0;
        for (String property : cumulativeActivities.keySet()) {
            for (int key : cumulativeActivities.get(property).keySet()) {
                if (cumulativeActivities.get(property).get(key) > 0)
                    diversity++;
            }
        }
        return diversity;
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
        for (String property : deltas_N.keySet()) {
            Set<Integer> unionOfKeys = Stream.concat(deltas_R.get(property).keySet().stream(), deltas_S.get(property).keySet().stream())
                                       .collect(Collectors.toSet());
            for (int key : unionOfKeys) {
                if (!deltas_R.get(property).containsKey(key))
                    deltas_R.get(property).put(key, 0);
                else if (!deltas_S.get(property).containsKey(key))
                    deltas_S.get(property).put(key, 0);
                deltas_N.get(property).put(key, deltas_R.get(property).get(key) - deltas_S.get(property).get(key));
            }
        }
    }

    /**
     * Computes the accumulations of the given deltas since t=0.
     * <br>
     */
    private void updateAccumulations(HashMap<String, HashMap<Integer, Integer>> accumulations, HashMap<String, HashMap<Integer, Integer>> deltas) {
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
    private void updateCumulativeActivities(HashMap<String, HashMap<Integer, Integer>> cumulativeActivities, HashMap<String, HashMap<Integer, Integer>> accumulations, HashMap<String, HashMap<Integer, Integer>> deltas) {
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
    private int computeTotalCumulativeActivity(HashMap<String, HashMap<Integer, Integer>> cumulativeActivities) {
        int totalCumulativeActivity = 0;
        for (String property : cumulativeActivities.keySet())
            totalCumulativeActivity += cumulativeActivities.get(property).values().stream().reduce(0, Integer::sum);
        return totalCumulativeActivity;
    }

}
