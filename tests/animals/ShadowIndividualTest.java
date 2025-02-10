package animals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A class for testing the ShadowIndividual class.
 */
public class ShadowIndividualTest {

    @Test
    public void shadowIndividualIsIdenticalToTheOriginalIndividualAtCreationIfParentIsNotLight() {

        int x = 42;
        int y = 666;
        int id = 64;
        int firstAncestorID = 1770;
        int birthDate = 1840;
        int parentID = 1936;

        IndividualWithProperties individual = new EmbodiedIndividual(x, y, id, firstAncestorID, birthDate, parentID);
        ShadowIndividual shadowIndividual = new ShadowIndividual(individual);

        assertEquals(individual, shadowIndividual);

    }

    @Test
    public void shadowIndividualIsIdenticalToTheOriginalIndividualAtCreationIfParentIsLight() {

        int x = 42;
        int y = 666;
        int id = 64;
        int firstAncestorID = 1770;
        int birthDate = 1840;
        int parentID = 1936;

        IndividualWithProperties individual = new EmbodiedIndividual(x, y, id, firstAncestorID, birthDate, parentID);
        individual.parentIsLight = true;
        ShadowIndividual shadowIndividual = new ShadowIndividual(individual);

        assertEquals(individual, shadowIndividual);

    }

}
