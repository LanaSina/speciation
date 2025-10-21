//package com.alife.tolsim.animals;
//
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//
///**
// * A class for testing the ShadowIndividual class.
// */
//public class ShadowIndividualTest {
//
//    @Test
//    public void shadowIndividualIsIdenticalToTheOriginalIndividualAtCreationIfParentIsNotLight() {
//        IndividualWithProperties individual = new EmbodiedIndividual(42, 666, 64, 1770, 1840, 1936);
//        ShadowIndividual shadowIndividual = new ShadowIndividual(individual);
//        assertEquals(individual, shadowIndividual);
//    }
//
//    @Test
//    public void shadowIndividualIsIdenticalToTheOriginalIndividualAtCreationIfParentIsLight() {
//        IndividualWithProperties individual = new EmbodiedIndividual(42, 666, 64, 1770, 1840, 1936);
//        individual.parentIsLight = true;
//        ShadowIndividual shadowIndividual = new ShadowIndividual(individual);
//        assertEquals(individual, shadowIndividual);
//    }
//
//}
