package application.runtime.sky;

import application.bootstrap.calendarpipeline.util.CelestialUtility;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassInstance;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.weatherpipeline.util.SkyColorUtility;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.postprocessing.PostEffectsSystem;
import application.runtime.water.WaterSystem;
import application.runtime.weather.WeatherSystem;
import application.runtime.world.WorldSystem;
import engine.assets.camera.CameraInstance;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class SquintSystem extends SystemPackage {

    /*
     * Narrows this window's view while its player looks into the sun. Each
     * frame the glare is measured from how near the camera looks to the sun,
     * how high the sun stands above the horizon, how far inside the screen
     * it sits, how clear this grid's sky is, and whether anything solid
     * stands between the eye and the sun; no glare reaches an eye under the
     * sea. The eyes close toward that glare quickly and open again slowly,
     * and while they are narrowed a fullscreen pass darkens the top and
     * bottom of the screen like closing lids and blurs the finished frame
     * between them around a clear middle, easing open wherever the scene or
     * the clouds hide the sun's disc. The sky's sun swirls by the same
     * squint. Nothing is drawn while the eyes are fully open.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private RaycastManager raycastManager;
    private WorldSystem worldSystem;
    private WaterSystem waterSystem;
    private WeatherSystem weatherSystem;
    private PostEffectsSystem postEffectsSystem;

    // Render Target
    private PassInstance squintPass;
    private FBOInstance squintFbo;

    // Look
    private float lookInner;
    private float lookOuter;

    // Glare
    private final Vector3 sunDirection = new Vector3();
    private final Vector2 sunScreen = new Vector2();
    private final BlockCastStruct castStruct = new BlockCastStruct();
    private float squint;

    // Internal \\

    @Override
    protected void create() {

        // Look
        this.lookInner = (float) Math.cos(Math.toRadians(RuntimeSetting.SQUINT_LOOK_INNER_DEGREES));
        this.lookOuter = (float) Math.cos(Math.toRadians(RuntimeSetting.SQUINT_LOOK_OUTER_DEGREES));
    }

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.raycastManager = get(RaycastManager.class);
        this.worldSystem = get(WorldSystem.class);
        this.waterSystem = get(WaterSystem.class);
        this.weatherSystem = get(WeatherSystem.class);
        this.postEffectsSystem = get(PostEffectsSystem.class);
    }

    @Override
    protected void awake() {

        this.squintPass = passManager.clonePass(RuntimeSetting.PASS_SQUINT);
        this.squintFbo = fboManager.cloneFbo(RuntimeSetting.FBO_SQUINT, context.getWindow());

        MaterialInstance mat = squintPass.getModelInstance().getMaterial();
        FBOInstance weatherFbo = weatherSystem.getWeatherFbo();

        mat.setUniform(RuntimeSetting.UNIFORM_SCENE_DEPTH, worldSystem.getWorldFbo().getDepthTexture());
        mat.setUniform(RuntimeSetting.UNIFORM_CLOUD_COLOR, weatherFbo.getColorTexture(RuntimeSetting.ATTACHMENT_COLOR));
        mat.setUniform(
                RuntimeSetting.UNIFORM_SCENE_COLOR,
                postEffectsSystem.getCompositeFbo().getColorTexture(RuntimeSetting.ATTACHMENT_COLOR));
    }

    @Override
    protected void update() {

        float glare = resolveGlare();
        float rate = glare > squint ? RuntimeSetting.SQUINT_CLOSE_RATE : RuntimeSetting.SQUINT_OPEN_RATE;

        squint += (glare - squint) * (1f - (float) Math.exp(-rate * internal.getDeltaTime()));
    }

    @Override
    protected void render() {

        if (squint < RuntimeSetting.SQUINT_VISIBLE_THRESHOLD)
            return;

        MaterialInstance mat = squintPass.getModelInstance().getMaterial();

        mat.setUniform(RuntimeSetting.UNIFORM_SQUINT, squint);
        mat.setUniform(RuntimeSetting.UNIFORM_SQUINT_SUN_SCREEN, sunScreen);

        renderManager.ensureFboRendered(worldSystem.getWorldFbo(), context.getWindow());
        renderManager.ensureFboRendered(weatherSystem.getWeatherFbo(), context.getWindow());
        renderManager.ensureFboRendered(postEffectsSystem.getCompositeFbo(), context.getWindow());
        renderManager.pushRenderCall(
                squintPass.getModelInstance(),
                squintFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                context.getWindow());
        fboRenderSystem.pushFbo(squintFbo, RuntimeSetting.LAYER_SQUINT, context.getWindow());
    }

    // Glare \\

    private float resolveGlare() {

        GridInstance grid = worldSystem.getGridInstance();
        CameraInstance camera = context.getWindow().getActiveCamera();

        if (grid == null || camera == null || waterSystem.isCameraSubmerged())
            return 0f;

        CelestialUtility.resolveSunDirection(grid.getClockInstance().getVisualTimeOfDay(), sunDirection);

        float glare = resolveSunHeight()
                * resolveLook(camera.getDirection())
                * resolveOnScreen(camera)
                * resolveClearSky(grid);

        if (glare <= 0f || isSunBlocked(grid, camera))
            return 0f;

        return glare;
    }

    private float resolveSunHeight() {
        return SkyColorUtility.smoothstep(
                RuntimeSetting.SQUINT_SUN_ELEVATION_START,
                RuntimeSetting.SQUINT_SUN_ELEVATION_END,
                sunDirection.y);
    }

    private float resolveLook(Vector3 lookDirection) {

        float facing = (lookDirection.x * sunDirection.x
                + lookDirection.y * sunDirection.y
                + lookDirection.z * sunDirection.z) / lookDirection.length();

        return SkyColorUtility.smoothstep(lookOuter, lookInner, facing);
    }

    // Projects the sun onto the screen and fades the glare as it nears the screen's edge
    private float resolveOnScreen(CameraInstance camera) {

        float[] matrix = camera.getViewProjection().val;
        float clipX = matrix[0] * sunDirection.x + matrix[4] * sunDirection.y + matrix[8] * sunDirection.z;
        float clipY = matrix[1] * sunDirection.x + matrix[5] * sunDirection.y + matrix[9] * sunDirection.z;
        float clipW = matrix[3] * sunDirection.x + matrix[7] * sunDirection.y + matrix[11] * sunDirection.z;

        if (clipW <= 0f)
            return 0f;

        float ndcX = clipX / clipW;
        float ndcY = clipY / clipW;

        sunScreen.set(ndcX * 0.5f + 0.5f, ndcY * 0.5f + 0.5f);

        return 1f - SkyColorUtility.smoothstep(
                RuntimeSetting.SQUINT_SCREEN_EDGE_START,
                RuntimeSetting.SQUINT_SCREEN_EDGE_END,
                Math.max(Math.abs(ndcX), Math.abs(ndcY)));
    }

    private float resolveClearSky(GridInstance grid) {
        return 1f - SkyColorUtility.resolveOvercast(grid.getWeatherInstance()) * RuntimeSetting.SQUINT_OVERCAST_RELIEF;
    }

    private boolean isSunBlocked(GridInstance grid, CameraInstance camera) {

        raycastManager.castSolidBlock(
                grid.getActiveChunkCoordinate(),
                camera.getPosition(),
                sunDirection,
                RuntimeSetting.SQUINT_OCCLUSION_DISTANCE,
                castStruct);

        return castStruct.isHit();
    }

    // Accessible \\

    public float getSquint() {
        return squint;
    }
}
