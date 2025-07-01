package com.alife.tolsim.communication;

import com.alife.tolsim.animals.EmbodiedIndividual;
import com.alife.tolsim.startup.Constants;

import java.io.IOException;
import java.util.List;


/**
 * Thread for removing the dead from the RealMap.
 */
public class ThreadRemoveDead extends ThreadApplyChanges {

    public ThreadRemoveDead(RealMap realMap, List<EmbodiedIndividual> removeAsList, int threadNumber) {
        super(realMap, removeAsList, threadNumber);
    }

    /**
     * Removes one dead from the map.
     *
     * @param i the index of the creature to treat
     */
    protected void treatCreature(int i) {
        EmbodiedIndividual creature = creaturesToTreat.get(i);
        if (Constants.Save && Constants.uniformDouble() <= Constants.SaveCoarse) {
            if (!creature.isLight() & !creature.parentIsLight()) {
                //write down info
                // "ID,pred_pos_x, pred_pos_y,isLight,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor, parentIsLight\n";
                String str = creature.stringDesc() + "\n";
                try {
                    synchronized (realMap.summaryWriter) {
                        realMap.summaryWriter.append(str);
                        realMap.summaryWriter.flush();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        //remove from display
        if (realMap.d != null)
            realMap.d.removeComponent(creature);
        //remove from map
        realMap.removeIndividual(creature);
    }
}
