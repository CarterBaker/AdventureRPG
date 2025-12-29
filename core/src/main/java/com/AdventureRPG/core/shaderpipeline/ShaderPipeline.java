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
        this.textureManager = (TextureManager) register(new TextureManager());
        this.uboManager = (UBOManager) register(new UBOManager());
        this.shaderManager = (ShaderManager) register(new ShaderManager());
        this.materialManager = (MaterialManager) register(new MaterialManager());
        this.passManager = (PassManager) register(new PassManager());
    }
}
