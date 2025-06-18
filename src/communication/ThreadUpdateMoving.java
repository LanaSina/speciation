package communication;

import animals.EmbodiedIndividual;

import java.util.ArrayList;

/**
 * Thread for updating the positions of creatures that move on the RealMap.
 */
public class ThreadUpdateMoving extends ThreadApplyChanges {

    public ThreadUpdateMoving(RealMap realMap, ArrayList<EmbodiedIndividual> moving, int threadNumber) {
        super(realMap, moving, threadNumber);
    }

    /**
     * Updates the position of one creature that moves.
     *
     * @param i the index of the creature to treat
     */
    protected void treatCreature(int i) {
        EmbodiedIndividual creature = creaturesToTreat.get(i);
        synchronized (creature) {
            double[] position = creature.getPosition();
            //new x,y
            int nx = (int) (realMap.newPositions.get(i *2)+0.5);
            int ny = (int) (realMap.newPositions.get(i *2+1)+0.5);
            realMap.updatePosition(nx,ny,creature);
            position[0] = realMap.newPositions.get(i *2);
            position[1] = realMap.newPositions.get(i *2+1);
            creature.setPosition(position);
        }
    }
}
