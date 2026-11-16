package editor.runtime.menueventsmanager.menus;

import editor.bootstrap.stamppipeline.stampmanager.StampManager;
import engine.root.BranchPackage;

public class StampBranch extends BranchPackage {

    /*
     * Menu event handlers for the Stamps tab. Choosing a stamp makes it the
     * stamp in use, the part placed next, and shows it in the Info Panel; stepping moves through the stamps in the order they are listed.
     * Tiles carry their stamp's name.
     */

    // Internal
    private StampManager stampManager;

    // Base \\

    @Override
    protected void get() {
        this.stampManager = get(StampManager.class);
    }

    // Stamps \\

    public void selectStamp(String stampName) {
        stampManager.selectStamp(stampName);
        stampManager.showActiveStamp();
    }

    public void previousStamp() {
        stampManager.cycleStamp(-1);
        stampManager.showActiveStamp();
    }

    public void nextStamp() {
        stampManager.cycleStamp(1);
        stampManager.showActiveStamp();
    }
}
