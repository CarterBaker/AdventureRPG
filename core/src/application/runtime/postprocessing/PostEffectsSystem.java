package application.runtime.postprocessing;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassInstance;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.water.WaterSystem;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PostEffectsSystem extends SystemPackage {

    /*
     * Gathers this window's scene layers — sky, clouds, the lit world, water
     * and precipitation — into one scene target and runs the post effects over
     * it before it reaches the screen. Scene systems hand their targets to
     * pushSceneLayer() instead of the screen, and the layers are blitted in
     * layer order during render, once every one of them is queued for the
     * frame. A half-resolution prefilter measures each pixel's blur against
     * AutoFocusSystem's focus and draws out the bright parts, a
     * half-resolution blur gathers the depth of field and the bloom, and the
     * composite applies anti-aliasing, depth of field, bloom, color grading
     * and the lens effects at full resolution. Both half-resolution passes are
     * skipped while depth of field and bloom are off. The passes are cloned
     * for this window, so every window binds its own targets and focus.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private WorldSystem worldSystem;
    private WaterSystem waterSystem;
    private AutoFocusSystem autoFocusSystem;

    // Render Target
    private PassInstance prefilterPass;
    private PassInstance blurPass;
    private PassInstance compositePass;
    private FBOInstance sceneFbo;
    private FBOInstance prefilterFbo;
    private FBOInstance blurFbo;
    private FBOInstance compositeFbo;

    // Scene Layers
    private ObjectArrayList<FBOInstance> sceneLayers;
    private IntArrayList sceneLayerOrders;

    // Grain
    private float grainTime;

    // Base \\

    @Override
    protected void create() {

        // Scene Layers
        this.sceneLayers = new ObjectArrayList<>();
        this.sceneLayerOrders = new IntArrayList();
    }

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
        this.waterSystem = get(WaterSystem.class);
        this.autoFocusSystem = get(AutoFocusSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        this.prefilterPass = passManager.clonePass(RuntimeSetting.PASS_POST_PREFILTER);
        this.blurPass = passManager.clonePass(RuntimeSetting.PASS_POST_BLUR);
        this.compositePass = passManager.clonePass(RuntimeSetting.PASS_POST_COMPOSITE);
        this.sceneFbo = fboManager.cloneFbo(RuntimeSetting.FBO_POST_SCENE, window);
        this.prefilterFbo = fboManager.cloneFbo(RuntimeSetting.FBO_POST_PREFILTER, window);
        this.blurFbo = fboManager.cloneFbo(RuntimeSetting.FBO_POST_BLUR, window);
        this.compositeFbo = fboManager.cloneFbo(RuntimeSetting.FBO_POST_COMPOSITE, window);

        bindEffectTargets();
    }

    @Override
    protected void update() {
        grainTime = (grainTime + internal.getDeltaTime()) % RuntimeSetting.GRAIN_TIME_PERIOD;
    }

    @Override
    protected void render() {

        WindowInstance window = context.getWindow();

        bindFocusData();
        queueFocusSources(window);
        compositeSceneLayers(window);

        if (settings.depthOfField || settings.bloom) {
            pushPrefilterPass(window);
            pushBlurPass(window);
        }

        pushCompositePass(window);
        fboRenderSystem.pushFbo(compositeFbo, RuntimeSetting.LAYER_WORLD, window);
    }

    // Scene Layers \\

    public void pushSceneLayer(FBOInstance fbo, int layer) {
        sceneLayers.add(fbo);
        sceneLayerOrders.add(layer);
    }

    private void compositeSceneLayers(WindowInstance window) {

        for (int i = 0; i < sceneLayers.size(); i++) {

            FBOInstance layer = sceneLayers.get(i);

            renderManager.ensureFboRendered(layer, window);
            fboRenderSystem.pushFboToTarget(layer, sceneFbo, sceneLayerOrders.getInt(i), window);
        }

        renderManager.ensureFboRendered(sceneFbo, window);

        sceneLayers.clear();
        sceneLayerOrders.clear();
    }

    // Targets \\

    private void bindEffectTargets() {

        int sceneColor = sceneFbo.getColorTexture(RuntimeSetting.ATTACHMENT_COLOR);

        MaterialInstance prefilter = prefilterPass.getMaterial();
        prefilter.setUniform(RuntimeSetting.UNIFORM_SCENE_COLOR, sceneColor);

        MaterialInstance blur = blurPass.getMaterial();
        blur.setUniform(
                RuntimeSetting.UNIFORM_PREFILTER_FOCUS,
                prefilterFbo.getColorTexture(RuntimeSetting.ATTACHMENT_FOCUS));
        blur.setUniform(
                RuntimeSetting.UNIFORM_PREFILTER_BLOOM,
                prefilterFbo.getColorTexture(RuntimeSetting.ATTACHMENT_BLOOM));

        MaterialInstance composite = compositePass.getMaterial();
        composite.setUniform(RuntimeSetting.UNIFORM_SCENE_COLOR, sceneColor);
        composite.setUniform(
                RuntimeSetting.UNIFORM_BLUR_DEPTH_OF_FIELD,
                blurFbo.getColorTexture(RuntimeSetting.ATTACHMENT_DEPTH_OF_FIELD));
        composite.setUniform(
                RuntimeSetting.UNIFORM_BLUR_BLOOM,
                blurFbo.getColorTexture(RuntimeSetting.ATTACHMENT_BLOOM));
    }

    // The water target is cloned after this system wakes, so the depths are bound as each frame renders
    private void bindFocusData() {
        bindFocusData(prefilterPass.getMaterial());
        bindFocusData(compositePass.getMaterial());
    }

    private void bindFocusData(MaterialInstance material) {
        material.setUniform(RuntimeSetting.UNIFORM_SCENE_DEPTH, worldSystem.getWorldFbo().getDepthTexture());
        material.setUniform(RuntimeSetting.UNIFORM_WATER_DEPTH, waterSystem.getWaterFbo().getDepthTexture());
        material.setUniform(RuntimeSetting.UNIFORM_FOCUS_INVERSE, autoFocusSystem.getFocusInverse());
    }

    private void queueFocusSources(WindowInstance window) {
        renderManager.ensureFboRendered(worldSystem.getWorldFbo(), window);
        renderManager.ensureFboRendered(waterSystem.getWaterFbo(), window);
    }

    // Passes \\

    private void pushPrefilterPass(WindowInstance window) {
        renderManager.pushRenderCall(
                prefilterPass.getModelInstance(),
                prefilterFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                window);
    }

    private void pushBlurPass(WindowInstance window) {
        renderManager.ensureFboRendered(prefilterFbo, window);
        renderManager.pushRenderCall(
                blurPass.getModelInstance(),
                blurFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                window);
    }

    private void pushCompositePass(WindowInstance window) {
        compositePass.getMaterial().setUniform(RuntimeSetting.UNIFORM_GRAIN_TIME, grainTime);
        renderManager.pushRenderCall(
                compositePass.getModelInstance(),
                compositeFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                window);
    }
}
