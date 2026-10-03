package application.runtime.sky;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class SkySystem extends SystemPackage {

    /*
     * Submits the sky pass render call each frame and queues the sky FBO
     * for compositing. Binds this window's own grid's Time, Sky, Sun, Moon,
     * and Celestial UBO instances so sky color, the sun-side glow, the sun,
     * the moon in its phase, and the turning stars all reflect this grid's
     * own location and date. The sun, moon and star pictures are sprites,
     * handed to the sky pass once on awake. The sky pass has no knowledge of
     * weather or clouds — that is handled entirely by the weather fullscreen
     * pass.
     */

    // Internal
    private PassManager passManager;
    private SpriteManager spriteManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private WorldSystem worldSystem;

    // Render Target
    private PassHandle skyPass;
    private FBOInstance skyFbo;

    @Override
    protected void get() {

        // Internal
        this.passManager = get(PassManager.class);
        this.spriteManager = get(SpriteManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
    }

    @Override
    protected void awake() {
        this.skyPass = passManager.getPassHandleFromPassName(RuntimeSetting.PASS_SKY);
        this.skyFbo = fboManager.cloneFbo(RuntimeSetting.FBO_SKY, context.getWindow());

        bindSkyBodyPictures();
    }

    @Override
    protected void update() {

        bindGridLightingData();

        renderManager.pushRenderCall(
                skyPass.getModelInstance(),
                skyFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                context.getWindow());
        fboRenderSystem.pushFbo(skyFbo, RuntimeSetting.LAYER_SKY, context.getWindow());
    }

    // Grid Lighting \\

    private void bindGridLightingData() {

        GridInstance grid = worldSystem.getGridInstance();

        if (grid == null)
            return;

        MaterialInstance mat = skyPass.getModelInstance().getMaterial();

        mat.setUBO(grid.getTimeDataUBO());
        mat.setUBO(grid.getSkyColorUBO());
        mat.setUBO(grid.getSunLightUBO());
        mat.setUBO(grid.getMoonLightUBO());
        mat.setUBO(grid.getCelestialDataUBO());
    }

    // Sky Bodies \\

    private void bindSkyBodyPictures() {

        MaterialInstance mat = skyPass.getModelInstance().getMaterial();

        mat.setUniform(RuntimeSetting.UNIFORM_SKY_SUN_TEXTURE, resolvePicture(RuntimeSetting.SPRITE_SKY_SUN));
        mat.setUniform(
                RuntimeSetting.UNIFORM_SKY_SUN_FLAME_TEXTURE,
                resolvePicture(RuntimeSetting.SPRITE_SKY_SUN_FLAMES));
        mat.setUniform(RuntimeSetting.UNIFORM_SKY_MOON_TEXTURE, resolvePicture(RuntimeSetting.SPRITE_SKY_MOON));
        mat.setUniform(RuntimeSetting.UNIFORM_SKY_STAR_TEXTURE, resolvePicture(RuntimeSetting.SPRITE_SKY_STAR));
    }

    private int resolvePicture(String spriteName) {
        return spriteManager.getSpriteHandleFromSpriteName(spriteName).getGpuHandle();
    }

    // Accessible \\

    public FBOInstance getSkyFbo() {
        return skyFbo;
    }
}