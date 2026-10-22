package application.runtime.sky;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassInstance;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.postprocessing.PostEffectsSystem;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class SkySystem extends SystemPackage {

    /*
     * Submits the sky pass render call each frame and queues the sky FBO
     * for compositing. Binds this window's own grid's Time, Sky, Sun, Moon,
     * and Celestial UBO instances so sky color, the sun-side glow, the sun,
     * the moon in its phase, and the turning stars all reflect this grid's
     * own location and date. The sun, moon and star pictures are sprites,
     * handed to the sky pass once on awake. The sun swirls as much as this
     * window's player squints, its flames turning faster and twisting into a
     * whirl while the eyes narrow. The sky pass has no knowledge of
     * weather or clouds — that is handled entirely by the weather fullscreen
     * pass.
     */

    // Internal
    private PassManager passManager;
    private SpriteManager spriteManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private PostEffectsSystem postEffectsSystem;
    private WorldSystem worldSystem;
    private SquintSystem squintSystem;

    // Render Target
    private PassInstance skyPass;
    private FBOInstance skyFbo;

    // Swirl
    private float swirlPhase;

    @Override
    protected void get() {

        // Internal
        this.passManager = get(PassManager.class);
        this.spriteManager = get(SpriteManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.postEffectsSystem = get(PostEffectsSystem.class);
        this.worldSystem = get(WorldSystem.class);
        this.squintSystem = get(SquintSystem.class);
    }

    @Override
    protected void awake() {
        this.skyPass = passManager.clonePass(RuntimeSetting.PASS_SKY);
        this.skyFbo = fboManager.cloneFbo(RuntimeSetting.FBO_SKY, context.getWindow());

        bindSkyBodyPictures();
    }

    @Override
    protected void update() {

        bindGridLightingData();
        bindSunSwirl();

        renderManager.pushRenderCall(
                skyPass.getModelInstance(),
                skyFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                context.getWindow());
        postEffectsSystem.pushSceneLayer(skyFbo, RuntimeSetting.LAYER_SKY);
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

    // Swirl \\

    private void bindSunSwirl() {

        float swirl = squintSystem.getSquint();
        MaterialInstance mat = skyPass.getModelInstance().getMaterial();

        swirlPhase = (swirlPhase + swirl * RuntimeSetting.SKY_SUN_SWIRL_SPEED * internal.getDeltaTime())
                % RuntimeSetting.SKY_SUN_SWIRL_PERIOD;

        mat.setUniform(RuntimeSetting.UNIFORM_SKY_SUN_SWIRL, swirl);
        mat.setUniform(RuntimeSetting.UNIFORM_SKY_SUN_SWIRL_PHASE, swirlPhase);
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