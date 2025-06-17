package communication;

import animals.EmbodiedIndividual;
import startup.Constants;

import java.io.IOException;

public class ThreadRemoveDead implements Runnable {

    private final RealMap realMap;
    private final int threadNumber;

    public ThreadRemoveDead(RealMap realMap, int threadNumber) {
        this.realMap = realMap;
        this.threadNumber = threadNumber;
    }

    public void run() {
        // compute bounds of the interval of `remove` to treat
        int total = realMap.remove.size();
        int chunk = total / Constants.NB_THREADS;
        int remainder = total % Constants.NB_THREADS;
        int start = threadNumber * chunk + Math.min(threadNumber, remainder);
        int end = start + chunk + (threadNumber < remainder ? 1 : 0);
        // treat one interval of `remove`
        for (int i = start; i < end; i++) {
            EmbodiedIndividual creature = realMap.remove.get(i);
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
