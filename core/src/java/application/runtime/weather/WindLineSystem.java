package application.runtime.weather;

import java.util.SplittableRandom;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.weatherpipeline.wind.WindInstance;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.ColumnHeightUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.runtime.RuntimeSetting;
import application.runtime.particle.ParticlePoolStruct;
import application.runtime.particle.ParticleUtility;
import application.runtime.water.WaterSystem;
import application.runtime.world.WorldSystem;
import engine.assets.camera.CameraInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class WindLineSystem extends SystemPackage {

    /*
     * Cartoon wind for this window. On a gust, white lines are drawn through the air the way a cartoon draws the
     * wind: each is inked in from its tail, runs mostly straight downwind with a gentle wave, curls once up and over
     * through a loop, and is then erased from its tail the way it came in. Every line stands in the world, spawned in
     * the open air ahead of the camera, clear of the ground beneath it, and carried downwind while it lives, so it
     * keeps its place as the camera turns and slides past with true parallax as it moves. All are drawn as ribbons
     * bent along their paths by the vertex shader, one instanced draw into the precipitation target, hidden behind
     * the scene by its depth. They come more often the harder the wind blows past a breeze, and none come under
     * water or while the camera stands under a roof or in a cave.
     */

    // Internal
    private PlayerManager playerManager;
    private ClockManager clockManager;
    private RenderManager renderManager;
    private MaterialManager materialManager;
    private MeshManager meshManager;
    private VAOManager vaoManager;
    private CompositeBufferManager compositeBufferManager;
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private WorldSystem worldSystem;
    private WaterSystem waterSystem;
    private PrecipitationSystem precipitationSystem;

    // Draw
    private CompositeBufferInstance buffer;
    private MaterialInstance material;
    private float[] instance;

    // Lines
    private ParticlePoolStruct lines;
    private SplittableRandom random;
    private float spawnDebt;

    // Base \\

    @Override
    protected void create() {

        // Draw
        this.instance = new float[RuntimeSetting.WIND_LINE_INSTANCE_FLOATS];

        // Lines
        this.lines = new ParticlePoolStruct(RuntimeSetting.WIND_LINE_CAPACITY, RuntimeSetting.WIND_LINE_FLOATS);
        this.random = new SplittableRandom(RuntimeSetting.WIND_LINE_RANDOM_SEED);
    }

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.clockManager = get(ClockManager.class);
        this.renderManager = get(RenderManager.class);
        this.materialManager = get(MaterialManager.class);
        this.meshManager = get(MeshManager.class);
        this.vaoManager = get(VAOManager.class);
        this.compositeBufferManager = get(CompositeBufferManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.worldSystem = get(WorldSystem.class);
        this.waterSystem = get(WaterSystem.class);
        this.precipitationSystem = get(PrecipitationSystem.class);
    }

    @Override
    protected void awake() {

        // Draw
        this.buffer = create(CompositeBufferInstance.class);
        compositeBufferManager.constructor(
                buffer,
                ParticleUtility.resolveRibbon(meshManager, vaoManager),
                RuntimeSetting.WIND_LINE_INSTANCE_ATTR_SIZES);
        this.material = materialManager.cloneMaterial(
                materialManager.getMaterialIDFromMaterialName(RuntimeSetting.WIND_LINE_MATERIAL));

        ParticleUtility.bindSceneDepth(
                material,
                worldSystem.getWorldFbo().getDepthTexture(),
                waterSystem.getWaterFbo().getDepthTexture());
    }

    @Override
    protected void dispose() {
        compositeBufferManager.dispose(buffer);
    }

    // Update \\

    @Override
    protected void lateUpdate() {

        int windowID = context.getWindow().getWindowID();
        GridInstance grid = worldSystem.getGridInstance();

        buffer.clear();

        if (grid == null || !playerManager.hasPlayerForWindow(windowID) || waterSystem.isCameraSubmerged()) {
            lines.clear();
            return;
        }

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        WorldHandle world = player.getWorldHandle();
        long chunkCoordinate = player.getWorldPositionStruct().getChunkCoordinate();
        double originX = ParticleUtility.resolveOriginX(chunkCoordinate);
        double originZ = ParticleUtility.resolveOriginZ(chunkCoordinate);
        CameraInstance camera = context.getWindow().getActiveCamera();
        WindInstance wind = grid.getWindInstance();
        float deltaTime = clockManager.isPaused() ? 0f : internal.getDeltaTime();

        advance(world, originX, originZ, camera.getPosition(), wind, deltaTime);
        spawn(world, originX, originZ, camera, wind, deltaTime);
        lay(world, originX, originZ);

        material.setUBO(grid.getSkyColorUBO());
        ParticleUtility.draw(
                renderManager, material, buffer, precipitationSystem.getPrecipitationFbo(), context.getWindow());
    }

    // Lines \\

    // Every line aged and carried downwind by the wind blowing now, those whose life ran out or that fell far behind
    // the camera let go
    private void advance(
            WorldHandle world,
            double originX,
            double originZ,
            Vector3 cameraPosition,
            WindInstance wind,
            float deltaTime) {

        Vector3 direction = wind.getLocalWindDirection();
        float drift = wind.getLocalWindSpeed() * RuntimeSetting.WIND_LINE_DRIFT_SHARE * deltaTime;
        float reach = RuntimeSetting.WIND_LINE_RETIRE_REACH;

        for (int line = lines.getCount() - 1; line >= 0; line--) {

            lines.addValue(line, RuntimeSetting.WIND_LINE_AGE, deltaTime);
            lines.move(line, direction.x * drift, 0.0, direction.z * drift);

            float dx = ParticleUtility.toFrameX(world, lines.getX(line), originX) - cameraPosition.x;
            float dz = ParticleUtility.toFrameZ(world, lines.getZ(line), originZ) - cameraPosition.z;

            float age = lines.getValue(line, RuntimeSetting.WIND_LINE_AGE);

            if (age >= lines.getValue(line, RuntimeSetting.WIND_LINE_LIFE) || dx * dx + dz * dz > reach * reach)
                lines.retire(line);
        }
    }

    // New lines owed by the gust since the last frame, each spawned ahead of the camera, none while it is under cover
    private void spawn(
            WorldHandle world,
            double originX,
            double originZ,
            CameraInstance camera,
            WindInstance wind,
            float deltaTime) {

        Vector3 position = camera.getPosition();
        double cameraX = originX + position.x;
        double cameraZ = originZ + position.z;
        float gust = ParticleUtility.resolveGust(wind.getLocalWindSpeed());

        spawnDebt = Math.min(spawnDebt + gust * RuntimeSetting.WIND_LINE_RATE * deltaTime, 1f);

        if (spawnDebt < 1f || isSheltered(world, cameraX, position.y, cameraZ))
            return;

        spawnDebt -= 1f;
        spawnLine(world, cameraX, position.y, cameraZ, camera.getDirection(), wind.getLocalWindDirection(), gust);
    }

    // Under a roof or in a cave: the column over the camera rises above it
    private boolean isSheltered(WorldHandle world, double cameraX, float cameraY, double cameraZ) {

        int top = ColumnHeightUtility.findColumnTop(
                worldStreamManager, blockManager, world, (int) Math.floor(cameraX), (int) Math.floor(cameraZ));

        return top == EngineSetting.INDEX_NOT_FOUND || top > cameraY + RuntimeSetting.WIND_LINE_SHELTER;
    }

    // One line spawned at a spot ahead of the camera, lifted clear of the ground there, its path centred on the spot
    // and heading downwind
    private void spawnLine(
            WorldHandle world,
            double cameraX,
            float cameraY,
            double cameraZ,
            Vector3 forward,
            Vector3 windDirection,
            float gust) {

        float headingLength = (float) Math.sqrt(windDirection.x * windDirection.x + windDirection.z * windDirection.z);

        if (headingLength < RuntimeSetting.WIND_LINE_MIN_HEADING)
            return;

        float headingX = windDirection.x / headingLength;
        float headingZ = windDirection.z / headingLength;
        float yaw = (float) Math.atan2(forward.z, forward.x) + ParticleUtility.roll(
                random, -RuntimeSetting.WIND_LINE_SPREAD_RADIANS, RuntimeSetting.WIND_LINE_SPREAD_RADIANS);
        float reach = ParticleUtility.roll(
                random, RuntimeSetting.WIND_LINE_REACH_MIN, RuntimeSetting.WIND_LINE_REACH_MAX);
        double x = cameraX + Math.cos(yaw) * reach;
        double z = cameraZ + Math.sin(yaw) * reach;
        int top = ColumnHeightUtility.findColumnTop(
                worldStreamManager, blockManager, world, (int) Math.floor(x), (int) Math.floor(z));

        if (top == EngineSetting.INDEX_NOT_FOUND)
            return;

        float straight = ParticleUtility.roll(
                random, RuntimeSetting.WIND_LINE_STRAIGHT_MIN, RuntimeSetting.WIND_LINE_STRAIGHT_MAX);
        double y = Math.max(
                cameraY + ParticleUtility.roll(
                        random, RuntimeSetting.WIND_LINE_HEIGHT_MIN, RuntimeSetting.WIND_LINE_HEIGHT_MAX),
                top + RuntimeSetting.WIND_LINE_CLEARANCE
                        + ParticleUtility.roll(random, 0f, RuntimeSetting.WIND_LINE_CLEARANCE_SPREAD));
        int line = lines.spawn(x - headingX * straight * 0.5, y, z - headingZ * straight * 0.5);

        if (line == EngineSetting.INDEX_NOT_FOUND)
            return;

        lines.setValue(line, RuntimeSetting.WIND_LINE_LIFE, ParticleUtility.roll(
                random, RuntimeSetting.WIND_LINE_LIFE_MIN, RuntimeSetting.WIND_LINE_LIFE_MAX));
        lines.setValue(line, RuntimeSetting.WIND_LINE_HEADING_X, headingX);
        lines.setValue(line, RuntimeSetting.WIND_LINE_HEADING_Z, headingZ);
        lines.setValue(line, RuntimeSetting.WIND_LINE_STRAIGHT, straight);
        lines.setValue(line, RuntimeSetting.WIND_LINE_RADIUS, ParticleUtility.roll(
                random, RuntimeSetting.WIND_LINE_RADIUS_MIN, RuntimeSetting.WIND_LINE_RADIUS_MAX));
        lines.setValue(line, RuntimeSetting.WIND_LINE_LOOP_AT, ParticleUtility.roll(
                random, RuntimeSetting.WIND_LINE_LOOP_AT_MIN, RuntimeSetting.WIND_LINE_LOOP_AT_MAX));
        lines.setValue(line, RuntimeSetting.WIND_LINE_WAVE, ParticleUtility.roll(
                random, RuntimeSetting.WIND_LINE_WAVE_MIN, RuntimeSetting.WIND_LINE_WAVE_MAX));
        lines.setValue(line, RuntimeSetting.WIND_LINE_SEED, (float) random.nextDouble());
        lines.setValue(line, RuntimeSetting.WIND_LINE_OPACITY_SHARE, RuntimeSetting.WIND_LINE_OPACITY * gust
                * ParticleUtility.roll(random, RuntimeSetting.WIND_LINE_OPACITY_MIN_SHARE, 1f));
    }

    // Draw \\

    // Every live line laid as an instance in the frame of the player's chunk
    private void lay(WorldHandle world, double originX, double originZ) {

        for (int line = 0; line < lines.getCount(); line++) {

            instance[0] = ParticleUtility.toFrameX(world, lines.getX(line), originX);
            instance[1] = (float) lines.getY(line);
            instance[2] = ParticleUtility.toFrameZ(world, lines.getZ(line), originZ);
            instance[3] = lines.getValue(line, RuntimeSetting.WIND_LINE_AGE)
                    / lines.getValue(line, RuntimeSetting.WIND_LINE_LIFE);
            instance[4] = lines.getValue(line, RuntimeSetting.WIND_LINE_HEADING_X);
            instance[5] = lines.getValue(line, RuntimeSetting.WIND_LINE_HEADING_Z);
            instance[6] = lines.getValue(line, RuntimeSetting.WIND_LINE_STRAIGHT);
            instance[7] = lines.getValue(line, RuntimeSetting.WIND_LINE_RADIUS);
            instance[8] = lines.getValue(line, RuntimeSetting.WIND_LINE_LOOP_AT);
            instance[9] = lines.getValue(line, RuntimeSetting.WIND_LINE_WAVE);
            instance[10] = lines.getValue(line, RuntimeSetting.WIND_LINE_SEED);
            instance[11] = lines.getValue(line, RuntimeSetting.WIND_LINE_OPACITY_SHARE);
            buffer.addInstance(instance);
        }
    }
}
