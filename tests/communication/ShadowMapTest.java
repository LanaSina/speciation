package communication;

import animals.EmbodiedIndividual;
import animals.Individual;
import animals.ShadowIndividual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import visualization.Display;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A class for testing the ShadowMap class.
 */
public class ShadowMapTest {


    private static RealMap realMap;
    private static final int MAP_SIZE = 50;
    private static final Display DISPLAY = null;
    private static final String DATA_FOLDER_NAME = "../new_data/some_date/";
    private static final String SUMMARY_FILE_NAME = "SummaryIndividuals";
    private static final String PREDATION_FILE_NAME = "Predation";
    private static final String SNAPSHOT_FILE_NAME = "Snapshot";
    private static final String SENSORS_FILE_NAME = "Sensors";

    // For the shadow map
    private static final String SHADOW_SUMMARY_FILE_NAME = "ShadowModel_SummaryIndividuals";
    private static final String SHADOW_SNAPSHOT_FILE_NAME = "ShadowModel_Snapshot";
    private static final String SHADOW_SENSORS_FILE_NAME = "ShadowModel_Sensors";


    @BeforeEach
    public void init() {
        realMap = new RealMap(MAP_SIZE, DISPLAY, DATA_FOLDER_NAME, SUMMARY_FILE_NAME, PREDATION_FILE_NAME, SNAPSHOT_FILE_NAME, SENSORS_FILE_NAME);
    }

    @Test
    public void shadowMapIsSimilarToRealMapAtCreation() {
        fillRealMap();
        ShadowMap shadowMap = new ShadowMap(realMap, SHADOW_SUMMARY_FILE_NAME, SHADOW_SNAPSHOT_FILE_NAME, SHADOW_SENSORS_FILE_NAME);
        checkIfShadowMapAndRealMapAreSimilar(shadowMap);
    }

    @Test
    public void shadowMapIsSimilarToRealMapAfterReset() {
        ShadowMap shadowMap = new ShadowMap(realMap, SHADOW_SUMMARY_FILE_NAME, SHADOW_SNAPSHOT_FILE_NAME, SHADOW_SENSORS_FILE_NAME);
        fillRealMap();
        shadowMap.reset();
        checkIfShadowMapAndRealMapAreSimilar(shadowMap);
    }

    @Test
    public void checkWhetherTheNumberOfRandomlyCreatedIndividualsIsCorrect() {
        ShadowMap shadowMap = new ShadowMap(realMap, SHADOW_SUMMARY_FILE_NAME, SHADOW_SNAPSHOT_FILE_NAME, SHADOW_SENSORS_FILE_NAME);
        int number = 92;
        ShadowIndividual individual = new ShadowIndividual(64, 1770, 1840, 1936);
        shadowMap.addIndividual(individual);
        assertEquals(0, shadowMap.babies.size());
        shadowMap.createRandomIndividuals(number);
        assertEquals(number, shadowMap.babies.size());
    }

    @Test
    public void tryingToCreateIndividualsWhileThereIsNoIndividualsThrowsRuntimeException() {
        ShadowMap shadowMap = new ShadowMap(realMap, SHADOW_SUMMARY_FILE_NAME, SHADOW_SNAPSHOT_FILE_NAME, SHADOW_SENSORS_FILE_NAME);
        int number = 42;
        assertEquals(0, shadowMap.getAllIndividuals().size());
        assertThrows(RuntimeException.class, () -> shadowMap.createRandomIndividuals(number));
    }

    @Test
    public void checkWhetherTheNumberOfRandomlyRemovedIndividualsIsCorrect() {
        ShadowMap shadowMap = new ShadowMap(realMap, SHADOW_SUMMARY_FILE_NAME, SHADOW_SNAPSHOT_FILE_NAME, SHADOW_SENSORS_FILE_NAME);
        int globalID = 30;
        int ancestor = 12;
        int date = 800;
        int parent = 2;
        int number = 10;
        for (int i = 0 ; i < number ; i++) {
            ShadowIndividual individual = new ShadowIndividual(globalID++, ancestor++, date++, parent++);
            shadowMap.addIndividual(individual);
        }
        assertEquals(0, shadowMap.remove.size());
        shadowMap.removeRandomIndividuals(number);
        assertEquals(number, shadowMap.remove.size());
    }

    @Test
    public void tryingToRemoveMoreIndividualsThanThereAreThrowsIllegalArgumentException() {
        ShadowMap shadowMap = new ShadowMap(realMap, SHADOW_SUMMARY_FILE_NAME, SHADOW_SNAPSHOT_FILE_NAME, SHADOW_SENSORS_FILE_NAME);
        int globalID = 30;
        int ancestor = 12;
        int date = 800;
        int parent = 2;
        int number = 10;
        for (int i = 0 ; i < number - 1 ; i++) {
            ShadowIndividual individual = new ShadowIndividual(globalID++, ancestor++, date++, parent++);
            shadowMap.addIndividual(individual);
        }
        assertEquals(0, shadowMap.remove.size());
        assertThrows(IllegalArgumentException.class, () -> shadowMap.removeRandomIndividuals(number));
    }




    private static void fillRealMap() {
        int globalID = 666;
        int time = 1789;

        double x = 42;
        double y = 42;

        realMap.setGlobalId(globalID);
        realMap.setTime(time);
        EmbodiedIndividual individual1 = new EmbodiedIndividual(x, y, 64, 1770, 1840, 1936);
        realMap.addIndividual((int) x++, (int) y++, individual1);
        EmbodiedIndividual individual2 = new EmbodiedIndividual(x, y, 18, 666, 102, 776);
        realMap.addIndividual((int) x++, (int) y++, individual2);
        realMap.remove.put(individual1.getID(), individual1);
        realMap.babies.add(new EmbodiedIndividual(x++, y++, 400, 977, 555, 222));
        realMap.moving.add(individual2);
        realMap.newPositions.add(x);
        realMap.newPositions.add(y);
    }

    private static void checkIfShadowMapAndRealMapAreSimilar(ShadowMap shadowMap) {
        assertSame(realMap, shadowMap.realMap);
        assertEquals(realMap.globalID.get(), shadowMap.globalID.get());
        assertEquals(realMap.time, shadowMap.time);
        assertEquals(realMap.dataFolderName, shadowMap.dataFolderName);
        assertEquals(realMap.cst_mut_factor, shadowMap.cst_mut_factor);
        assertEquals(realMap.cst_speed_factor, shadowMap.cst_speed_factor);
        assertEquals(realMap.cst_speed_max, shadowMap.cst_speed_max);
        assertEquals(realMap.cst_light_birth_dst, shadowMap.cst_light_birth_dst);
        assertEquals(realMap.cst_birth_dst, shadowMap.cst_birth_dst);
        assertEquals(realMap.cst_energy_max, shadowMap.cst_energy_max);
        assertEquals(realMap.cst_speed_cost, shadowMap.cst_speed_cost);
        assertEquals(realMap.cst_sensor_cost, shadowMap.cst_sensor_cost);
        assertEquals(realMap.cst_error_cost, shadowMap.cst_error_cost);
        assertEquals(realMap.cst_free_energy, shadowMap.cst_free_energy);
        assertEquals(realMap.cst_sensor_precision, shadowMap.cst_sensor_precision);
        assertEquals(realMap.cst_max_number_actions, shadowMap.cst_max_number_actions);
        assertEquals(realMap.cst_energy_cost_factor, shadowMap.cst_energy_cost_factor);
        assertEquals(realMap.cst_step_cost, shadowMap.cst_step_cost);
        checkEqualityOfLists(realMap.babies, shadowMap.babies);
        checkEqualityOfLists(new LinkedList<>(realMap.remove.values()), shadowMap.remove);
        checkEqualityOfLists(realMap.getAllIndividuals(), shadowMap.getAllIndividuals());
    }

    private static void checkEqualityOfLists(List<EmbodiedIndividual> realList, List<ShadowIndividual> shadowList) {
        List<Individual> rList = realList.stream()
                .map(ind -> (Individual) ind)
                .toList();
        List<Individual> sList = shadowList.stream()
                .map(ind -> (Individual) ind)
                .toList();
        assertEquals(rList.size(), sList.size());
        for (Individual realInd : rList) {
            assertTrue(sList.contains(realInd));
        }
    }

}
