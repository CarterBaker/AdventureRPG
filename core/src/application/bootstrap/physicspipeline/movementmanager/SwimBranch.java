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
     * Owns the vertical axis and the horizontal drag multiplier whenever an
     * entity's feet are inside a LIQUID block. Water no longer collides like
     * a solid (see BlockCollisionBranch), so this is what keeps an entity
     * from sinking straight through the moment it touches one.
     *
     * refresh() runs once per move(), before anything else touches the Y
     * axis. It samples the single block-space column under the entity's
     * feet — the same single-column convention WorldPositionUtility uses for
     * spawn safety — and, if liquid, resolves both the local surface height
     * and this liquid's drag/viscosity for the whole frame. Everything
     * downstream (isSwimming(), calculate(), attemptClimbOut(),
     * MovementManager's wading flag, MovementBranch's speed multiplier)
     * reads off those cached results rather than re-querying the world; this
     * is the same scratch-field pattern FluidSimulationSystem uses for its
     * own neighbor resolution.
     *
     * isSwimming() is the gate between two very different feels:
     * - Not enough to completely submerge the entity (surface height below
     * SWIM_FULL_SUBMERGE_FRACTION of its own size.y): WADING. calculate()
     * never runs; gravity keeps ownership of the Y axis (see
     * MovementManager/GravityBranch) and collision naturally rests the
     * entity on the floor beneath the liquid, same as dry ground. Only the
     * drag multiplier and a diminished jump apply — see
     * EngineSetting.WADE_JUMP_HEIGHT_MULTIPLIER — so wading through a
     * shallow pond feels like wading, not swimming, and never traps the
     * entity the way full swim control used to over a shallow floor.
     * - Deep enough to fully submerge: calculate() takes over. Fully
     * submerged (eye more than SWIM_DEEP_THRESHOLD below the surface)
     * sinks gently unless jump is held, in which case it swims up at
     * SWIM_UP_SPEED. Once close enough to breathe, a proportional
     * controller settles the entity so its eye sits SWIM_HEAD_CLEARANCE
     * above the surface — this is what treading water looks like, and
     * also what gives first person its "crawling across the surface"
     * feel, since the eye deliberately settles low, right at the water
     * plane, instead of standing eye height.
     *
     * attemptClimbOut() runs once per move(), right after collision, only
     * while genuinely swimming. It is what lets an entity actually leave a
     * body of water it can't touch bottom in — see its own doc comment.
     *
     * Every speed above is scaled by getSpeedMultiplier(), derived from
     * BlockHandle.getViscosity() of the touched liquid — thin fluid barely
     * registers, thick fluid drags hard, with a floor so even the thickest
     * fluid still allows some control.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;

    // Settings
    private int chunkSize;

    // Per-frame result — written by refresh(), read by isSwimming(),
    // calculate(), attemptClimbOut(), and MovementManager (via
    // getSpeedMultiplier()) for the rest of this entity's move() call.
    private boolean submerged;
    private float speedMultiplier;
    private float liquidViscosity;
    private float surfaceY;
    private float feetY;

    // Per-frame result — the entity's own column, cached by refresh() so
    // attemptClimbOut() doesn't have to re-resolve the chunk/coordinates it
    // already looked up moments earlier this same move().
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

        this.submerged = false;
        this.speedMultiplier = 1f;
        this.liquidViscosity = EngineSetting.SWIM_VISCOSITY_REFERENCE;
        this.surfaceY = LiquidColumnUtility.NO_SURFACE;
        this.currentChunk = null;

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        long chunkCoordinate = entity.getWorldPositionStruct().getChunkCoordinate();

        this.feetY = position.y;

        ChunkInstance chunk = worldStreamManager.getChunkInstance(chunkCoordinate);

        if (chunk == null)
            return false;

        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        int feetTotalY = (int) Math.floor(position.y);

        BlockHandle touched = LiquidColumnUtility.getBlockAt(chunk, blockManager, blockX, feetTotalY, blockZ);

        if (!LiquidColumnUtility.isLiquid(touched))
            return false;

        this.submerged = true;
        this.liquidViscosity = touched.hasViscosity() ? touched.getViscosity() : EngineSetting.SWIM_VISCOSITY_REFERENCE;
        this.speedMultiplier = calculateSpeedMultiplier(liquidViscosity);
        this.surfaceY = LiquidColumnUtility.findSurfaceHeight(chunk, blockManager, blockX, feetTotalY, blockZ);

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

    /*
     * True only when there's enough liquid above the feet to fully cover the
     * entity — see EngineSetting.SWIM_FULL_SUBMERGE_FRACTION. Anything
     * shallower than that is wading, not swimming (see MovementManager),
     * even though the feet are still touching liquid.
     */
    boolean isSwimming(EntityInstance entity) {

        if (!submerged || Float.isNaN(surfaceY))
            return false;

        float fullSubmergeDepth = entity.getSize().y * EngineSetting.SWIM_FULL_SUBMERGE_FRACTION;

        return (surfaceY - feetY) >= fullSubmergeDepth;
    }

    float getSpeedMultiplier() {
        return speedMultiplier;
    }

    // Vertical \\

    void calculate(Vector3 movement, EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        EntityInputHandle input = entity.getEntityInputHandle();
        Vector3 position = entity.getWorldPositionStruct().getPosition();
        Vector3 vertical = state.getGravityVelocity();
        float delta = internal.getDeltaTime();

        state.setMovementState(EntityState.SWIMMING);

        float eyeY = position.y + entity.getEyeHeight();
        float gapAboveEye = surfaceY - eyeY;

        if (gapAboveEye > EngineSetting.SWIM_DEEP_THRESHOLD) {
            // Genuinely underwater — sink gently, or swim up while jump is held.
            vertical.y = input.isJump()
                    ? EngineSetting.SWIM_UP_SPEED * speedMultiplier
                    : -EngineSetting.SWIM_SINK_SPEED * speedMultiplier;
        } else {
            // Near/at the surface — tread so the eye clears it by SWIM_HEAD_CLEARANCE.
            float targetY = surfaceY + EngineSetting.SWIM_HEAD_CLEARANCE - entity.getEyeHeight();
            float diff = targetY - position.y;
            float maxStep = EngineSetting.SWIM_TREAD_SPEED * speedMultiplier;
            vertical.y = Math.max(-maxStep, Math.min(maxStep, diff * EngineSetting.SWIM_TREAD_RESPONSIVENESS));
        }

        movement.y += vertical.y * delta;
    }

    // Climb Out \\

    /*
     * Called once per move(), right after collision, only while genuinely
     * swimming (see isSwimming()). Deep water on its own never lets an
     * entity climb back out — calculate() only ever tries to keep the eye
     * at the surface, it never lifts the entity onto dry land — so without
     * this an entity that can't touch bottom stays swimming forever against
     * any bank, however low.
     *
     * If this frame's horizontal movement was just blocked by a solid
     * ledge (on either axis — both are tried in case a corner blocked
     * both at once), the adjacent column is scanned for dry ground. A
     * ledge only counts as climbable if its standing surface is at or
     * below this liquid's own surface height, rounded up to the
     * containing block — level with, or lower than, the entity's back
     * while treading water. A ledge a full block above the surface is out
     * of reach this way; the entity stays swimming and has to find another
     * way out — that's intentional, the same as a real swimmer being
     * unable to simply float up onto a ledge above their own shoulders.
     *
     * Gated on this liquid's own viscosity via SWIM_CLIMB_OUT_MAX_VISCOSITY
     * — anything thicker than plain water never allows a climb-out at all,
     * regardless of ledge height.
     */
    boolean attemptClimbOut(Vector3 preCollision, Vector3 postCollision, EntityInstance entity) {

        if (!submerged || Float.isNaN(surfaceY) || currentChunk == null)
            return false;

        if (liquidViscosity > EngineSetting.SWIM_CLIMB_OUT_MAX_VISCOSITY)
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

        float standingY = findClimbOutHeight(neighborChunk, neighborBlockX, neighborBlockZ, entity.getSize().y);

        if (Float.isNaN(standingY) || standingY > Math.ceil(surfaceY))
            return false;

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        postCollision.y = standingY - position.y;

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

    /*
     * Scans the adjacent column downward from this liquid's own surface
     * layer for the first dry ground with clear headroom above it. Bails
     * out the moment it finds more of the same liquid still that deep — a
     * ledge only counts if it actually breaks the surface near the top of
     * the scan, not a submerged shelf a few blocks further down.
     */
    private float findClimbOutHeight(ChunkInstance chunk, int blockX, int blockZ, float entityHeight) {

        int scanTop = (int) Math.ceil(surfaceY);
        int scanBottom = Math.max(0, scanTop - EngineSetting.SWIM_CLIMB_OUT_SCAN_DEPTH);
        int clearanceBlocks = Math.max(1, (int) Math.ceil(entityHeight));

        for (int y = scanTop; y >= scanBottom; y--) {

            BlockHandle block = LiquidColumnUtility.getBlockAt(chunk, blockManager, blockX, y, blockZ);

            if (block == null)
                return Float.NaN;

            if (block.getGeometry() == DynamicGeometryType.NONE)
                continue;

            if (LiquidColumnUtility.isLiquid(block))
                return Float.NaN;

            return hasClearance(chunk, blockX, blockZ, y + 1, clearanceBlocks) ? y + 1 : Float.NaN;
        }

        return Float.NaN;
    }

    private boolean hasClearance(ChunkInstance chunk, int blockX, int blockZ, int fromY, int blockCount) {

        for (int i = 0; i < blockCount; i++) {

            BlockHandle block = LiquidColumnUtility.getBlockAt(chunk, blockManager, blockX, fromY + i, blockZ);

            if (block == null || block.getGeometry() != DynamicGeometryType.NONE)
                return false;
        }

        return true;
    }
}