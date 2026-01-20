package com.internal.bootstrap.renderpipeline.rendercall;

import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.core.engine.HandlePackage;

public class RenderCallHandle extends HandlePackage {

    // Internal
    private int handle;
    private ModelHandle modelHandle;

    // internal \\

    public void constructor(
            int handle,
            ModelHandle modelHandle) {

        // Internal
        this.handle = handle;
        this.modelHandle = modelHandle;
    }

    public void dispose() {
        this.modelHandle = null;
    }

    // Accessible \\

    public int getHandle() {
        return handle;
    }

    public ModelHandle getModelHandle() {
        return modelHandle;
    }
}
