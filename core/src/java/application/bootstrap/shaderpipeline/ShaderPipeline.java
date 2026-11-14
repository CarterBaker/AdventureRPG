package application.bootstrap.shaderpipeline;

import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.PipelinePackage;

public class ShaderPipeline extends PipelinePackage {

    /*
     * Registers the shader managers: shaders, UBOs and textures first, then the
     * materials, sprites and full-screen passes built on them.
     */

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
