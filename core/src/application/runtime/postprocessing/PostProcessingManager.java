package application.runtime.postprocessing;

import engine.root.ManagerPackage;

public class PostProcessingManager extends ManagerPackage {

    /*
     * Owns all post-processing systems. SSAOSystem must be created before
     * LightingSystem — lighting reads the SSAO FBO reference from SSAOSystem
     * at awake time. PostEffectsSystem gathers the scene layers and runs the
     * screen effects over them, focused by AutoFocusSystem.
     */

    @Override
    protected void create() {
        create(SSAOSystem.class);
        create(LightingSystem.class);
        create(AutoFocusSystem.class);
        create(PostEffectsSystem.class);
    }
}