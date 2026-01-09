package com.AdventureRPG.bootstrap.shaderpipeline;

import com.AdventureRPG.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.bootstrap.shaderpipeline.passmanager.PassManager;
import com.AdventureRPG.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import com.AdventureRPG.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.AdventureRPG.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.AdventureRPG.core.engine.PipelinePackage;

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
