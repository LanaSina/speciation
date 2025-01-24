package communication;

import animals.EmbodiedIndividual;
import animals.Individual;
import animals.IndividualWithProperties;
import animals.ShadowIndividual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import visualization.Display;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A class for testing the ShadowMap class.
 */
public class ShadowMapTest {


    private static Map realMap;
    private static final int MAP_SIZE = 50;
    private static final Display DISPLAY = null;
    private static final String DATA_FOLDER_NAME = "../new_data/some_date/";
    private static final String SUMMARY_FILE_NAME = "SummaryIndividuals";
    private static final String PREDATION_FILE_NAME = "Predation";
    private static final String SNAPSHOT_FILE_NAME = "Snapshot";
    private static final String SENSORS_FILE_NAME = "Sensors";

    // For the shadow map
    private static final Display SHADOW_DISPLAY = null;
    private static final String SHADOW_SUMMARY_FILE_NAME = "ShadowModel_SummaryIndividuals";
    private static final String SHADOW_PREDATION_FILE_NAME = "ShadowModel_Predation";
    private static final String SHADOW_SNAPSHOT_FILE_NAME = "ShadowModel_Snapshot";
    private static final String SHADOW_SENSORS_FILE_NAME = "ShadowModel_Sensors";


    @BeforeEach
    public void init() {
        realMap = new RealMap(MAP_SIZE, DISPLAY, DATA_FOLDER_NAME, SUMMARY_FILE_NAME, PREDATION_FILE_NAME, SNAPSHOT_FILE_NAME, SENSORS_FILE_NAME);
    }

    @Test
    public void shadowMapIsSimilarToRealMapAtCreation() {
        fillRealMap();
        ShadowMap shadowMap = new ShadowMap(realMap, SHADOW_DISPLAY, SHADOW_SUMMARY_FILE_NAME, SHADOW_PREDATION_FILE_NAME, SHADOW_SNAPSHOT_FILE_NAME, SHADOW_SENSORS_FILE_NAME);
        checkWhetherShadowMapAndRealMapAreSimilar(shadowMap);
    }

    @Test
    public void shadowMapIsSimilarToRealMapAfterReset() {
        ShadowMap shadowMap = new ShadowMap(realMap, SHADOW_DISPLAY, SHADOW_SUMMARY_FILE_NAME, SHADOW_PREDATION_FILE_NAME, SHADOW_SNAPSHOT_FILE_NAME, SHADOW_SENSORS_FILE_NAME);
        fillRealMap();
        shadowMap.reset();
        checkWhetherShadowMapAndRealMapAreSimilar(shadowMap);
    }

    private static void fillRealMap() {
        int globalID = 666;
        int time = 1789;

        double x = 42;
        double y = 42;
        int id = 64;
        int firstAncestorID = 1770;
        int birthDate = 1840;
        int parentID = 1936;

        realMap.setGlobalId(globalID);
        realMap.setTime(time);
        IndividualWithProperties individual1 = new EmbodiedIndividual(x, y, id, firstAncestorID, birthDate, parentID);
        realMap.addIndividual((int) x, (int) y, individual1);
        x++;
        y++;
        id++;
        firstAncestorID++;
        birthDate++;
        parentID++;
        IndividualWithProperties individual2 = new EmbodiedIndividual(x, y, id, firstAncestorID, birthDate, parentID);
        realMap.addIndividual((int) x, (int) y, individual2);
        x++;
        y++;
        id++;
        firstAncestorID++;
        birthDate++;
        parentID++;
        realMap.remove.add(individual1);
        realMap.babies.add(new EmbodiedIndividual(x, y, id, firstAncestorID, birthDate, parentID));
        x++;
        y++;
        realMap.moving.add(individual2);
        realMap.newPositions.add(x);
        realMap.newPositions.add(y);
    }

    private static void checkWhetherShadowMapAndRealMapAreSimilar(ShadowMap shadowMap) {
        assertSame(realMap, shadowMap.realMap);
        assertNotSame(realMap.map, shadowMap.map);
        assertEquals(realMap.size, shadowMap.size);
        assertEquals(realMap.globalID, shadowMap.globalID);
        assertEquals(realMap.time, shadowMap.time);
        assertEquals(realMap.dataFolderName, shadowMap.dataFolderName);
        assertEquals(realMap.cst_mut_factor, shadowMap.cst_mut_factor);
        assertEquals(realMap.cst_speed_factor, shadowMap.cst_speed_factor);
        assertEquals(realMap.cst_speed_max, shadowMap.cst_speed_max);
        assertEquals(realMap.cst_light_birth_dst, shadowMap.cst_light_birth_dst);
        assertEquals(realMap.cst_birth_dst, shadowMap.cst_birth_dst);
        assertEquals(realMap.cst_grid_max, shadowMap.cst_grid_max);
        assertEquals(realMap.cst_energy_max, shadowMap.cst_energy_max);
        assertEquals(realMap.cst_speed_cost, shadowMap.cst_speed_cost);
        assertEquals(realMap.cst_sensor_cost, shadowMap.cst_sensor_cost);
        assertEquals(realMap.cst_error_cost, shadowMap.cst_error_cost);
        assertEquals(realMap.cst_free_energy, shadowMap.cst_free_energy);
        assertEquals(realMap.cst_sensor_precision, shadowMap.cst_sensor_precision);
        assertEquals(realMap.cst_max_number_actions, shadowMap.cst_max_number_actions);
        assertEquals(realMap.cst_energy_cost_factor, shadowMap.cst_energy_cost_factor);
        assertEquals(realMap.cst_step_cost, shadowMap.cst_step_cost);
        assertNotSame(realMap.babies, shadowMap.babies);
        assertEquals(realMap.babies, shadowMap.babies);
        assertNotSame(realMap.remove, shadowMap.remove);
        assertEquals(realMap.remove, shadowMap.remove);
        assertNotSame(realMap.moving, shadowMap.moving);
        assertEquals(realMap.moving, shadowMap.moving);
        assertNotSame(realMap.newPositions, shadowMap.newPositions);
        assertEquals(realMap.newPositions, shadowMap.newPositions);

        // Check whether the individuals are similar and present in the same quantity
        for (int i = 0; i < realMap.size; i++) {
            for (int j = 0; j < realMap.size; j++) {
                Cell realCell = realMap.map[i][j];
                Cell shadowCell = shadowMap.map[i][j];
                assertSame(realCell.creatures.size(), shadowCell.creatures.size());
                // Compare each real individual with the shadow individual with the same ID
                for (Individual realInd : realCell.creatures) {
                    ShadowIndividual shadowIndividual = null;
                    for (Individual shadowInd : shadowCell.creatures) {
                        if (shadowInd.getID() == realInd.getID()) {
                            shadowIndividual = (ShadowIndividual) shadowInd;
                            break;
                        }
                    }
                    assertEquals(realInd, shadowIndividual);

                }
            }
        }
    }

}
