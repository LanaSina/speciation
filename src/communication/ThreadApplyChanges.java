package communication;

import animals.EmbodiedIndividual;
import startup.Constants;

import java.util.List;

/**
 * A generic class for threads that apply changes at a given time step, i.e. either births (`babies`), deaths (`remove`),
 * or movements (`moving`).
 */
public abstract class ThreadApplyChanges implements Runnable {

    /** The RealMap where the changes are supposed to be applied. */
    protected final RealMap realMap;
    /** The list of creatures to treat: either the babies, the dead, or the ones that move. */
    protected final List<EmbodiedIndividual> creaturesToTreat;
    /** The number of this thread. */
    protected final int threadNumber;

    /**
     * Base constructor for threads that apply changes at a time step.
     *
     * @param realMap the real map where the changes are supposed to be applied
     * @param creaturesToTreat the babies, the dead, or the ones that move
     * @param threadNumber the number of this thread
     */
    public ThreadApplyChanges(RealMap realMap, List<EmbodiedIndividual> creaturesToTreat, int threadNumber) {
        this.realMap = realMap;
        this.creaturesToTreat = creaturesToTreat;
        this.threadNumber = threadNumber;
    }

    /**
     * Treats one creature from the list of creatures to treat.
     *
     * @param i the index of the creature to treat
     */
    protected abstract void treatCreature(int i);

    public final void run() {
        // compute bounds of the interval of `creaturesToTreat` to treat
        int total = creaturesToTreat.size();
        int chunk = total / Constants.NB_THREADS;
        int remainder = total % Constants.NB_THREADS;
        int start = threadNumber * chunk + Math.min(threadNumber, remainder);
        int end = start + chunk + (threadNumber < remainder ? 1 : 0);
        // treat one interval of `creaturesToTreat`
        for (int i = start; i < end; i++) {
            treatCreature(i);
        }
    }

}
