package animals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A class for testing the ShadowIndividual class.
 */
public class ShadowIndividualTest {

    @Test
    public void shadowIndividualIsIdenticalToTheOriginalIndividualAtCreation() {

        int x = 42;
        int y = 666;
        int id = 64;
        int firstAncestorID = 1770;
        int birthDate = 1840;
        int parentID = 1936;

        IndividualWithProperties individual = new EmbodiedIndividual(x, y, id, firstAncestorID, birthDate, parentID);
        ShadowIndividual shadowIndividual = new ShadowIndividual(individual);

        assertEquals(individual.cellTransparency, shadowIndividual.cellTransparency);
        assertEquals(individual.getSpeed(), shadowIndividual.getSpeed());
        assertEquals(individual.getMaxEnergy(), shadowIndividual.getMaxEnergy());
        assertEquals(individual.getKidEnergy(), shadowIndividual.getKidEnergy());
        assertEquals(individual.getMatForKids(), shadowIndividual.getMatForKids());
        assertEquals(individual.getNKids(), shadowIndividual.getNKids());
        assertEquals(individual.getDeath(), shadowIndividual.getDeath());

//        assertEquals(individual.getSensors(), shadowIndividual.getSensors()); // does not work: no method equals() in Tree
        assertEquals(id, shadowIndividual.getID());
        assertEquals(parentID, shadowIndividual.getParentID());
        assertEquals(firstAncestorID, shadowIndividual.getFirstAncestorID());
        assertEquals(birthDate, shadowIndividual.getBirthDate());

        assertEquals(individual.getLifeSpan(), shadowIndividual.getLifeSpan());
        assertEquals(individual.isLight(), shadowIndividual.isLight());

        assertEquals(individual.getEnergy(), shadowIndividual.getEnergy());
        assertEquals(x, shadowIndividual.getPosition()[0]);
        assertEquals(y, shadowIndividual.getPosition()[1]);
        assertEquals(individual.color, shadowIndividual.color);
        assertEquals(individual.borderColor, shadowIndividual.borderColor);

    }

}
