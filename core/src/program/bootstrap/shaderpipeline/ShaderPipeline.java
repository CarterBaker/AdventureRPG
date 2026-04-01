package program.bootstrap.shaderpipeline;

import program.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import program.bootstrap.shaderpipeline.passmanager.PassManager;
import program.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import program.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import program.bootstrap.shaderpipeline.texturemanager.TextureManager;
import program.bootstrap.shaderpipeline.ubomanager.UBOManager;
import program.core.engine.PipelinePackage;

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
