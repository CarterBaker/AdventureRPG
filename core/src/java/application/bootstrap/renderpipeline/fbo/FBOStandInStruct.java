package application.bootstrap.renderpipeline.fbo;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.StructPackage;

public class FBOStandInStruct extends StructPackage {

    /*
     * What stands in for one window's layers: the window, the target its
     * layers are gathered into instead of being shown, the target shown in
     * their place, the model that draws that target once the layers are
     * gathered, and whether the window pushed any layer this frame.
     */

    // Window
    private final WindowInstance window;

    // Targets
    private FBOInstance gatherTarget;
    private FBOInstance standIn;
    private ModelInstance standInModel;

    // Frame
    private boolean gathered;

    // Constructor \\

    public FBOStandInStruct(WindowInstance window) {

        // Window
        this.window = window;
    }

    // Management \\

    public void set(FBOInstance gatherTarget, FBOInstance standIn, ModelInstance standInModel) {
        this.gatherTarget = gatherTarget;
        this.standIn = standIn;
        this.standInModel = standInModel;
    }

    // Accessible \\

    public WindowInstance getWindow() {
        return window;
    }

    public FBOInstance getGatherTarget() {
        return gatherTarget;
    }

    public FBOInstance getStandIn() {
        return standIn;
    }

    public ModelInstance getStandInModel() {
        return standInModel;
    }

    public boolean isGathered() {
        return gathered;
    }

    public void setGathered(boolean gathered) {
        this.gathered = gathered;
    }
}
