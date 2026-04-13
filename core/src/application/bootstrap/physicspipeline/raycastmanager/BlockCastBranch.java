package application.bootstrap.physicspipeline.raycastmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.settings.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;

class BlockCastBranch extends BranchPackage {

    /*
     * Performs DDA block raycasting from a world position along a direction.
     * Writes hit results into a caller-supplied BlockCastStruct — no allocation
     * per cast. Crosses chunk boundaries transparently during traversal.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;

    // Settings
    private int chunkSize;
    private int subVoxelResolution;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.subVoxelResolution = EngineSetting.SUB_VOXEL_RESOLUTION;
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Cast \\

    void cast(
            long chunkCoordinate,
            Vector3 rayOrigin,
            Vector3 direction,
            float maxDistance,
            BlockCastStruct out) {

        out.setHit(false);

        int startChunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int startChunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        int chunkX = startChunkX;
        int chunkZ = startChunkZ;

        int blockX = (int) Math.floor(rayOrigin.x);
        int blockColumnY = (int) Math.floor(rayOrigin.y);
        int blockZ = (int) Math.floor(rayOrigin.z);

        int stepX = direction.x > 0 ? 1 : (direction.x < 0 ? -1 : 0);
        int stepY = direction.y > 0 ? 1 : (direction.y < 0 ? -1 : 0);
        int stepZ = direction.z > 0 ? 1 : (direction.z < 0 ? -1 : 0);

        float tDeltaX = stepX != 0 ? Math.abs(1f / direction.x) : Float.MAX_VALUE;
        float tDeltaY = stepY != 0 ? Math.abs(1f / direction.y) : Float.MAX_VALUE;
        float tDeltaZ = stepZ != 0 ? Math.abs(1f / direction.z) : Float.MAX_VALUE;

        float fractX = rayOrigin.x - blockX;
        float fractY = rayOrigin.y - blockColumnY;
        float fractZ = rayOrigin.z - blockZ;

        float tMaxX = stepX > 0 ? tDeltaX * (1f - fractX) : stepX < 0 ? tDeltaX * fractX : Float.MAX_VALUE;
        float tMaxY = stepY > 0 ? tDeltaY * (1f - fractY) : stepY < 0 ? tDeltaY * fractY : Float.MAX_VALUE;
        float tMaxZ = stepZ > 0 ? tDeltaZ * (1f - fractZ) : stepZ < 0 ? tDeltaZ * fractZ : Float.MAX_VALUE;

        Direction3Vector lastFace = null;
        float t = 0f;

        while (t < maxDistance) {

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                t = tMaxX;
                if (t >= maxDistance)
                    break;
                blockX += stepX;
                tMaxX += tDeltaX;
                lastFace = Direction3Vector.getDirectionX(-stepX);
            } else if (tMaxY < tMaxZ) {
                t = tMaxY;
                if (t >= maxDistance)
                    break;
                blockColumnY += stepY;
                tMaxY += tDeltaY;
                lastFace = Direction3Vector.getDirectionY(-stepY);
            } else {
                t = tMaxZ;
                if (t >= maxDistance)
                    break;
                blockZ += stepZ;
                tMaxZ += tDeltaZ;
                lastFace = Direction3Vector.getDirectionZ(-stepZ);
            }

            if (blockX < 0) {
                chunkX--;
                blockX += chunkSize;
            } else if (blockX >= chunkSize) {
                chunkX++;
                blockX -= chunkSize;
            }

            if (blockZ < 0) {
                chunkZ--;
                blockZ += chunkSize;
            } else if (blockZ >= chunkSize) {
                chunkZ++;
                blockZ -= chunkSize;
            }

            int subChunkY = blockColumnY / chunkSize;
            int localY = blockColumnY % chunkSize;

            long currentChunkCoord = Coordinate2Long.pack(chunkX, chunkZ);
            ChunkInstance chunk = worldStreamManager.getChunkInstance(currentChunkCoord);

            if (chunk == null)
                continue;

            SubChunkInstance subChunk = chunk.getSubChunk(subChunkY);

            if (subChunk == null)
                continue;

            int localCoord = Coordinate3Int.pack(blockX, localY, blockZ);
            short blockID = subChunk.getBlockPaletteHandle().getBlock(localCoord);
            BlockHandle block = blockManager.getBlockHandleFromBlockID(blockID);

            if (block == null || block.getGeometry() == DynamicGeometryType.NONE)
                continue;

            float hitX = rayOrigin.x + direction.x * t;
            float hitY = rayOrigin.y + direction.y * t;
            float hitZ = rayOrigin.z + direction.z * t;

            float relBlockX = (chunkX - startChunkX) * chunkSize + blockX;
            float relBlockZ = (chunkZ - startChunkZ) * chunkSize + blockZ;

            out.setHitSubX(
                    Math.max(0, Math.min(subVoxelResolution - 1, (int) ((hitX - relBlockX) * subVoxelResolution))));
            out.setHitSubY(
                    Math.max(0, Math.min(subVoxelResolution - 1, (int) ((hitY - blockColumnY) * subVoxelResolution))));
            out.setHitSubZ(
                    Math.max(0, Math.min(subVoxelResolution - 1, (int) ((hitZ - relBlockZ) * subVoxelResolution))));

            out.setHit(true);
            out.setChunkCoordinate(currentChunkCoord);
            out.setBlockX(blockX);
            out.setBlockY(localY);
            out.setBlockZ(blockZ);
            out.setSubChunkY(subChunkY);
            out.setHitFace(lastFace);
            out.setDistance(t);
            out.setBlock(block);
            return;
        }
    }
}