package com.internal.bootstrap.renderpipeline.rendercall;

import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.core.engine.HandlePackage;

public class RenderCallHandle extends HandlePackage {

    // Internal
    private ModelHandle modelHandle;

    // internal \\

    public void constructor(ModelHandle modelHandle) {

        // Internal
        this.modelHandle = modelHandle;
    }

    public void dispose() {
        this.modelHandle = null;
    }

    // Accessible \\

    public ModelHandle getModelHandle() {
        return modelHandle;
    }
}
