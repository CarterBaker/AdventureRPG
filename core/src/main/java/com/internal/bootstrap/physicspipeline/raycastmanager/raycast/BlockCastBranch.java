package com.internal.bootstrap.physicspipeline.raycastmanager.raycast;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import com.internal.bootstrap.physicspipeline.util.BlockCastStruct;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.Extras.Coordinate3Int;
import com.internal.core.util.mathematics.Extras.Direction3Vector;
import com.internal.core.util.mathematics.vectors.Vector3;

public class BlockCastBranch extends BranchPackage {

    // Internal
    private ChunkStreamManager chunkStreamManager;
    private BlockManager blockManager;

    private int CHUNK_SIZE;

    // Internal \\

    @Override
    protected void create() {
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Cast \\

    public void cast(
            long chunkCoordinate,
            Vector3 rayOrigin,
            Vector3 direction,
            float maxDistance,
            BlockCastStruct out) {

        out.hit = false;

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
                blockX += CHUNK_SIZE;
            } else if (blockX >= CHUNK_SIZE) {
                chunkX++;
                blockX -= CHUNK_SIZE;
            }

            if (blockZ < 0) {
                chunkZ--;
                blockZ += CHUNK_SIZE;
            } else if (blockZ >= CHUNK_SIZE) {
                chunkZ++;
                blockZ -= CHUNK_SIZE;
            }

            int subChunkY = blockColumnY / CHUNK_SIZE;
            int localY = blockColumnY % CHUNK_SIZE;

            long currentChunkCoord = Coordinate2Long.pack(chunkX, chunkZ);
            ChunkInstance chunk = chunkStreamManager.getChunkInstance(currentChunkCoord);
            if (chunk == null)
                continue;

            SubChunkInstance subChunk = chunk.getSubChunk(subChunkY);
            if (subChunk == null)
                continue;

            int localCoord = Coordinate3Int.pack(blockX, localY, blockZ);
            short blockID = subChunk.getBlockPaletteHandle().getBlock(localCoord);
            BlockHandle block = blockManager.getBlockFromBlockID(blockID);

            if (block != null && block.getGeometry() != DynamicGeometryType.NONE) {

                // Hit point in same space as rayOrigin (chunk-local to start chunk)
                float hitX = rayOrigin.x + direction.x * t;
                float hitY = rayOrigin.y + direction.y * t;
                float hitZ = rayOrigin.z + direction.z * t;

                // Block origin in same space — relative to start chunk, not absolute world
                float relBlockX = (chunkX - startChunkX) * CHUNK_SIZE + blockX;
                float relBlockZ = (chunkZ - startChunkZ) * CHUNK_SIZE + blockZ;

                int SVR = EngineSetting.SUB_VOXEL_RESOLUTION;

                out.hitSubX = Math.max(0, Math.min(SVR - 1, (int) ((hitX - relBlockX) * SVR)));
                out.hitSubY = Math.max(0, Math.min(SVR - 1, (int) ((hitY - blockColumnY) * SVR)));
                out.hitSubZ = Math.max(0, Math.min(SVR - 1, (int) ((hitZ - relBlockZ) * SVR)));

                out.hit = true;
                out.chunkCoordinate = currentChunkCoord;
                out.blockX = blockX;
                out.blockY = localY;
                out.blockZ = blockZ;
                out.subChunkY = subChunkY;
                out.hitFace = lastFace;
                out.distance = t;
                out.block = block;
                return;
            }
        }
    }
}