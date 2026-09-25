package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.util.EntityInputHandle;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.util.LiquidColumnUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;

public class SwimBranch extends BranchPackage {

    /*
     * Owns every liquid interaction for an entity. refresh() samples the column under the entity's feet once per
     * move() and caches the surface height, the water depth down to the floor, and the viscosity drag for the rest
     * of the frame. Depth measured against the entity's own height drives everything else: wading drag and the
     * running penalty, the depth-scaled jump nerf with its minimum, the running entry leap, the switch to swimming
     * once the water reaches SWIM_DEPTH_FRACTION of the entity's height, the surface leap, and climbing out onto
     * any ledge or shelf within reach of the surface.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;

    // Settings
    private int chunkSize;

    // Frame
    private boolean submerged;
    private boolean entering;
    private float speedMultiplier;
    private float liquidViscosity;
    private float surfaceY;
    private float depth;
    private float depthFactor;

    // Column
    private ChunkInstance currentChunk;
    private int currentBlockX;
    private int currentBlockZ;

    // Internal \\

    @Override
    protected void create() {
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Refresh \\

    boolean refresh(EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        boolean wasInLiquid = state.isInLiquid();

        this.submerged = resolveLiquid(entity);
        this.entering = submerged && !wasInLiquid;
        state.setInLiquid(submerged);

        return submerged;
    }

    private boolean resolveLiquid(EntityInstance entity) {

        this.speedMultiplier = 1f;
        this.liquidViscosity = EngineSetting.SWIM_VISCOSITY_REFERENCE;
        this.surfaceY = LiquidColumnUtility.NO_SURFACE;
        this.depth = 0f;
        this.depthFactor = 0f;
        this.currentChunk = null;

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        long chunkCoordinate = entity.getWorldPositionStruct().getChunkCoordinate();
        ChunkInstance chunk = worldStreamManager.getChunkInstance(chunkCoordinate);

        if (chunk == null)
            return false;

        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        int feetTotalY = (int) Math.floor(position.y);

        BlockHandle touched = LiquidColumnUtility.getBlockAt(chunk, blockManager, blockX, feetTotalY, blockZ);

        if (!LiquidColumnUtility.isLiquid(touched))
            return false;

        float entityHeight = entity.getSize().y;
        float swimDepth = entityHeight * EngineSetting.SWIM_DEPTH_FRACTION;

        this.liquidViscosity = touched.hasViscosity() ? touched.getViscosity() : EngineSetting.SWIM_VISCOSITY_REFERENCE;
        this.speedMultiplier = calculateSpeedMultiplier(liquidViscosity);
        this.surfaceY = LiquidColumnUtility.findSurfaceHeight(chunk, blockManager, blockX, feetTotalY, blockZ);

        int floorLimit = (int) Math.floor(surfaceY - entityHeight);
        float floorY = LiquidColumnUtility.findFloorHeight(
                chunk,
                blockManager,
                blockX,
                feetTotalY,
                blockZ,
                floorLimit);

        this.depth = Math.max(0f, surfaceY - floorY);
        this.depthFactor = Math.min(1f, depth / swimDepth);

        this.currentChunk = chunk;
        this.currentBlockX = blockX;
        this.currentBlockZ = blockZ;

        return true;
    }

    private float calculateSpeedMultiplier(float viscosity) {

        float overage = Math.max(0f, viscosity - EngineSetting.SWIM_VISCOSITY_REFERENCE);
        float multiplier = 1f / (1f + EngineSetting.SWIM_VISCOSITY_DRAG_SCALE * overage);

        return Math.max(EngineSetting.SWIM_MIN_SPEED_MULTIPLIER, Math.min(1f, multiplier));
    }

    // Accessible \\

    boolean isSwimming() {
        return submerged && depthFactor >= 1f;
    }

    float getSpeedMultiplier(EntityInstance entity, boolean swimming) {

        if (swimming)
            return speedMultiplier;

        return speedMultiplier * calculateWadeMultiplier(entity);
    }

    private float calculateWadeMultiplier(EntityInstance entity) {

        float depthDrag = 1f - depthFactor * (1f - EngineSetting.WADE_DEEP_SPEED_MULTIPLIER);

        if (entity.getEntityStateHandle().getMovementState() != EntityState.RUNNING)
            return depthDrag;

        return depthDrag * (1f - depthFactor * (1f - EngineSetting.WADE_RUN_SPEED_MULTIPLIER));
    }

    // Jump \\

    float resolveJumpHeight(EntityInstance entity) {

        float jumpHeight = entity.getStatisticsHandle().getJumpHeight();

        if (!submerged)
            return jumpHeight;

        float nerfedHeight = jumpHeight * (1f - depthFactor * (1f - EngineSetting.WATER_JUMP_DEEP_MULTIPLIER));
        float minimumHeight = Math.min(jumpHeight, EngineSetting.WATER_JUMP_MIN_HEIGHT);

        return Math.max(nerfedHeight, minimumHeight);
    }

    boolean isEntryLeap(EntityInstance entity) {

        if (!entering || depthFactor < EngineSetting.WATER_ENTRY_LEAP_MIN_DEPTH_FACTOR)
            return false;

        EntityStateHandle state = entity.getEntityStateHandle();

        return state.getMovementState() == EntityState.RUNNING
                && state.getGravityVelocity().y > -EngineSetting.WATER_ENTRY_LEAP_MAX_FALL_SPEED;
    }

    float getEntryLeapHeight(EntityInstance entity) {
        return entity.getStatisticsHandle().getJumpHeight() * EngineSetting.WATER_ENTRY_LEAP_MULTIPLIER * depthFactor;
    }

    boolean isSurfaceLeap(EntityInstance entity) {

        if (!isSwimming() || !entity.getEntityInputHandle().isJump())
            return false;

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        float restY = Math.max(calculateTreadHeight(entity), surfaceY - depth);

        return calculateGapAboveEye(entity) <= EngineSetting.SWIM_DEEP_THRESHOLD
                && position.y <= restY + EngineSetting.SWIM_LEAP_REST_TOLERANCE;
    }

    // Vertical \\

    void calculate(Vector3 movement, EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        EntityInputHandle input = entity.getEntityInputHandle();
        Vector3 position = entity.getWorldPositionStruct().getPosition();
        Vector3 vertical = state.getGravityVelocity();
        float delta = internal.getDeltaTime();

        state.setMovementState(EntityState.SWIMMING);

        if (calculateGapAboveEye(entity) > EngineSetting.SWIM_DEEP_THRESHOLD) {

            // Underwater — sink gently, or swim up while jump is held
            vertical.y = input.isJump()
                    ? EngineSetting.SWIM_UP_SPEED * speedMultiplier
                    : -EngineSetting.SWIM_SINK_SPEED * speedMultiplier;
        } else {

            // Surface — tread so the eye clears the water by SWIM_HEAD_CLEARANCE
            float diff = calculateTreadHeight(entity) - position.y;
            float maxStep = EngineSetting.SWIM_TREAD_SPEED * speedMultiplier;
            vertical.y = Math.max(-maxStep, Math.min(maxStep, diff * EngineSetting.SWIM_TREAD_RESPONSIVENESS));
        }

        movement.y += vertical.y * delta;
    }

    private float calculateTreadHeight(EntityInstance entity) {
        return surfaceY + EngineSetting.SWIM_HEAD_CLEARANCE - entity.getEyeHeight();
    }

    private float calculateGapAboveEye(EntityInstance entity) {
        return surfaceY - (entity.getWorldPositionStruct().getPosition().y + entity.getEyeHeight());
    }

    // Climb Out \\

    boolean attemptClimbOut(
            Vector3 preCollision,
            Vector3 postCollision,
            EntityInstance entity,
            boolean swimming) {

        if (!submerged || currentChunk == null)
            return false;

        if (liquidViscosity > EngineSetting.SWIM_CLIMB_OUT_MAX_VISCOSITY)
            return false;

        if (!swimming && !entity.getEntityInputHandle().isJump())
            return false;

        if (preCollision.x != 0f && postCollision.x == 0f) {
            Direction3Vector direction = Direction3Vector.getDirectionX((int) Math.signum(preCollision.x));
            if (tryClimbOut(direction, postCollision, entity))
                return true;
        }

        if (preCollision.z != 0f && postCollision.z == 0f) {
            Direction3Vector direction = Direction3Vector.getDirectionZ((int) Math.signum(preCollision.z));
            if (tryClimbOut(direction, postCollision, entity))
                return true;
        }

        return false;
    }

    private boolean tryClimbOut(Direction3Vector direction, Vector3 postCollision, EntityInstance entity) {

        ChunkInstance neighborChunk = resolveNeighborChunk(currentChunk, currentBlockX, currentBlockZ, direction);

        if (neighborChunk == null)
            return false;

        int neighborBlockX = wrapBlockCoordinate(currentBlockX + direction.x);
        int neighborBlockZ = wrapBlockCoordinate(currentBlockZ + direction.z);

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        float entityHeight = entity.getSize().y;
        float reachY = surfaceY + entityHeight * EngineSetting.SWIM_CLIMB_OUT_HEIGHT_FRACTION;
        float standingY = findClimbOutHeight(
                neighborChunk,
                neighborBlockX,
                neighborBlockZ,
                (int) Math.floor(position.y),
                (int) Math.floor(reachY),
                entityHeight);

        if (Float.isNaN(standingY) || standingY > reachY)
            return false;

        postCollision.y = standingY + EngineSetting.SWIM_CLIMB_OUT_LIFT - position.y;

        EntityStateHandle state = entity.getEntityStateHandle();
        state.getGravityVelocity().set(0, 0, 0);
        state.setMovementState(EntityState.IDLE);

        return true;
    }

    private ChunkInstance resolveNeighborChunk(
            ChunkInstance chunk,
            int blockX,
            int blockZ,
            Direction3Vector direction) {

        boolean crossesEdge = (direction.x < 0 && blockX == 0) || (direction.x > 0 && blockX == chunkSize - 1)
                || (direction.z < 0 && blockZ == 0) || (direction.z > 0 && blockZ == chunkSize - 1);

        if (!crossesEdge)
            return chunk;

        return chunk.getChunkNeighbors().getNeighborChunk(direction.to2D().index);
    }

    private int wrapBlockCoordinate(int value) {

        if (value < 0)
            return value + chunkSize;

        if (value >= chunkSize)
            return value - chunkSize;

        return value;
    }

    private float findClimbOutHeight(
            ChunkInstance chunk,
            int blockX,
            int blockZ,
            int feetTotalY,
            int scanTop,
            float entityHeight) {

        int scanBottom = Math.max(Math.max(0, feetTotalY), scanTop - EngineSetting.SWIM_CLIMB_OUT_SCAN_DEPTH);
        int clearanceBlocks = Math.max(1, (int) Math.ceil(entityHeight));

        for (int y = scanTop; y >= scanBottom; y--) {

            BlockHandle block = LiquidColumnUtility.getBlockAt(chunk, blockManager, blockX, y, blockZ);

            if (block == null)
                return Float.NaN;

            if (isPassable(block))
                continue;

            return hasClearance(chunk, blockX, blockZ, y + 1, clearanceBlocks) ? y + 1 : Float.NaN;
        }

        return Float.NaN;
    }

    private boolean hasClearance(ChunkInstance chunk, int blockX, int blockZ, int fromY, int blockCount) {

        for (int i = 0; i < blockCount; i++) {

            BlockHandle block = LiquidColumnUtility.getBlockAt(chunk, blockManager, blockX, fromY + i, blockZ);

            if (block == null || !isPassable(block))
                return false;
        }

        return true;
    }

    private boolean isPassable(BlockHandle block) {
        return block.getGeometry() == DynamicGeometryType.NONE || LiquidColumnUtility.isLiquid(block);
    }
}
