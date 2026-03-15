package com.internal.runtime.debug;

import com.internal.bootstrap.shaderpipeline.pass.PassHandle;
import com.internal.bootstrap.shaderpipeline.passmanager.PassManager;
import com.internal.core.engine.SystemPackage;

public class Sky extends SystemPackage {

    // Internal
    private PassManager passManager;

    // Pass
    private PassHandle skyPass;

    // Internal \\

    @Override
    protected void get() {

        this.passManager = get(PassManager.class);
        this.skyPass = passManager.getPassHandleFromPassName("Sky");
    }

    @Override
    protected void update() {
        passManager.pushPass(skyPass, -10);
    }
}