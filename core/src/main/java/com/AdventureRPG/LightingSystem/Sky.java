package com.AdventureRPG.lightingsystem;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.shaderpipeline.passmanager.PassManager;
import com.AdventureRPG.core.shaderpipeline.processingpass.ProcessingPass;
import com.AdventureRPG.core.shaderpipeline.ubomanager.UBOHandle;

public class Sky extends SystemPackage {

    // Root
    private PassManager passmanager;

    // Shader
    private int skyPassID;
    private ProcessingPass skyPass;
    private UBOHandle timeUBO;

    // Base \\

    @Override
    protected void init() {

        // Root
        this.passmanager = internal.get(PassManager.class);

        // Shader
        this.skyPassID = passmanager.getPassIDFromPassName("Sky");
        this.skyPass = passmanager.getPassFromPassID(skyPassID);
        this.timeUBO = skyPass.material.getUBO("TimeData");

        // Render management
        passmanager.pushPass(skyPass, -10);
    }

    // TODO: I do not know why I decided to do this here. old code. needs refactor
    // to more appropriate spot
    public void generateRandomOffsetFromDay(long day) {

        long mixed = day ^ System.currentTimeMillis();

        // Simple hash
        mixed ^= (mixed >>> 33);
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= (mixed >>> 33);

        // Normalize to [0,1)
        double normalized = (mixed & 0xFFFFFFL) / (double) (1 << 24);

        float noise = (float) Math.max(0.001, normalized);

        timeUBO.updateUniform("u_randomNoiseFromDay", noise);

        timeUBO.push();
    }
}
