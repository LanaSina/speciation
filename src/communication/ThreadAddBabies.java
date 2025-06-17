package communication;

import animals.EmbodiedIndividual;
import startup.Constants;

public class ThreadAddBabies implements Runnable {

    private final RealMap realMap;
    private final int threadNumber;

    public ThreadAddBabies(RealMap realMap, int threadNumber) {
        this.realMap = realMap;
        this.threadNumber = threadNumber;
    }

    public void run() {
        // compute bounds of the interval of `babies` to treat
        int total = realMap.babies.size();
        int chunk = total / Constants.NB_THREADS;
        int remainder = total % Constants.NB_THREADS;
        int start = threadNumber * chunk + Math.min(threadNumber, remainder);
        int end = start + chunk + (threadNumber < remainder ? 1 : 0);
        // treat one interval of `babies`
        for (int i = start; i < end; i++) {
            EmbodiedIndividual baby = realMap.babies.get(i);
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
}
