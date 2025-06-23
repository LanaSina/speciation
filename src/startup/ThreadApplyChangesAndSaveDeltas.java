package startup;

import communication.Map;
import oee_analysis.DeltasSaver;

public class ThreadApplyChangesAndSaveDeltas implements Runnable {

    private final Map map;
    private final DeltasSaver deltasSaver;
    private final int deltasSavedEvery;
    private final int t;

    public ThreadApplyChangesAndSaveDeltas(Map map, DeltasSaver deltasSaver, int deltasSavedEvery, int t) {
        this.map = map;
        this.deltasSaver = deltasSaver;
        this.deltasSavedEvery = deltasSavedEvery;
        this.t = t;
    }

    public void run() {
        map.applyChanges();
        if (t % deltasSavedEvery == 0)
            deltasSaver.update();
    }
}
