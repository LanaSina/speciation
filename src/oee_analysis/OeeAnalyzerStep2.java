package oee_analysis;

import communication.RealMap;
import communication.ShadowMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class for doing step 2 of the Channon procedure.
 */
public class OeeAnalyzerStep2 extends OeeAnalyzer {

    /** The real map. */
    private final RealMap realMap;
    /** The shadow map. */
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

    /** Builds an analyzer for doing the step 2 of Channon's procedure. */
    public OeeAnalyzerStep2(RealMap realMap, ShadowMap shadowMap, String dataFolderName) {
        super(dataFolderName, "Step2Stats");
        // initialize the file
        String str = "t, AN_cum, AN_cum_median" + "\n";
        try {
            summaryWriter.append(str);
        } catch (IOException e) {
            throw new RuntimeException();
        }
        // initialize the maps
        this.realMap = realMap;
        this.shadowMap = shadowMap;
        // initialize the statistics
        this.deltas_R = initHashMap();
        this.accumulations_R = initHashMap();
        this.cumulativeActivities_R = initHashMap();
        this.deltas_S = initHashMap();
        this.deltas_N = initHashMap();
        this.accumulations_N = initHashMap();
        this.cumulativeActivities_N = initHashMap();
    }

    @Override
    public void update() {
        // compute adaptive total cumulative evolutionary activity
        updateDeltas(deltas_R, realMap);
        updateDeltas(deltas_S, shadowMap);
        updateNormalizedDeltas(deltas_N, deltas_R, deltas_S);
        updateAccumulations(accumulations_N, deltas_N);
        updateCumulativeActivities(cumulativeActivities_N, accumulations_N, deltas_N);
        totalCumulativeActivity_N = computeTotalCumulativeActivity(accumulations_N);
        // compute adaptive median cumulative evolutionary activity
        updateAccumulations(accumulations_R, deltas_R);
        updateCumulativeActivities(cumulativeActivities_R, accumulations_R, deltas_R);
        diversity_R = computeDiversity(cumulativeActivities_R);
        String medianCumulativeActivity_N_str;
        try {
            medianCumulativeActivity_N = totalCumulativeActivity_N / diversity_R;
            medianCumulativeActivity_N_str = Integer.toString(medianCumulativeActivity_N);
        } catch (ArithmeticException e) {
            medianCumulativeActivity_N_str = "N/A";
        }
        try {
            String line = realMap.getTime() + "," + totalCumulativeActivity_N + "," + medianCumulativeActivity_N_str + "\n";
            summaryWriter.append(line);
            summaryWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }


    /**
     * Computes the component diversity, i.e. the number of components present.
     *
     * @return the component diversity
     */
    protected static int computeDiversity(HashMap<String, HashMap<Integer, Integer>> cumulativeActivities) {
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
     * Updates the normalized deltas.
     *
     * @param deltas_N the normalized deltas to update
     * @param deltas_R the deltas of the real model
     * @param deltas_S the deltas of the shadow model
     */
    protected static void updateNormalizedDeltas(HashMap<String, HashMap<Integer, Integer>> deltas_N, HashMap<String, HashMap<Integer, Integer>> deltas_R, HashMap<String, HashMap<Integer, Integer>>deltas_S) {
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

}
