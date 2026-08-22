package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.physicspipeline.util.BlockCompositionStruct;
import application.bootstrap.physicspipeline.worlddistortionsystem.WorldDistortionSystem;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

public class BlockCollisionBranch extends BranchPackage {

    /*
     * Tests the entity's block composition against adjacent blocks each frame and
     * zeros out any movement axis where a solid block collision is detected.
     * Each axis is tested independently so sliding along walls works correctly.
     * A natural block's collision volume is offset by the exact same
     * WorldDistortionManager lattice cell its geometry is rendered displaced
     * by, so a player never floats above or clips into the visual wobble
     * applied to organic terrain.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private WorldDistortionSystem worldDistortionSystem;

    // Settings
    private int chunkSize;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.worldDistortionSystem = get(WorldDistortionSystem.class);
    }

    // Collision \\

    void calculate(Vector3 position, Vector3 movement, EntityInstance entity) {

        entity.updateBlockComposition();

        BlockCompositionStruct blockCompositionStruct = entity.getBlockCompositionStruct();
        Vector3 size = entity.getSize();

        if (movement.x != 0) {
            Direction3Vector direction = Direction3Vector.getDirectionX((int) Math.signum(movement.x));
            if (hasCollisionInDirection(
                    blockCompositionStruct, direction,
                    position.x, size.x, movement.x))
                movement.x = 0;
        }

        if (movement.y != 0) {
            Direction3Vector direction = Direction3Vector.getDirectionY((int) Math.signum(movement.y));
            if (hasCollisionInDirection(
                    blockCompositionStruct, direction,
                    position.y, size.y, movement.y))
                movement.y = 0;
        }

        if (movement.z != 0) {
            Direction3Vector direction = Direction3Vector.getDirectionZ((int) Math.signum(movement.z));
            if (hasCollisionInDirection(
                    blockCompositionStruct, direction,
                    position.z, size.z, movement.z))
                movement.z = 0;
        }
    }

    private boolean hasCollisionInDirection(
            BlockCompositionStruct blockCompositionStruct,
            Direction3Vector direction,
            float axisPosition,
            float axisSize,
            float axisMovement) {

        Int2LongOpenHashMap adjacentBlocks = blockCompositionStruct.getAllBlocksForSide(direction);

        for (Int2LongMap.Entry entry : adjacentBlocks.int2LongEntrySet()) {

            int blockCoordinate = entry.getIntKey();
            long chunkCoordinate = entry.getLongValue();

            ChunkInstance chunk = worldStreamManager.getChunkInstance(chunkCoordinate);

            if (chunk == null)
                continue;

            int localX = Coordinate3Int.unpackX(blockCoordinate);
            int localY = Coordinate3Int.unpackY(blockCoordinate);
            int localZ = Coordinate3Int.unpackZ(blockCoordinate);

            int subChunkY = localY / chunkSize;
            int subLocalY = localY % chunkSize;

            SubChunkInstance subChunk = chunk.getSubChunk(subChunkY);

            if (subChunk == null)
                continue;

            int localCoordinate = Coordinate3Int.pack(localX, subLocalY, localZ);
            short blockID = subChunk.getBlockPaletteHandle().getBlock(localCoordinate);
            BlockHandle block = blockManager.getBlockHandleFromBlockID(blockID);

            float distortion = resolveNaturalDistortion(block, direction, localX, localZ);

            if (isColliding(block, direction, axisPosition, axisSize, axisMovement, distortion))
                return true;
        }

        return false;
    }

    // Distortion \\

    /*
     * Zero for any non-natural block — those never render displaced, so
     * they never collide displaced either. For a natural block, resolves to
     * whichever single axis component this direction actually tests, since
     * calculate() only ever tests one axis at a time.
     */
    private float resolveNaturalDistortion(BlockHandle block, Direction3Vector direction, int localX, int localZ) {

        if (block == null || !block.isNatural())
            return 0f;

        Vector3 offset = worldDistortionSystem.getDistortionOffset(localX, localZ);

        if (direction.x != 0)
            return offset.x;

        if (direction.y != 0)
            return offset.y;

        return offset.z;
    }

    private boolean isColliding(
            BlockHandle block,
            Direction3Vector direction,
            float axisPosition,
            float axisSize,
            float axisMovement,
            float distortion) {

        if (block == null
                || block.getGeometry() == DynamicGeometryType.NONE
                || block.getGeometry() == DynamicGeometryType.LIQUID)
            return false;

        float fractional = axisPosition - (int) axisPosition;

        if (direction.negative)
            return fractional + axisMovement <= distortion;

        return fractional + axisSize + axisMovement >= 1f + distortion;
    }
}