package application.runtime.water;

import java.util.SplittableRandom;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.oceanpipeline.util.OceanWaveUtility;
import application.bootstrap.oceanpipeline.wave.WaveInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.runtime.RuntimeSetting;
import application.runtime.particle.ParticlePoolStruct;
import application.runtime.particle.ParticleUtility;
import application.runtime.weather.PrecipitationSystem;
import application.runtime.world.WorldSystem;
import engine.assets.camera.CameraInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class BubbleSystem extends SystemPackage {

    /*
     * The breath of whoever this window looks through, while its camera is under water. Now and then a puff of
     * bubbles is breathed out just beside and below the view, each bubble pushed out toward a side and rising, so the
     * puff comes up through the sides of the screen: seldom and only a bubble or two just under the surface, often
     * and many at a time deep down. Every bubble lives in the world, so swimming leaves it behind, rises faster the
     * bigger it is, and pops as it reaches the surface. All are drawn as cards facing the camera from the bubble's
     * picture, one instanced draw into the precipitation target, hidden behind the scene and the surface above by
     * their depth. Surfacing lets every bubble go.
     */

    // Internal
    private PlayerManager playerManager;
    private ClockManager clockManager;
    private RenderManager renderManager;
    private MaterialManager materialManager;
    private MeshManager meshManager;
    private VAOManager vaoManager;
    private SpriteManager spriteManager;
    private CompositeBufferManager compositeBufferManager;
    private WorldSystem worldSystem;
    private WaterSystem waterSystem;
    private PrecipitationSystem precipitationSystem;

    // Draw
    private CompositeBufferInstance buffer;
    private MaterialInstance material;
    private float[] instance;

    // Breath — the wait until the next puff, the bubbles of this one still to come, and the wait between them
    private float breathSeconds;
    private int puffRemaining;
    private float puffSeconds;

    // Bubbles
    private ParticlePoolStruct bubbles;
    private SplittableRandom random;

    // Base \\

    @Override
    protected void create() {

        // Draw
        this.instance = new float[RuntimeSetting.BUBBLE_INSTANCE_FLOATS];

        // Bubbles
        this.bubbles = new ParticlePoolStruct(RuntimeSetting.BUBBLE_CAPACITY, RuntimeSetting.BUBBLE_FLOATS);
        this.random = new SplittableRandom(RuntimeSetting.BUBBLE_RANDOM_SEED);
    }

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.clockManager = get(ClockManager.class);
        this.renderManager = get(RenderManager.class);
        this.materialManager = get(MaterialManager.class);
        this.meshManager = get(MeshManager.class);
        this.vaoManager = get(VAOManager.class);
        this.spriteManager = get(SpriteManager.class);
        this.compositeBufferManager = get(CompositeBufferManager.class);
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
                ParticleUtility.resolveQuad(meshManager, vaoManager),
                RuntimeSetting.BUBBLE_INSTANCE_ATTR_SIZES);
        this.material = materialManager.cloneMaterial(
                materialManager.getMaterialIDFromMaterialName(RuntimeSetting.BUBBLE_MATERIAL));

        ParticleUtility.bindSceneDepth(
                material,
                worldSystem.getWorldFbo().getDepthTexture(),
                waterSystem.getWaterFbo().getDepthTexture());
        material.setUniform(
                RuntimeSetting.UNIFORM_BUBBLE_TEXTURE,
                spriteManager.getSpriteHandleFromSpriteName(RuntimeSetting.SPRITE_WATER_BUBBLE).getGpuHandle());
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

        if (grid == null || !playerManager.hasPlayerForWindow(windowID) || !waterSystem.isCameraSubmerged()) {
            bubbles.clear();
            puffRemaining = 0;
            breathSeconds = 0f;
            return;
        }

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        WorldHandle world = player.getWorldHandle();
        long chunkCoordinate = player.getWorldPositionStruct().getChunkCoordinate();
        double originX = ParticleUtility.resolveOriginX(chunkCoordinate);
        double originZ = ParticleUtility.resolveOriginZ(chunkCoordinate);
        CameraInstance camera = context.getWindow().getActiveCamera();
        WaveInstance wave = grid.getWaveInstance();
        float surface = wave.getCameraSurfaceHeightBlocks();
        float deltaTime = clockManager.isPaused() ? 0f : internal.getDeltaTime();

        advance(world, originX, originZ, camera.getPosition(), surface, deltaTime);
        breathe(originX, originZ, camera, wave.getCameraDepthBlocks(), deltaTime);
        lay(world, originX, originZ, surface);

        material.setUBO(grid.getSkyColorUBO());
        ParticleUtility.draw(
                renderManager, material, buffer, precipitationSystem.getPrecipitationFbo(), context.getWindow());
    }

    // Bubbles \\

    // Every bubble aged, risen ever faster, its push toward the side easing off; one at the surface pops, and one
    // that lived too long or was left far behind is let go
    private void advance(
            WorldHandle world,
            double originX,
            double originZ,
            Vector3 camera,
            float surface,
            float deltaTime) {

        float ease = Math.min(1f, RuntimeSetting.BUBBLE_DRAG * deltaTime);
        float reach = RuntimeSetting.BUBBLE_RETIRE_REACH;

        for (int bubble = bubbles.getCount() - 1; bubble >= 0; bubble--) {

            float rise = bubbles.getValue(bubble, RuntimeSetting.BUBBLE_RISE)
                    + RuntimeSetting.BUBBLE_RISE_ACCELERATION * deltaTime;
            float velocityX = bubbles.getValue(bubble, RuntimeSetting.BUBBLE_VELOCITY_X) * (1f - ease);
            float velocityZ = bubbles.getValue(bubble, RuntimeSetting.BUBBLE_VELOCITY_Z) * (1f - ease);

            bubbles.addValue(bubble, RuntimeSetting.BUBBLE_AGE, deltaTime);
            bubbles.setValue(bubble, RuntimeSetting.BUBBLE_RISE, rise);
            bubbles.setValue(bubble, RuntimeSetting.BUBBLE_VELOCITY_X, velocityX);
            bubbles.setValue(bubble, RuntimeSetting.BUBBLE_VELOCITY_Z, velocityZ);
            bubbles.move(bubble, velocityX * deltaTime, rise * deltaTime, velocityZ * deltaTime);

            float dx = ParticleUtility.toFrameX(world, bubbles.getX(bubble), originX) - camera.x;
            float dy = (float) bubbles.getY(bubble) - camera.y;
            float dz = ParticleUtility.toFrameZ(world, bubbles.getZ(bubble), originZ) - camera.z;

            if (bubbles.getY(bubble) + bubbles.getValue(bubble, RuntimeSetting.BUBBLE_RADIUS) >= surface
                    || bubbles.getValue(bubble, RuntimeSetting.BUBBLE_AGE) >= RuntimeSetting.BUBBLE_LIFE_MAX
                    || dx * dx + dy * dy + dz * dz > reach * reach)
                bubbles.retire(bubble);
        }
    }

    // The breath run down: a puff begun when it is due, as often and as full as the camera's depth calls for, and its
    // bubbles breathed out one by one, none just under the surface
    private void breathe(double originX, double originZ, CameraInstance camera, float depth, float deltaTime) {

        if (depth < RuntimeSetting.BUBBLE_DEPTH_MIN)
            return;

        float deep = OceanWaveUtility.smoothstep(
                RuntimeSetting.BUBBLE_DEPTH_SHALLOW, RuntimeSetting.BUBBLE_DEPTH_DEEP, depth);

        breathSeconds -= deltaTime;
        puffSeconds -= deltaTime;

        if (breathSeconds <= 0f) {
            breathSeconds = ParticleUtility.lerp(
                    RuntimeSetting.BUBBLE_BREATH_SHALLOW_SECONDS, RuntimeSetting.BUBBLE_BREATH_DEEP_SECONDS, deep)
                    * ParticleUtility.roll(
                            random, 1f - RuntimeSetting.BUBBLE_BREATH_JITTER, 1f + RuntimeSetting.BUBBLE_BREATH_JITTER);
            puffRemaining = Math.round(
                    ParticleUtility.lerp(RuntimeSetting.BUBBLE_PUFF_SHALLOW, RuntimeSetting.BUBBLE_PUFF_DEEP, deep)
                            + ParticleUtility.roll(random, 0f, ParticleUtility.lerp(
                                    RuntimeSetting.BUBBLE_PUFF_EXTRA_SHALLOW, RuntimeSetting.BUBBLE_PUFF_EXTRA_DEEP,
                                    deep)));
        }

        if (puffRemaining <= 0 || puffSeconds > 0f)
            return;

        puffRemaining--;
        puffSeconds = RuntimeSetting.BUBBLE_PUFF_GAP_SECONDS;
        spawnBubble(originX, originZ, camera);
    }

    // One bubble breathed out beside and below the view to one side, pushed on out toward that side
    private void spawnBubble(double originX, double originZ, CameraInstance camera) {

        Vector3 position = camera.getPosition();
        Vector3 forward = camera.getDirection();
        Vector3 up = camera.getUp();
        float rightX = forward.y * up.z - forward.z * up.y;
        float rightY = forward.z * up.x - forward.x * up.z;
        float rightZ = forward.x * up.y - forward.y * up.x;
        float side = random.nextBoolean() ? 1f : -1f;
        float ahead = ParticleUtility.roll(
                random, RuntimeSetting.BUBBLE_FORWARD_MIN, RuntimeSetting.BUBBLE_FORWARD_MAX);
        float aside = ParticleUtility.roll(random, RuntimeSetting.BUBBLE_SIDE_MIN, RuntimeSetting.BUBBLE_SIDE_MAX)
                * side;
        float drop = RuntimeSetting.BUBBLE_DROP;
        int bubble = bubbles.spawn(
                originX + position.x + forward.x * ahead + rightX * aside - up.x * drop,
                position.y + forward.y * ahead + rightY * aside - up.y * drop,
                originZ + position.z + forward.z * ahead + rightZ * aside - up.z * drop);

        if (bubble == EngineSetting.INDEX_NOT_FOUND)
            return;

        float radius = ParticleUtility.roll(random, RuntimeSetting.BUBBLE_RADIUS_MIN, RuntimeSetting.BUBBLE_RADIUS_MAX);
        float push = RuntimeSetting.BUBBLE_SPREAD_SPEED * side;

        bubbles.setValue(bubble, RuntimeSetting.BUBBLE_RADIUS, radius);
        bubbles.setValue(bubble, RuntimeSetting.BUBBLE_SEED, (float) random.nextDouble());
        bubbles.setValue(bubble, RuntimeSetting.BUBBLE_RISE,
                RuntimeSetting.BUBBLE_RISE_BASE + radius * RuntimeSetting.BUBBLE_RISE_PER_RADIUS);
        bubbles.setValue(bubble, RuntimeSetting.BUBBLE_VELOCITY_X, rightX * push);
        bubbles.setValue(bubble, RuntimeSetting.BUBBLE_VELOCITY_Z, rightZ * push);
    }

    // Draw \\

    // Every live bubble laid as an instance in the frame of the player's chunk, swelling in as it is breathed out and
    // thinning as it nears the surface
    private void lay(WorldHandle world, double originX, double originZ, float surface) {

        for (int bubble = 0; bubble < bubbles.getCount(); bubble++) {

            float radius = bubbles.getValue(bubble, RuntimeSetting.BUBBLE_RADIUS);
            float age = bubbles.getValue(bubble, RuntimeSetting.BUBBLE_AGE);
            float below = surface - (float) bubbles.getY(bubble) - radius;

            instance[0] = ParticleUtility.toFrameX(world, bubbles.getX(bubble), originX);
            instance[1] = (float) bubbles.getY(bubble);
            instance[2] = ParticleUtility.toFrameZ(world, bubbles.getZ(bubble), originZ);
            instance[3] = age;
            instance[4] = radius;
            instance[5] = bubbles.getValue(bubble, RuntimeSetting.BUBBLE_SEED);
            instance[6] = Math.min(
                    Math.min(1f, age / RuntimeSetting.BUBBLE_FADE_IN_SECONDS),
                    Math.min(1f, below / RuntimeSetting.BUBBLE_POP_BLOCKS));
            instance[7] = 0f;
            buffer.addInstance(instance);
        }
    }
}
