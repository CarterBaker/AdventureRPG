package editor.runtime.menueventsmanager.menus;

import editor.bootstrap.stamppipeline.stampmanager.StampManager;
import engine.root.BranchPackage;

public class StampBranch extends BranchPackage {

    /*
     * Menu event handlers for the Stamps tab. Choosing a stamp makes it the
     * stamp in use: a part the Sculpt Brush places next, shown in the Info
     * Panel, or a picture the Image Editor's Brush presses next, by the
     * stamps the tab shows; stepping moves through them in the order they
     * are listed. Tiles carry their stamp's name.
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
        stampManager.chooseStamp(stampName);
    }

    public void previousStamp() {
        stampManager.stepStamp(-1);
    }

    public void nextStamp() {
        stampManager.stepStamp(1);
    }
}
