package com.AdventureRPG.core.renderpipeline;

import com.AdventureRPG.core.kernel.PipelineFrame;
import com.AdventureRPG.core.renderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.core.renderpipeline.modelmanager.ModelManager;
import com.AdventureRPG.core.renderpipeline.passmanager.PassManager;
import com.AdventureRPG.core.renderpipeline.rendermanager.RenderManager;
import com.AdventureRPG.core.renderpipeline.shadermanager.ShaderManager;
import com.AdventureRPG.core.renderpipeline.texturemanager.TextureManager;
import com.AdventureRPG.core.renderpipeline.vaomanager.VAOManager;

public class RenderPipeline extends PipelineFrame {

    // Render Pipeline
    private TextureManager textureManager;
    private ShaderManager shaderManager;
    private MaterialManager materialManager;
    private PassManager passManager;
    private VAOManager vaoManager;
    private ModelManager modelManager;
    private RenderManager renderManager;

    @Override
    protected void create() {

        // Render Pipeline
        this.textureManager = (TextureManager) register(new TextureManager());
        this.shaderManager = (ShaderManager) register(new ShaderManager());
        this.materialManager = (MaterialManager) register(new MaterialManager());
        this.passManager = (PassManager) register(new PassManager());
        this.vaoManager = (VAOManager) register(new VAOManager());
        this.modelManager = (ModelManager) register(new ModelManager());
        this.renderManager = (RenderManager) register(new RenderManager());
    }

    // Render Pipeline \\

    public void draw() {

    }
}
