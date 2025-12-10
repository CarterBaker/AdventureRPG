package com.AdventureRPG.core.geometrypipeline.modelmanager;

import com.AdventureRPG.core.kernel.HandleFrame;

public class ModelHandle extends HandleFrame {

    // Internal
    public final MeshHandle meshHandle;
    public final int shaderHandle;

    public ModelHandle(
            MeshHandle meshHandle,
            int shaderHandle) {

        // Internal
        this.meshHandle = meshHandle;
        this.shaderHandle = shaderHandle;
    }
}