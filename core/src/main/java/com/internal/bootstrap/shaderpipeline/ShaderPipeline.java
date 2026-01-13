package com.internal.bootstrap.shaderpipeline;

import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.passmanager.PassManager;
import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.PipelinePackage;

public class ShaderPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Render Pipeline
        create(TextureManager.class);
        create(UBOManager.class);
        create(ShaderManager.class);
        create(MaterialManager.class);
        create(PassManager.class);
    }
}
