package com.alife.tolsim.communication;

import com.alife.tolsim.animals.EmbodiedIndividual;

import java.util.ArrayList;

/**
 * Thread for adding babies to the RealMap.
 */
public class ThreadAddBabies extends ThreadApplyChanges {

    public ThreadAddBabies(RealMap realMap, ArrayList<EmbodiedIndividual> babies, int threadNumber) {
        super(realMap, babies, threadNumber);
    }

    /**
     * Adds one baby to the map.
     *
     * @param i the index of the creature to treat
     */
    protected void treatCreature(int i) {
        EmbodiedIndividual baby = creaturesToTreat.get(i);
        double[] position;
        synchronized (baby) {
            baby.setID(realMap.globalID.incrementAndGet());
            position = baby.getPosition();
        }
        int nx = (int) (position[0]+0.5);
        int ny = (int) (position[1]+0.5);
        realMap.addIndividual(nx, ny, baby);
        if (realMap.d != null)
            realMap.d.addComponent(baby);
    }
}
