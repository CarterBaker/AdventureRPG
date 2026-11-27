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
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.subchunk.LiquidFallStruct;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ColumnHeightUtility;
import application.bootstrap.worldpipeline.util.LiquidFlowUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.runtime.RuntimeSetting;
import application.runtime.particle.ParticlePoolStruct;
import application.runtime.particle.ParticleUtility;
import application.runtime.weather.PrecipitationSystem;
import application.runtime.world.WorldSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;

public class WaterfallSpraySystem extends SystemPackage {

    /*
     * The spray thrown up at the foot of every fall near this window's camera, a river stepping down its channel and
     * a torrent off a cliff alike. The feet the geometry build keeps per subchunk are gathered again every second, and
     * each throws up three kinds of particle across the pool face it churns, more and bigger the further the water
     * drops: splashes leaping up and falling back into the pool, foam riding off on the pool away from the fall and
     * spreading as it fades, and, once the drop is tall enough, mist rising and swelling over it. Every kind moves by
     * the same rules read from its row of the settings tables, so none needs a path of its own. All are drawn as
     * cards from one procedural shader, foam lying flat on the water and the rest facing the camera, one instanced
     * draw into the precipitation target, hidden behind the scene by its depth; none are drawn while the camera is
     * under water.
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
    private WorldSystem worldSystem;
    private WaterSystem waterSystem;
    private PrecipitationSystem precipitationSystem;

    // Draw
    private CompositeBufferInstance buffer;
    private MaterialInstance material;
    private float[] instance;

    // Sources — the centre of each foot's pool face in absolute blocks, its extents, its drop, the way away from the
    // fall and what each kind is owed of a particle
    private int sourceCount;
    private double[] sourceX;
    private double[] sourceY;
    private double[] sourceZ;
    private float[] sourceSizeX;
    private float[] sourceSizeZ;
    private float[] sourceDrop;
    private float[] sourceAwayX;
    private float[] sourceAwayZ;
    private float[] sourceDebts;
    private float sourceSeconds;

    // Spray
    private ParticlePoolStruct spray;
    private SplittableRandom random;

    // Base \\

    @Override
    protected void create() {

        // Draw
        this.instance = new float[RuntimeSetting.SPRAY_INSTANCE_FLOATS];

        // Sources
        int capacity = RuntimeSetting.SPRAY_SOURCE_CAPACITY;

        this.sourceX = new double[capacity];
        this.sourceY = new double[capacity];
        this.sourceZ = new double[capacity];
        this.sourceSizeX = new float[capacity];
        this.sourceSizeZ = new float[capacity];
        this.sourceDrop = new float[capacity];
        this.sourceAwayX = new float[capacity];
        this.sourceAwayZ = new float[capacity];
        this.sourceDebts = new float[capacity * RuntimeSetting.SPRAY_KINDS];

        // Spray
        this.spray = new ParticlePoolStruct(RuntimeSetting.SPRAY_CAPACITY, RuntimeSetting.SPRAY_FLOATS);
        this.random = new SplittableRandom(RuntimeSetting.SPRAY_RANDOM_SEED);
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
                RuntimeSetting.SPRAY_INSTANCE_ATTR_SIZES);
        this.material = materialManager.cloneMaterial(
                materialManager.getMaterialIDFromMaterialName(RuntimeSetting.SPRAY_MATERIAL));

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

        if (grid == null || !playerManager.hasPlayerForWindow(windowID)) {
            spray.clear();
            sourceCount = 0;
            return;
        }

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        WorldHandle world = player.getWorldHandle();
        long chunkCoordinate = player.getWorldPositionStruct().getChunkCoordinate();
        double originX = ParticleUtility.resolveOriginX(chunkCoordinate);
        double originZ = ParticleUtility.resolveOriginZ(chunkCoordinate);
        Vector3 camera = context.getWindow().getActiveCamera().getPosition();
        float deltaTime = clockManager.isPaused() ? 0f : internal.getDeltaTime();

        advance(world, originX, originZ, camera, deltaTime);
        spawn(world, originX + camera.x, camera.y, originZ + camera.z, deltaTime);

        if (waterSystem.isCameraSubmerged())
            return;

        lay(world, originX, originZ);

        material.setUBO(grid.getSkyColorUBO());
        ParticleUtility.draw(
                renderManager, material, buffer, precipitationSystem.getPrecipitationFbo(), context.getWindow());
    }

    // Spray \\

    // Every particle aged, pulled by its kind's gravity and eased by its drag; one falling back below the pool it
    // rose from, one that lived its life and one left far behind is let go
    private void advance(WorldHandle world, double originX, double originZ, Vector3 camera, float deltaTime) {

        float reach = RuntimeSetting.SPRAY_RETIRE_REACH;

        for (int particle = spray.getCount() - 1; particle >= 0; particle--) {

            int kind = (int) spray.getValue(particle, RuntimeSetting.SPRAY_KIND);
            float ease = 1f - Math.min(1f, RuntimeSetting.SPRAY_DRAG[kind] * deltaTime);
            float velocityX = spray.getValue(particle, RuntimeSetting.SPRAY_VELOCITY_X) * ease;
            float velocityY = (spray.getValue(particle, RuntimeSetting.SPRAY_VELOCITY_Y)
                    - RuntimeSetting.SPRAY_GRAVITY[kind] * deltaTime) * ease;
            float velocityZ = spray.getValue(particle, RuntimeSetting.SPRAY_VELOCITY_Z) * ease;

            spray.addValue(particle, RuntimeSetting.SPRAY_AGE, deltaTime);
            spray.setValue(particle, RuntimeSetting.SPRAY_VELOCITY_X, velocityX);
            spray.setValue(particle, RuntimeSetting.SPRAY_VELOCITY_Y, velocityY);
            spray.setValue(particle, RuntimeSetting.SPRAY_VELOCITY_Z, velocityZ);
            spray.move(particle, velocityX * deltaTime, velocityY * deltaTime, velocityZ * deltaTime);

            float dx = ParticleUtility.toFrameX(world, spray.getX(particle), originX) - camera.x;
            float dz = ParticleUtility.toFrameZ(world, spray.getZ(particle), originZ) - camera.z;
            float age = spray.getValue(particle, RuntimeSetting.SPRAY_AGE);

            if (age >= spray.getValue(particle, RuntimeSetting.SPRAY_LIFE)
                    || (velocityY < 0f && spray.getY(particle) < spray.getValue(particle, RuntimeSetting.SPRAY_FLOOR))
                    || dx * dx + dz * dz > reach * reach)
                spray.retire(particle);
        }
    }

    // The feet gathered again when due, then what every foot owes of each kind since the last frame thrown up from it
    private void spawn(WorldHandle world, double cameraX, float cameraY, double cameraZ, float deltaTime) {

        sourceSeconds -= deltaTime;

        if (sourceSeconds <= 0f) {
            sourceSeconds = RuntimeSetting.SPRAY_SOURCE_SECONDS;
            gatherSources(world, cameraX, cameraY, cameraZ);
        }

        for (int source = 0; source < sourceCount; source++) {

            float drop = sourceDrop[source];
            float area = sourceSizeX[source] * sourceSizeZ[source];

            for (int kind = 0; kind < RuntimeSetting.SPRAY_KINDS; kind++) {

                int debt = source * RuntimeSetting.SPRAY_KINDS + kind;
                float perBlock = RuntimeSetting.SPRAY_RATE_PER_BLOCK[kind]
                        * (1f + drop * RuntimeSetting.SPRAY_RATE_PER_DROP[kind]);
                float rate = Math.min(perBlock, RuntimeSetting.SPRAY_RATE_MAX_PER_BLOCK[kind])
                        * area
                        * OceanWaveUtility.smoothstep(
                                RuntimeSetting.SPRAY_DROP_MIN[kind], RuntimeSetting.SPRAY_DROP_FULL[kind], drop);

                sourceDebts[debt] += rate * deltaTime;

                while (sourceDebts[debt] >= 1f) {

                    sourceDebts[debt] -= 1f;

                    if (!spawnParticle(source, kind)) {
                        sourceDebts[debt] = 0f;
                        break;
                    }
                }
            }
        }
    }

    // Every foot near the camera, from the subchunks about it within reach above and below
    private void gatherSources(WorldHandle world, double cameraX, float cameraY, double cameraZ) {

        long center = ColumnHeightUtility.resolveColumnChunk(
                world, (int) Math.floor(cameraX), (int) Math.floor(cameraZ));
        int centerX = Coordinate2Long.unpackX(center);
        int centerZ = Coordinate2Long.unpackY(center);
        int chunks = RuntimeSetting.SPRAY_SOURCE_CHUNKS;
        float reach = RuntimeSetting.SPRAY_SOURCE_REACH;
        int lowest = Math.max(0, (int) Math.floor((cameraY - reach) / EngineSetting.CHUNK_SIZE));
        int highest = Math.min(EngineSetting.WORLD_HEIGHT - 1,
                (int) Math.floor((cameraY + reach) / EngineSetting.CHUNK_SIZE));

        sourceCount = 0;

        for (int offsetZ = -chunks; offsetZ <= chunks; offsetZ++)
            for (int offsetX = -chunks; offsetX <= chunks; offsetX++) {

                ChunkInstance chunk = worldStreamManager.getChunkInstance(WorldWrapUtility.wrapAroundWorld(
                        world, Coordinate2Long.pack(centerX + offsetX, centerZ + offsetZ)));

                if (chunk == null || chunk.getWorldHandle() != world)
                    continue;

                for (int subChunkIndex = lowest; subChunkIndex <= highest; subChunkIndex++) {

                    SubChunkInstance subChunk = chunk.getSubChunk(subChunkIndex);

                    if (subChunk != null)
                        gatherFeet(world, chunk, subChunkIndex, subChunk.getLiquidFalls(),
                                cameraX, cameraY, cameraZ);
                }
            }
    }

    // A subchunk's feet within reach of the camera taken as sources, while there is room for them
    private void gatherFeet(
            WorldHandle world,
            ChunkInstance chunk,
            int subChunkIndex,
            LiquidFallStruct falls,
            double cameraX,
            float cameraY,
            double cameraZ) {

        int count = falls.getCount();
        double chunkX = ParticleUtility.resolveOriginX(chunk.getCoordinate());
        double chunkZ = ParticleUtility.resolveOriginZ(chunk.getCoordinate());
        float reach = RuntimeSetting.SPRAY_SOURCE_REACH;

        for (int i = 0; i < count && sourceCount < RuntimeSetting.SPRAY_SOURCE_CAPACITY; i++) {

            int foot = falls.getFoot(i);

            if (foot == EngineSetting.LIQUID_FALL_FOOT_NONE)
                continue;

            int sizeX = LiquidFlowUtility.getFootSizeX(foot);
            int sizeZ = LiquidFlowUtility.getFootSizeZ(foot);
            double x = chunkX + LiquidFlowUtility.getFootX(foot) + sizeX * 0.5;
            double y = subChunkIndex * EngineSetting.CHUNK_SIZE + LiquidFlowUtility.getFootY(foot) + 1.0;
            double z = chunkZ + LiquidFlowUtility.getFootZ(foot) + sizeZ * 0.5;
            float dx = (float) WorldWrapUtility.wrappedBlockDeltaX(world, x, cameraX);
            float dy = (float) (y - cameraY);
            float dz = (float) WorldWrapUtility.wrappedBlockDeltaZ(world, z, cameraZ);

            if (dx * dx + dy * dy + dz * dz > reach * reach)
                continue;

            Direction3Vector side = Direction3Vector.VALUES[LiquidFlowUtility.getFootSide(foot)];
            int source = sourceCount++;

            sourceX[source] = x;
            sourceY[source] = y;
            sourceZ[source] = z;
            sourceSizeX[source] = sizeX;
            sourceSizeZ[source] = sizeZ;
            sourceDrop[source] = LiquidFlowUtility.getFootDrop(foot);
            sourceAwayX[source] = -side.x;
            sourceAwayZ[source] = -side.z;

            for (int kind = 0; kind < RuntimeSetting.SPRAY_KINDS; kind++)
                sourceDebts[source * RuntimeSetting.SPRAY_KINDS + kind] = 0f;
        }
    }

    // One particle of a kind thrown up from a point of a foot's pool face: sized, lived and sent off by its kind's row
    // of the tables, all of it growing with the drop; false while the pool is full
    private boolean spawnParticle(int source, int kind) {

        float drop = sourceDrop[source];
        double floor = sourceY[source];
        int particle = spray.spawn(
                sourceX[source] + (random.nextDouble() - 0.5) * sourceSizeX[source],
                floor + RuntimeSetting.SPRAY_LIFT[kind],
                sourceZ[source] + (random.nextDouble() - 0.5) * sourceSizeZ[source]);

        if (particle == EngineSetting.INDEX_NOT_FOUND)
            return false;

        float root = (float) Math.sqrt(drop);
        float speed = Math.min(
                RuntimeSetting.SPRAY_SPEED_BASE[kind] + root * RuntimeSetting.SPRAY_SPEED_PER_ROOT_DROP[kind],
                RuntimeSetting.SPRAY_SPEED_LIMIT[kind])
                * ParticleUtility.roll(random, RuntimeSetting.SPRAY_SPEED_JITTER_MIN, 1f);
        float turn = ParticleUtility.roll(random, 0f, RuntimeSetting.PARTICLE_FULL_TURN);
        float scatter = speed * RuntimeSetting.SPRAY_SCATTER[kind] * (float) random.nextDouble();
        float away = speed * RuntimeSetting.SPRAY_AWAY[kind];
        float size = Math.min(
                ParticleUtility.roll(random, RuntimeSetting.SPRAY_SIZE_MIN[kind], RuntimeSetting.SPRAY_SIZE_MAX[kind])
                        + drop * RuntimeSetting.SPRAY_SIZE_PER_DROP[kind],
                RuntimeSetting.SPRAY_SIZE_LIMIT[kind]);

        spray.setValue(particle, RuntimeSetting.SPRAY_LIFE, ParticleUtility.roll(
                random, RuntimeSetting.SPRAY_LIFE_MIN[kind], RuntimeSetting.SPRAY_LIFE_MAX[kind]));
        spray.setValue(particle, RuntimeSetting.SPRAY_KIND, kind);
        spray.setValue(particle, RuntimeSetting.SPRAY_SIZE, size);
        spray.setValue(particle, RuntimeSetting.SPRAY_SEED, (float) random.nextDouble());
        spray.setValue(particle, RuntimeSetting.SPRAY_FLOOR, (float) floor);
        spray.setValue(particle, RuntimeSetting.SPRAY_VELOCITY_X,
                sourceAwayX[source] * away + (float) Math.cos(turn) * scatter);
        spray.setValue(particle, RuntimeSetting.SPRAY_VELOCITY_Y, speed * RuntimeSetting.SPRAY_UPWARD[kind]);
        spray.setValue(particle, RuntimeSetting.SPRAY_VELOCITY_Z,
                sourceAwayZ[source] * away + (float) Math.sin(turn) * scatter);

        return true;
    }

    // Draw \\

    // Every live particle laid as an instance in the frame of the player's chunk: how far through its life it is, its
    // size, kind and seed, and how much of it shows as it fades in and out
    private void lay(WorldHandle world, double originX, double originZ) {

        for (int particle = 0; particle < spray.getCount(); particle++) {

            int kind = (int) spray.getValue(particle, RuntimeSetting.SPRAY_KIND);
            float age = spray.getValue(particle, RuntimeSetting.SPRAY_AGE);
            float life = spray.getValue(particle, RuntimeSetting.SPRAY_LIFE);

            instance[0] = ParticleUtility.toFrameX(world, spray.getX(particle), originX);
            instance[1] = (float) spray.getY(particle);
            instance[2] = ParticleUtility.toFrameZ(world, spray.getZ(particle), originZ);
            instance[3] = Math.min(1f, age / life);
            instance[4] = spray.getValue(particle, RuntimeSetting.SPRAY_SIZE);
            instance[5] = kind;
            instance[6] = spray.getValue(particle, RuntimeSetting.SPRAY_SEED);
            instance[7] = Math.max(0f, Math.min(
                    Math.min(1f, age / RuntimeSetting.SPRAY_FADE_IN_SECONDS[kind]),
                    (life - age) / RuntimeSetting.SPRAY_FADE_OUT_SECONDS[kind]));
            buffer.addInstance(instance);
        }
    }
}
