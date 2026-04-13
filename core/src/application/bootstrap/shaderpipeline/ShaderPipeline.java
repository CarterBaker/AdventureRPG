package application.bootstrap.shaderpipeline;

import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.core.engine.PipelinePackage;

public class ShaderPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Render Pipeline
        create(ShaderManager.class);
        create(UBOManager.class);
        create(TextureManager.class);
        create(MaterialManager.class);
        create(SpriteManager.class);
        create(PassManager.class);
    }
}
