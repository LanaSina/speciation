package communication;

import animals.EmbodiedIndividual;
import startup.Constants;

import java.io.IOException;
import java.util.List;

public class ThreadRemoveDead implements Runnable {

    private final RealMap realMap;
    private final List<EmbodiedIndividual> removeAsList;
    private final int threadNumber;

    public ThreadRemoveDead(RealMap realMap, List<EmbodiedIndividual> removeAsList, int threadNumber) {
        this.realMap = realMap;
        this.removeAsList = removeAsList;
        this.threadNumber = threadNumber;
    }

    public void run() {
        // compute bounds of the interval of `removeAsList` to treat
        int total = removeAsList.size();
        int chunk = total / Constants.NB_THREADS;
        int remainder = total % Constants.NB_THREADS;
        int start = threadNumber * chunk + Math.min(threadNumber, remainder);
        int end = start + chunk + (threadNumber < remainder ? 1 : 0);
        // treat one interval of `remove`
        for (int i = start; i < end; i++) {
            EmbodiedIndividual creature = removeAsList.get(i);
            if (Constants.Save) {
                if (Constants.uniformDouble() < 1) { //0.01
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
            }

            //remove from display
            if (realMap.d != null)
                realMap.d.removeComponent(creature);
            //remove from map
            realMap.removeIndividual(creature);
        }
    }
}
