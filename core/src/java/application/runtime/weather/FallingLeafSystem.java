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
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.bootstrap.weatherpipeline.wind.WindInstance;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import application.bootstrap.worldpipeline.util.ColumnHeightUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.runtime.RuntimeSetting;
import application.runtime.particle.ParticlePoolStruct;
import application.runtime.particle.ParticleUtility;
import application.runtime.water.WaterSystem;
import application.runtime.world.WorldSystem;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FallingLeafSystem extends SystemPackage {

    /*
     * Leaves falling from the trees around this window's camera. The trees standing near it whose shape is already
     * grown are gathered again every second, and leaves are torn from them, a few in calm air and many in a gust, each
     * from a point inside one of its tree's own leaf clusters and in that cluster's color, now and then a dry one. A
     * leaf lives in the world: it drifts with the wind as it falls, rocking and spinning in the vertex shader, comes to
     * rest on the top of the column beneath it, lies there a while and fades. All are drawn as cards from the leaf's
     * picture, one instanced draw into the precipitation target, hidden behind the scene by its depth; none are drawn
     * while the camera is under water.
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
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private WorldSystem worldSystem;
    private WaterSystem waterSystem;
    private PrecipitationSystem precipitationSystem;

    // Draw
    private CompositeBufferInstance buffer;
    private MaterialInstance material;
    private float[] instance;

    // Sources — the grown trees near the camera, gathered again once a while
    private ObjectArrayList<TreeInstance> sources;
    private float sourceSeconds;

    // Leaves
    private ParticlePoolStruct leaves;
    private SplittableRandom random;
    private float spawnDebt;

    // Base \\

    @Override
    protected void create() {

        // Draw
        this.instance = new float[RuntimeSetting.LEAF_INSTANCE_FLOATS];

        // Sources
        this.sources = new ObjectArrayList<>();

        // Leaves
        this.leaves = new ParticlePoolStruct(RuntimeSetting.LEAF_CAPACITY, RuntimeSetting.LEAF_FLOATS);
        this.random = new SplittableRandom(RuntimeSetting.LEAF_RANDOM_SEED);
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
                ParticleUtility.resolveQuad(meshManager, vaoManager),
                RuntimeSetting.LEAF_INSTANCE_ATTR_SIZES);
        this.material = materialManager.cloneMaterial(
                materialManager.getMaterialIDFromMaterialName(RuntimeSetting.LEAF_MATERIAL));

        ParticleUtility.bindSceneDepth(
                material,
                worldSystem.getWorldFbo().getDepthTexture(),
                waterSystem.getWaterFbo().getDepthTexture());
        material.setUniform(
                RuntimeSetting.UNIFORM_LEAF_TEXTURE,
                spriteManager.getSpriteHandleFromSpriteName(RuntimeSetting.SPRITE_WEATHER_LEAF).getGpuHandle());
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
            leaves.clear();
            sources.clear();
            return;
        }

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        WorldHandle world = player.getWorldHandle();
        long chunkCoordinate = player.getWorldPositionStruct().getChunkCoordinate();
        double originX = ParticleUtility.resolveOriginX(chunkCoordinate);
        double originZ = ParticleUtility.resolveOriginZ(chunkCoordinate);
        Vector3 camera = context.getWindow().getActiveCamera().getPosition();
        WindInstance wind = grid.getWindInstance();
        float gust = ParticleUtility.resolveGust(wind.getLocalWindSpeed());
        float deltaTime = clockManager.isPaused() ? 0f : internal.getDeltaTime();

        advance(world, originX, originZ, camera, wind, gust, deltaTime);
        spawn(world, originX + camera.x, camera.y, originZ + camera.z, gust, deltaTime);

        if (waterSystem.isCameraSubmerged())
            return;

        lay(world, originX, originZ);

        material.setUBO(grid.getSkyColorUBO());
        ParticleUtility.draw(
                renderManager, material, buffer, precipitationSystem.getPrecipitationFbo(), context.getWindow());
    }

    // Leaves \\

    // Every leaf aged: one falling drifts with the wind and comes to rest on the column beneath it, one resting lies
    // there until its rest runs out, and any that lived too long or fell far behind the camera is let go
    private void advance(
            WorldHandle world,
            double originX,
            double originZ,
            Vector3 camera,
            WindInstance wind,
            float gust,
            float deltaTime) {

        Vector3 direction = wind.getLocalWindDirection();
        float driftX = direction.x * wind.getLocalWindSpeed() * RuntimeSetting.LEAF_DRIFT_SHARE;
        float driftZ = direction.z * wind.getLocalWindSpeed() * RuntimeSetting.LEAF_DRIFT_SHARE;
        float ease = Math.min(1f, RuntimeSetting.LEAF_DRAG * deltaTime);
        float drop = RuntimeSetting.LEAF_FALL_SPEED * (1f - RuntimeSetting.LEAF_GUST_FALL_SHARE * gust) * deltaTime;
        float reach = RuntimeSetting.LEAF_RETIRE_REACH;

        for (int leaf = leaves.getCount() - 1; leaf >= 0; leaf--) {

            leaves.addValue(leaf, RuntimeSetting.LEAF_AGE, deltaTime);

            float dx = ParticleUtility.toFrameX(world, leaves.getX(leaf), originX) - camera.x;
            float dz = ParticleUtility.toFrameZ(world, leaves.getZ(leaf), originZ) - camera.z;

            if (leaves.getValue(leaf, RuntimeSetting.LEAF_AGE) >= RuntimeSetting.LEAF_LIFE_MAX
                    || dx * dx + dz * dz > reach * reach) {
                leaves.retire(leaf);
                continue;
            }

            float rest = leaves.getValue(leaf, RuntimeSetting.LEAF_REST);

            if (rest >= 0f) {

                if (rest + deltaTime >= RuntimeSetting.LEAF_REST_SECONDS)
                    leaves.retire(leaf);
                else
                    leaves.setValue(leaf, RuntimeSetting.LEAF_REST, rest + deltaTime);

                continue;
            }

            float velocityX = leaves.getValue(leaf, RuntimeSetting.LEAF_VELOCITY_X);
            float velocityZ = leaves.getValue(leaf, RuntimeSetting.LEAF_VELOCITY_Z);

            velocityX += (driftX - velocityX) * ease;
            velocityZ += (driftZ - velocityZ) * ease;
            leaves.setValue(leaf, RuntimeSetting.LEAF_VELOCITY_X, velocityX);
            leaves.setValue(leaf, RuntimeSetting.LEAF_VELOCITY_Z, velocityZ);
            leaves.move(leaf, velocityX * deltaTime, -drop, velocityZ * deltaTime);

            if (!land(world, leaf))
                leaves.retire(leaf);
        }
    }

    // A falling leaf held to the ground of the column it is over, read again only when it crosses into another
    // column, laid to rest once it reaches it; false when that column is no longer loaded
    private boolean land(WorldHandle world, int leaf) {

        int columnX = (int) Math.floor(leaves.getX(leaf));
        int columnZ = (int) Math.floor(leaves.getZ(leaf));

        if (columnX != Float.floatToRawIntBits(leaves.getValue(leaf, RuntimeSetting.LEAF_COLUMN_X))
                || columnZ != Float.floatToRawIntBits(leaves.getValue(leaf, RuntimeSetting.LEAF_COLUMN_Z))) {

            int top = ColumnHeightUtility.findColumnTop(worldStreamManager, blockManager, world, columnX, columnZ);

            if (top == EngineSetting.INDEX_NOT_FOUND)
                return false;

            leaves.setValue(leaf, RuntimeSetting.LEAF_GROUND, top);
            leaves.setValue(leaf, RuntimeSetting.LEAF_COLUMN_X, Float.intBitsToFloat(columnX));
            leaves.setValue(leaf, RuntimeSetting.LEAF_COLUMN_Z, Float.intBitsToFloat(columnZ));
        }

        double rest = leaves.getValue(leaf, RuntimeSetting.LEAF_GROUND) + RuntimeSetting.LEAF_REST_LIFT;

        if (leaves.getY(leaf) > rest)
            return true;

        if (leaves.getY(leaf) > rest - RuntimeSetting.LEAF_REST_LIFT * 2f)
            leaves.setY(leaf, rest);

        leaves.setValue(leaf, RuntimeSetting.LEAF_REST, 0f);

        return true;
    }

    // Spawn \\

    // The trees gathered again when due, then the leaves owed since the last frame torn from them, more in a gust and
    // fewer where trees stand thin
    private void spawn(WorldHandle world, double cameraX, float cameraY, double cameraZ, float gust, float deltaTime) {

        sourceSeconds -= deltaTime;

        if (sourceSeconds <= 0f) {
            sourceSeconds = RuntimeSetting.LEAF_SOURCE_SECONDS;
            gatherSources(world, cameraX, cameraZ);
        }

        if (sources.isEmpty())
            return;

        float rate = (RuntimeSetting.LEAF_RATE_CALM + RuntimeSetting.LEAF_RATE_GUST * gust)
                * Math.min(1f, sources.size() / (float) RuntimeSetting.LEAF_SOURCE_FULL);

        spawnDebt = Math.min(spawnDebt + rate * deltaTime, 1f);

        if (spawnDebt < 1f)
            return;

        spawnDebt -= 1f;
        spawnLeaf(world, cameraX, cameraY, cameraZ, sources.get(random.nextInt(sources.size())));
    }

    // Every tree near the camera whose shape is grown, from the chunks about the one the camera stands over
    private void gatherSources(WorldHandle world, double cameraX, double cameraZ) {

        long center = ColumnHeightUtility.resolveColumnChunk(
                world, (int) Math.floor(cameraX), (int) Math.floor(cameraZ));
        int centerX = Coordinate2Long.unpackX(center);
        int centerZ = Coordinate2Long.unpackY(center);
        int chunks = RuntimeSetting.LEAF_SOURCE_CHUNKS;
        float reach = RuntimeSetting.LEAF_SOURCE_REACH;

        sources.clear();

        for (int offsetZ = -chunks; offsetZ <= chunks; offsetZ++)
            for (int offsetX = -chunks; offsetX <= chunks; offsetX++) {

                ChunkInstance chunk = worldStreamManager.getChunkInstance(WorldWrapUtility.wrapAroundWorld(
                        world, Coordinate2Long.pack(centerX + offsetX, centerZ + offsetZ)));

                if (chunk == null || chunk.getWorldHandle() != world)
                    continue;

                TreeInstance[] trees = chunk.getTreePaletteHandle().getTrees();

                for (int i = 0; i < trees.length; i++) {

                    TreeInstance tree = trees[i];
                    float dx = (float) WorldWrapUtility.wrappedBlockDeltaX(
                            world, tree.getAnchorX() + EngineSetting.TREE_ROOT_CENTER_BLOCKS, cameraX);
                    float dz = (float) WorldWrapUtility.wrappedBlockDeltaZ(
                            world, tree.getAnchorZ() + EngineSetting.TREE_ROOT_CENTER_BLOCKS, cameraZ);

                    if (tree.isRealized() && dx * dx + dz * dz <= reach * reach)
                        sources.add(tree);
                }
            }
    }

    // One leaf torn from a point inside one of a tree's leaf clusters, in that cluster's color, unless the tree's
    // shape has been let go since it was gathered or the point lies out of the camera's reach
    private void spawnLeaf(WorldHandle world, double cameraX, float cameraY, double cameraZ, TreeInstance tree) {

        if (!tree.isRealized())
            return;

        TreeShapeStruct shape = tree.getShape();

        if (shape.getLeafCount() == 0)
            return;

        int cluster = random.nextInt(shape.getLeafCount());
        float rise = ParticleUtility.roll(random, -1f, 1f);
        float turn = ParticleUtility.roll(random, 0f, RuntimeSetting.PARTICLE_FULL_TURN);
        float spread = (float) Math.sqrt(1f - rise * rise);
        float shell = ParticleUtility.roll(random, RuntimeSetting.LEAF_SHELL_MIN, 1f);
        double x = tree.getAnchorX() + EngineSetting.TREE_ROOT_CENTER_BLOCKS + shape.getLeafX(cluster)
                + Math.cos(turn) * spread * shape.getLeafRadiusH(cluster) * shell;
        double y = tree.getBaseY() + shape.getLeafY(cluster) + rise * shape.getLeafRadiusV(cluster) * shell;
        double z = tree.getAnchorZ() + EngineSetting.TREE_ROOT_CENTER_BLOCKS + shape.getLeafZ(cluster)
                + Math.sin(turn) * spread * shape.getLeafRadiusH(cluster) * shell;
        float dx = (float) WorldWrapUtility.wrappedBlockDeltaX(world, x, cameraX);
        float dy = (float) (y - cameraY);
        float dz = (float) WorldWrapUtility.wrappedBlockDeltaZ(world, z, cameraZ);
        float reach = RuntimeSetting.LEAF_SOURCE_REACH;

        if (dx * dx + dy * dy + dz * dz > reach * reach)
            return;

        int leaf = leaves.spawn(x, y, z);

        if (leaf == EngineSetting.INDEX_NOT_FOUND)
            return;

        leaves.setValue(leaf, RuntimeSetting.LEAF_SEED, (float) random.nextDouble());
        leaves.setValue(leaf, RuntimeSetting.LEAF_REST, RuntimeSetting.LEAF_AIRBORNE);
        leaves.setValue(leaf, RuntimeSetting.LEAF_COLUMN_X, Float.intBitsToFloat(EngineSetting.INDEX_NOT_FOUND));
        leaves.setValue(leaf, RuntimeSetting.LEAF_COLUMN_Z, Float.intBitsToFloat(EngineSetting.INDEX_NOT_FOUND));
        leaves.setValue(leaf, RuntimeSetting.LEAF_SIZE, ParticleUtility.roll(
                random, RuntimeSetting.LEAF_SIZE_MIN, RuntimeSetting.LEAF_SIZE_MAX));
        leaves.setValue(leaf, RuntimeSetting.LEAF_SPIN, ParticleUtility.roll(
                random, RuntimeSetting.LEAF_SPIN_MIN, RuntimeSetting.LEAF_SPIN_MAX));
        colorLeaf(leaf, tree, shape.isLeafAccent(cluster));
    }

    // A leaf given its tree's leaf or accent color, shaded a little lighter or darker, now and then turned dry
    private void colorLeaf(int leaf, TreeInstance tree, boolean accent) {

        int albedo = tree.getTreeHandle().getPartAlbedos()[
                accent ? EngineSetting.TREE_PART_ACCENT : EngineSetting.TREE_PART_LEAF];
        float shade = ParticleUtility.roll(random, RuntimeSetting.LEAF_SHADE_MIN, RuntimeSetting.LEAF_SHADE_MAX)
                * RuntimeSetting.LEAF_COLOR_SCALE;
        float dry = random.nextDouble() < RuntimeSetting.LEAF_DRY_SHARE ? RuntimeSetting.LEAF_DRY_MIX : 0f;

        leaves.setValue(leaf, RuntimeSetting.LEAF_RED,
                PackedColorUtility.red(albedo) * shade * (1f - dry) + RuntimeSetting.LEAF_DRY_RED * dry);
        leaves.setValue(leaf, RuntimeSetting.LEAF_GREEN,
                PackedColorUtility.green(albedo) * shade * (1f - dry) + RuntimeSetting.LEAF_DRY_GREEN * dry);
        leaves.setValue(leaf, RuntimeSetting.LEAF_BLUE,
                PackedColorUtility.blue(albedo) * shade * (1f - dry) + RuntimeSetting.LEAF_DRY_BLUE * dry);
    }

    // Draw \\

    // Every live leaf laid as an instance in the frame of the player's chunk: rising in as it is torn loose, settling
    // flat as it comes to rest and fading at the end of its rest
    private void lay(WorldHandle world, double originX, double originZ) {

        for (int leaf = 0; leaf < leaves.getCount(); leaf++) {

            float age = leaves.getValue(leaf, RuntimeSetting.LEAF_AGE);
            float rest = leaves.getValue(leaf, RuntimeSetting.LEAF_REST);
            boolean resting = rest >= 0f;

            instance[0] = ParticleUtility.toFrameX(world, leaves.getX(leaf), originX);
            instance[1] = (float) leaves.getY(leaf);
            instance[2] = ParticleUtility.toFrameZ(world, leaves.getZ(leaf), originZ);
            instance[3] = age;
            instance[4] = leaves.getValue(leaf, RuntimeSetting.LEAF_RED);
            instance[5] = leaves.getValue(leaf, RuntimeSetting.LEAF_GREEN);
            instance[6] = leaves.getValue(leaf, RuntimeSetting.LEAF_BLUE);
            instance[7] = leaves.getValue(leaf, RuntimeSetting.LEAF_SEED);
            instance[8] = resting ? Math.min(1f, rest / RuntimeSetting.LEAF_SETTLE_SECONDS) : 0f;
            instance[9] = Math.min(
                    Math.min(1f, age / RuntimeSetting.LEAF_FADE_IN_SECONDS),
                    resting
                            ? Math.min(1f, (RuntimeSetting.LEAF_REST_SECONDS - rest)
                                    / RuntimeSetting.LEAF_FADE_OUT_SECONDS)
                            : 1f);
            instance[10] = leaves.getValue(leaf, RuntimeSetting.LEAF_SIZE);
            instance[11] = leaves.getValue(leaf, RuntimeSetting.LEAF_SPIN);
            buffer.addInstance(instance);
        }
    }
}
