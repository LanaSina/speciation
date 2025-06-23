package startup;

import communication.Map;
import oee_analysis.DeltasSaver;

public class ThreadApplyChangesAndSaveDeltas implements Runnable {

    private final Map map;
    private final DeltasSaver deltasSaver;

    public ThreadApplyChangesAndSaveDeltas(Map map, DeltasSaver deltasSaver) {
        this.map = map;
        this.deltasSaver = deltasSaver;
    }

    public void run() {
        map.applyChanges();
        deltasSaver.update();
    }
}
