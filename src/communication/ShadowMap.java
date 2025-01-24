package communication;

import animals.Individual;
import animals.IndividualWithProperties;
import animals.ShadowIndividual;
import visualization.Display;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for shadow maps.
 */
public class ShadowMap extends Map {

    /**
     * The real map
     */
    Map realMap;

    /**
     * Creates a shadow version of the provided map.
     * </br>
     * This shadow map is initially similar to the provided map :
     * <ul>
     *     <li>same dimensions</li>
     *     <li>same number of individuals</li>
     *     <li>similar individuals (although they are not identical, see class <code>ShadowIndividual</code>)</li>
     *     <li>similar <code>babies</code>, <code>remove</code>, <code>moved</code> and <code>newPositions</code>
     *         (meaning they contain similar individuals)</li>
     *     <li>same global ID</li>
     *     <li>same time</li>
     *     <li>same data folder name</li>
     *     <li>same constants (e.g. mutation factor, speed factor, max speed, etc.)</li>
     * </ul>
     *
     * @param realMap           the real map
     * @param d                 the display
     * @param summaryFileName   the name of the individuals summary file
     * @param predationFileName the name of the predation file
     * @param snapshotFileName  the name of the snapshot file
     * @param sensorsFileName   the name of the sensors file
     */
    public ShadowMap(Map realMap,
                     Display d,
                     String summaryFileName,
                     String predationFileName,
                     String snapshotFileName,
                     String sensorsFileName) {
        super(realMap.size, d, realMap.dataFolderName, summaryFileName, predationFileName, snapshotFileName, sensorsFileName);
        this.realMap = realMap;
        this.reset();
    }

    /**
     * Reset this shadow map's state the real map's state:
     * <ul>
     *     <li>same global ID</li>
     *     <li>same time</li>
     *     <li>same number of individuals</li>
     *     <li>similar individuals (although they are different, see class <code>ShadowIndividual</code>)</li>
     *     <li>similar <code>babies</code>, <code>remove</code>, <code>moved</code> and <code>newPositions</code>
     *         (meaning they contain similar individual</li>
     * </ul>
     */
    public void reset() {
        globalID = realMap.globalID;
        time = realMap.time;
        // Reset individuals
        babies.clear();
        remove.clear();
        moving.clear();
        newPositions.clear();
        for (int i = 0; i < realMap.size; i++) {
            for (int j = 0; j < realMap.size; j++) {
                Cell realCell = realMap.map[i][j];
                Cell shadowCell = map[i][j];
                // Remove all individuals on the shadow cell
                shadowCell.creatures.clear();
                // Copy all the individuals from the real cell to the shadow cell
                for (Individual realIndividual : realCell.creatures) {
                    ShadowIndividual shadowIndividual = new ShadowIndividual((IndividualWithProperties) realIndividual);
                    shadowCell.creatures.add(shadowIndividual);
                    if (realMap.remove.contains(realIndividual))
                        remove.add(shadowIndividual);
                    if (realMap.moving.contains(realIndividual))
                        moving.add(shadowIndividual);
                }
            }
        }
        for (Individual baby : realMap.babies)
            babies.add(new ShadowIndividual((IndividualWithProperties) baby));
        newPositions.addAll(realMap.newPositions);
    }

    @Override
    public void updateCell(int x, int y) {
        // TODO
    }

    @Override
    protected MyLog createMyLog() {
        return new MyLog("Shadow map", true);
    }

}
