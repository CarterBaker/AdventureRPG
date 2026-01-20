package com.internal.runtime.debug;

import com.internal.bootstrap.shaderpipeline.passmanager.PassHandle;
import com.internal.bootstrap.shaderpipeline.passmanager.PassManager;
import com.internal.core.engine.SystemPackage;

public class Sky extends SystemPackage {

    // Internal
    private PassManager passmanager;

    // Shader
    private PassHandle skyPass;

    // Debug
    private PassHandle debugPass;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.passmanager = get(PassManager.class);

        // Shader
        int skyPassID = passmanager.getPassIDFromPassName("Sky");
        this.skyPass = passmanager.getPassFromPassID(skyPassID);

        // Debug
        int debugPassID = passmanager.getPassIDFromPassName("DebugCameraOrientationHUD");
        this.debugPass = passmanager.getPassFromPassID(debugPassID);
    }

    @Override
    protected void update() {
        passmanager.pushPass(skyPass, -10);
        passmanager.pushPass(debugPass, 10);
    }
}
