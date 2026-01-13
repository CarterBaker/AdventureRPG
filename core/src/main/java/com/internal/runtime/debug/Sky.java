package com.internal.runtime.debug;

import com.internal.bootstrap.shaderpipeline.passmanager.PassHandle;
import com.internal.bootstrap.shaderpipeline.passmanager.PassManager;
import com.internal.core.engine.SystemPackage;

public class Sky extends SystemPackage {

    // Root
    private PassManager passmanager;

    // Shader
    private int skyPassID;
    private PassHandle skyPass;

    // Base \\

    @Override
    protected void get() {

        // Root
        this.passmanager = get(PassManager.class);

        // Shader
        this.skyPassID = passmanager.getPassIDFromPassName("Sky");
        this.skyPass = passmanager.getPassFromPassID(skyPassID);

        // Render management
        passmanager.pushPass(skyPass, -10);

        int debugPassID = passmanager.getPassIDFromPassName("DebugCameraOrientationHUD");
        PassHandle debugPass = passmanager.getPassFromPassID(debugPassID);
        passmanager.pushPass(debugPass, 10);
    }
}
