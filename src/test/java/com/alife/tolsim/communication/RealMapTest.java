package com.alife.tolsim.communication;

import com.alife.tolsim.animals.EmbodiedIndividual;
import com.alife.tolsim.animals.IndividualWithProperties;
import com.alife.tolsim.visualization.Display;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;


/**
 * A class for testing the RealMap class.
 */
public class RealMapTest {


    private static RealMap realMap;
    private static final int MAP_SIZE = 50;
    private static final Display DISPLAY = null;
    private static final String DATA_FOLDER_NAME = "../new_data/some_date/";
    private static final String SUMMARY_FILE_NAME = "SummaryIndividuals";
    private static final String PREDATION_FILE_NAME = "Predation";
    private static final String SNAPSHOT_FILE_NAME = "Snapshot";
    private static final String SENSORS_FILE_NAME = "Sensors";
    private static final String POPULATION_FILE_NAME = "Population";


    @BeforeEach
    public void init() {
        realMap = new RealMap(MAP_SIZE, DISPLAY, DATA_FOLDER_NAME, SUMMARY_FILE_NAME, PREDATION_FILE_NAME, SNAPSHOT_FILE_NAME, SENSORS_FILE_NAME, POPULATION_FILE_NAME);
    }

    @Test
    public void realMapIteratorHasNotNextWhenItIsEmpty() {
        Iterator<IndividualWithProperties> it = realMap.iterator();

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void realMapIteratorHasNextWhileThereAreIndividualsToIterateOn() {
        // add two individuals on the map
        double x1 = 0;
        double y1 = 0;
        EmbodiedIndividual individual1 = new EmbodiedIndividual(x1, y1, 64, 1770, 1840, 1936);
        realMap.addIndividual((int) x1, (int) y1, individual1);
        double x2 = realMap.map.length - 1;
        double y2 = realMap.map[0].length - 1; // by using 0 as an index, we assume the map has at least one cell
        EmbodiedIndividual individual2 = new EmbodiedIndividual(x2, y2, 18, 666, 102, 776);
        realMap.addIndividual((int) x2, (int) y2, individual2);

        Iterator<IndividualWithProperties> it = realMap.iterator();

        // should iterate through two individuals
        assertTrue(it.hasNext());
        assertSame(individual1, it.next());
        assertTrue(it.hasNext());
        assertSame(individual2, it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void realMapIteratorRemoveMethodThrowsUnsupportedOperationException() {
        Iterator<IndividualWithProperties> it = realMap.iterator();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

}
