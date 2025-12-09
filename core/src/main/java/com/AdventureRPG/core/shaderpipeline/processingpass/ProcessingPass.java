package com.AdventureRPG.core.shaderpipeline.processingpass;

import com.AdventureRPG.core.shaderpipeline.material.Material;

public class ProcessingPass {

    // Internal
    public final String passName;
    public final int passID;
    private final Material material;

    public ProcessingPass(
            String passName,
            int passID,
            Material material) {

        // Internal
        this.passName = passName;
        this.passID = passID;
        this.material = material;
    }

    // Accessible

    public Material getMaterial() {
        return material;
    }
}
