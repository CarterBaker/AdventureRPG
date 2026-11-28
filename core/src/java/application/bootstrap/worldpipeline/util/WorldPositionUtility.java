package application.bootstrap.worldpipeline.util;

import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.mathematics.vectors.Vector3;

public class WorldPositionUtility extends EngineUtility {

    /*
     * Spawn helpers: picks a random chunk inside a world and finds the nearest
     * safe standing height in a column. settleChunk() is the one place a
     * chunk-local position that has moved past its chunk's edge is handed to
     * the chunk it now lies in, wrapped around the world. toCanonical() gives
     * a position a grid holds in its chart as the face cell it stands on, its
     * chunk-local position turned into that face's axes, for anything kept
     * past the grid, such as a save.
     */

    public static long getRandomChunk(WorldHandle worldHandle) {

        if (worldHandle.isGlobe()) {

            int faceChunks = GlobeUtility.getFaceChunks(worldHandle);
            int face = ThreadLocalRandom.current().nextInt(0, EngineSetting.GLOBE_FACE_COUNT);

            return Coordinate2Long.pack(
                    (int) GlobeUtility.faceOriginX(face, faceChunks) + ThreadLocalRandom.current().nextInt(0, faceChunks),
                    ThreadLocalRandom.current().nextInt(0, faceChunks));
        }

        Vector2Int scale = worldHandle.getWorldScale();

        int maxX = scale.x / EngineSetting.CHUNK_SIZE;
        int maxY = scale.y / EngineSetting.CHUNK_SIZE;

        int x = ThreadLocalRandom.current().nextInt(0, maxX);
        int y = ThreadLocalRandom.current().nextInt(0, maxY);

        return Coordinate2Long.pack(x, y);
    }

    // Moves a position that left its chunk into the chunk it now lies in, wrapped around the world's edges
    public static void settleChunk(WorldHandle worldHandle, WorldPositionStruct worldPositionStruct) {

        Vector3 position = worldPositionStruct.getPosition();
        int chunkOffsetX = Math.floorDiv((int) Math.floor(position.x), EngineSetting.CHUNK_SIZE);
        int chunkOffsetZ = Math.floorDiv((int) Math.floor(position.z), EngineSetting.CHUNK_SIZE);
        long chunkCoordinate = Coordinate2Long.add(
                worldPositionStruct.getChunkCoordinate(), chunkOffsetX, chunkOffsetZ);

        WorldWrapUtility.wrapAroundChunk(position);
        worldPositionStruct.setChunkCoordinate(WorldWrapUtility.wrapAroundWorld(worldHandle, chunkCoordinate));
    }

    // The canonical form of a position the grid holds in its chart — a torus grid's positions already are
    public static void toCanonical(
            GridInstance grid,
            WorldPositionStruct chartPosition,
            GlobeFrameStruct frameScratch,
            WorldPositionStruct out) {

        grid.resolveFrame(chartPosition.getChunkCoordinate(), frameScratch);

        Vector3 position = chartPosition.getPosition();
        float center = EngineSetting.CHUNK_SIZE * 0.5f;
        float x = position.x - center;
        float z = position.z - center;
        int turns = frameScratch.getTurns();

        out.setChunkCoordinate(frameScratch.getCanonicalCoordinate());
        out.getPosition().set(
                (float) GlobeUtility.turnX(x, z, turns) + center,
                position.y,
                (float) GlobeUtility.turnZ(x, z, turns) + center);
    }

    public static int findSafeSpawnHeight(
            ChunkInstance chunkInstance,
            BlockManager blockManager,
            int blockX,
            int totalY,
            int blockZ) {

        // Check if current position is already safe
        if (totalY >= 0 && totalY < EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE) {

            int subChunkIndex = totalY / EngineSetting.CHUNK_SIZE;
            int localY = totalY % EngineSetting.CHUNK_SIZE;

            SubChunkInstance subChunk = chunkInstance.getSubChunk(subChunkIndex);
            short blockId = subChunk.getBlock(blockX, localY, blockZ);
            BlockHandle block = blockManager.getBlockHandleFromBlockID(blockId);

            if (block.getGeometry() == DynamicGeometryType.NONE && totalY > 0) {

                int belowSubChunk = (totalY - 1) / EngineSetting.CHUNK_SIZE;
                int belowLocalY = (totalY - 1) % EngineSetting.CHUNK_SIZE;

                SubChunkInstance belowChunk = chunkInstance.getSubChunk(belowSubChunk);
                short belowBlockId = belowChunk.getBlock(blockX, belowLocalY, blockZ);
                BlockHandle belowBlock = blockManager.getBlockHandleFromBlockID(belowBlockId);

                if (belowBlock.getGeometry() != DynamicGeometryType.NONE)
                    return totalY;
            }
        }

        // Search from top down
        for (int subChunkIndex = EngineSetting.WORLD_HEIGHT - 1; subChunkIndex >= 0; subChunkIndex--) {
            SubChunkInstance subChunk = chunkInstance.getSubChunk(subChunkIndex);

            for (int localY = EngineSetting.CHUNK_SIZE - 1; localY >= 0; localY--) {
                short blockId = subChunk.getBlock(blockX, localY, blockZ);
                BlockHandle block = blockManager.getBlockHandleFromBlockID(blockId);

                if (block.getGeometry() != DynamicGeometryType.NONE) {
                    int totalYHere = subChunkIndex * EngineSetting.CHUNK_SIZE + localY;

                    if (localY + 1 < EngineSetting.CHUNK_SIZE) {
                        // Above block is in same subchunk
                        short aboveBlockId = subChunk.getBlock(blockX, localY + 1, blockZ);
                        BlockHandle aboveBlock = blockManager.getBlockHandleFromBlockID(aboveBlockId);

                        if (aboveBlock.getGeometry() == DynamicGeometryType.NONE) {
                            return totalYHere + 1;
                        }
                    } else if (subChunkIndex + 1 < EngineSetting.WORLD_HEIGHT) {
                        // Above block is in next subchunk
                        SubChunkInstance aboveChunk = chunkInstance.getSubChunk(subChunkIndex + 1);
                        short aboveBlockId = aboveChunk.getBlock(blockX, 0, blockZ);
                        BlockHandle aboveBlock = blockManager.getBlockHandleFromBlockID(aboveBlockId);

                        if (aboveBlock.getGeometry() == DynamicGeometryType.NONE) {
                            return totalYHere + 1;
                        }
                    }
                }
            }
        }

        return -1;
    }
}
