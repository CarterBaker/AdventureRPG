package com.AdventureRPG.core.shaderpipeline;

import com.AdventureRPG.core.engine.PipelineFrame;
import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.core.shaderpipeline.passmanager.PassManager;
import com.AdventureRPG.core.shaderpipeline.shadermanager.ShaderManager;
import com.AdventureRPG.core.shaderpipeline.texturemanager.TextureManager;
import com.AdventureRPG.core.shaderpipeline.ubomanager.UBOManager;

public class ShaderPipeline extends PipelineFrame {

    // Render Pipeline
    private TextureManager textureManager;
    private UBOManager uboManager;
    private ShaderManager shaderManager;
    private MaterialManager materialManager;
    private PassManager passManager;

    @Override
    protected void create() {

        // Render Pipeline
        this.textureManager = create(TextureManager.class);
        this.uboManager = create(UBOManager.class);
        this.shaderManager = create(ShaderManager.class);
        this.materialManager = create(MaterialManager.class);
        this.passManager = create(PassManager.class);
    }
}
