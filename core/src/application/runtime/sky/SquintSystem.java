package application.runtime.sky;

import application.bootstrap.calendarpipeline.util.CelestialUtility;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.weatherpipeline.util.SkyColorUtility;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.water.WaterSystem;
import application.runtime.world.WorldSystem;
import engine.assets.camera.CameraInstance;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class SquintSystem extends SystemPackage {

    /*
     * Narrows this window's view while its player looks into the sun. Each
     * frame the glare is measured from how near the camera looks to the sun,
     * how high the sun stands above the horizon, how clear this grid's sky
     * is, and whether anything solid stands between the eye and the sun; no
     * glare reaches an eye under the sea. The eyes close toward that glare
     * quickly and open again slowly, and while they are narrowed a fullscreen
     * pass darkens the top and bottom of the screen like closing lids. Nothing
     * is drawn while the eyes are fully open.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private RaycastManager raycastManager;
    private WorldSystem worldSystem;
    private WaterSystem waterSystem;

    // Render Target
    private PassHandle squintPass;
    private FBOInstance squintFbo;

    // Look
    private float lookInner;
    private float lookOuter;

    // Glare
    private final Vector3 sunDirection = new Vector3();
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
    }

    @Override
    protected void awake() {
        this.squintPass = passManager.getPassHandleFromPassName(RuntimeSetting.PASS_SQUINT);
        this.squintFbo = fboManager.cloneFbo(RuntimeSetting.FBO_SQUINT, context.getWindow());
    }

    @Override
    protected void update() {

        float glare = resolveGlare();
        float rate = glare > squint ? RuntimeSetting.SQUINT_CLOSE_RATE : RuntimeSetting.SQUINT_OPEN_RATE;

        squint += (glare - squint) * (1f - (float) Math.exp(-rate * internal.getDeltaTime()));
    }

    @Override
    protected void lateUpdate() {

        if (squint < RuntimeSetting.SQUINT_VISIBLE_THRESHOLD)
            return;

        squintPass.getModelInstance().getMaterial().setUniform(RuntimeSetting.UNIFORM_SQUINT, squint);

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

        float glare = resolveSunHeight() * resolveLook(camera.getDirection()) * resolveClearSky(grid);

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
}
