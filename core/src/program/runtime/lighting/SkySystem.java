package program.runtime.lighting;

import program.bootstrap.shaderpipeline.pass.PassHandle;
import program.bootstrap.shaderpipeline.passmanager.PassManager;
import program.core.engine.SystemPackage;

public class SkySystem extends SystemPackage {

    /*
     * Pushes the sky pass to the render pipeline each frame.
     * Owned by RuntimeContext.
     */

    // Internal
    private PassManager passManager;

    // Pass
    private PassHandle skyPass;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.passManager = get(PassManager.class);
        this.skyPass = passManager.getPassHandleFromPassName("Sky");
    }

    @Override
    protected void update() {
        passManager.pushPass(skyPass, -10, context.getWindow());
    }
}