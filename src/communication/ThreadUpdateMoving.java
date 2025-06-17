package communication;

import animals.EmbodiedIndividual;
import startup.Constants;

public class ThreadUpdateMoving implements Runnable {

    private final RealMap realMap;
    private final int threadNumber;

    public ThreadUpdateMoving(RealMap realMap, int threadNumber) {
        this.realMap = realMap;
        this.threadNumber = threadNumber;
    }

    public void run() {
        // compute bounds of the interval of `moving` to treat
        int total = realMap.moving.size();
        int chunk = total / Constants.NB_THREADS;
        int remainder = total % Constants.NB_THREADS;
        int start = threadNumber * chunk + Math.min(threadNumber, remainder);
        int end = start + chunk + (threadNumber < remainder ? 1 : 0);
        // treat one interval of `moving`
        for (int i = start; i < end; i++) {
            EmbodiedIndividual creature = realMap.moving.get(i);
            synchronized (creature) {
                double[] position = creature.getPosition();
                //new x,y
                int nx = (int) (realMap.newPositions.get(i*2)+0.5);
                int ny = (int) (realMap.newPositions.get(i*2+1)+0.5);
                realMap.updatePosition(nx,ny,creature);
                position[0] = realMap.newPositions.get(i*2);
                position[1] = realMap.newPositions.get(i*2+1);
                creature.setPosition(position);
            }
        }
    }
}
