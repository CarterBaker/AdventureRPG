package com.AdventureRPG.lightingsystem;

import com.AdventureRPG.core.kernel.SystemFrame;
import com.AdventureRPG.core.shaderpipeline.UBOManager.UBOHandle;
import com.AdventureRPG.core.shaderpipeline.passmanager.PassManager;
import com.AdventureRPG.core.shaderpipeline.processingpass.ProcessingPass;

public class Sky extends SystemFrame {

    // Root
    private PassManager passmanager;

    // Shader
    private int skyPassID;
    private ProcessingPass skyPass;
    private UBOHandle skyUBO;

    // Base \\

    @Override
    protected void init() {

        // Root
        this.passmanager = gameEngine.get(PassManager.class);

        // Shader
        skyPassID = passmanager.getPassIDFromPassName("sky");
        skyPass = passmanager.getPassFromPassID(skyPassID);
        skyUBO = skyPass.material.getUBO("sky");
    }

    public void generateRandomOffsetFromDay(long day) {

        long mixed = day ^ System.currentTimeMillis();

        // Simple hash
        mixed ^= (mixed >>> 33);
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= (mixed >>> 33);

        // Normalize to [0,1)
        double normalized = (mixed & 0xFFFFFFL) / (double) (1 << 24);

        float noise = (float) Math.max(0.001, normalized);

        skyUBO.update("u_randomNoiseFromDay", noise);

    }
}
