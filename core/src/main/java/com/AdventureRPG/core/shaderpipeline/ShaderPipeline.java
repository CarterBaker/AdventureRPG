package com.AdventureRPG.core.shaderpipeline;

import com.AdventureRPG.core.engine.PipelinePackage;
import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.core.shaderpipeline.passmanager.PassManager;
import com.AdventureRPG.core.shaderpipeline.shadermanager.ShaderManager;
import com.AdventureRPG.core.shaderpipeline.texturemanager.TextureManager;
import com.AdventureRPG.core.shaderpipeline.ubomanager.UBOManager;

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
