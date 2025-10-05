package com.AdventureRPG.WorldSystem.BatchSystem;

import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.SettingsSystem.GlobalConstant;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.Util.MeshPacket;
import com.badlogic.gdx.math.Vector3;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class MegaChunk {

    // Game Manager
    private final BatchSystem batchSystem;

    // Mega
    public final long megaCoordinate;
    public final int megaX, megaY;

    // Position
    private final int offsetX, offsetY;
    private final Vector3 currentPosition;
    private final Vector2Int chunkCoordinate;
    private final Vector3 renderPosition;

    // Chunk Tracking
    private final int totalChunks;
    private Long2ObjectOpenHashMap<Chunk> combinedChunks;

    // Data
    private final MeshPacket megaPacket;

    // Base \\

    public MegaChunk(
            BatchSystem batchSystem,
            PlayerSystem playerSystem,
            long megaCoordinate,
            int megaX, int megaY) {

        // Game Manager
        this.batchSystem = batchSystem;

        // Mega
        this.megaCoordinate = megaCoordinate;
        this.megaX = megaX;
        this.megaY = megaY;

        // Position
        this.offsetX = megaX * GlobalConstant.MEGA_CHUNK_SIZE;
        this.offsetY = megaY * GlobalConstant.MEGA_CHUNK_SIZE;
        this.currentPosition = playerSystem.currentPosition();
        this.chunkCoordinate = playerSystem.chunkCoordinate();
        this.renderPosition = new Vector3();

        // Chunk Tracking
        int size = GlobalConstant.MEGA_CHUNK_SIZE;
        this.totalChunks = size * size;
        this.combinedChunks = new Long2ObjectOpenHashMap<>(totalChunks);

        // Data
        this.megaPacket = new MeshPacket();
    }

    public void render() {

        calculateRenderPosition();
        renderMega();
    }

    // Position \\

    private void calculateRenderPosition() {

        float aX = (offsetX - chunkCoordinate.x) * GlobalConstant.CHUNK_SIZE;
        float aY = (offsetY - chunkCoordinate.y) * GlobalConstant.CHUNK_SIZE;

        float bX = aX + currentPosition.x;
        float bY = currentPosition.y;
        float bZ = aY + currentPosition.z;

        renderPosition.x = bX;
        renderPosition.y = bY;
        renderPosition.z = bZ;
    }

    // Render \\

    private void renderMega() {

    }

    // Batching \\

    public void addChunk(Chunk chunk) {

        if (!isCompatibleChunk(chunk))
            return;

        combinedChunks.putIfAbsent(chunk.coordinate, chunk);

        MeshPacket other = chunk.meshPacket();
        megaPacket.merge(other);

        if (combinedChunks.size() == totalChunks)
            combinedChunks();
    }

    public void removeChunk(Chunk chunk) {

        if (!isCompatibleChunk(chunk))
            return;

        combinedChunks.remove(chunk.coordinate);

        if (combinedChunks.size() < 1)
            batchSystem.removeMegaChunk(megaCoordinate);
    }

    public void assessChunk(Chunk chunk) {

        if (!isCompatibleChunk(chunk))
            return;

        if (!combinedChunks.containsKey(chunk.coordinate))
            addChunk(chunk);
    }

    // Data \\

    private void combinedChunks() {

        megaPacket.clear();

        for (Long2ObjectMap.Entry<Chunk> entry : combinedChunks.long2ObjectEntrySet()) {

            Chunk chunk = entry.getValue();

            MeshPacket other = chunk.meshPacket();
            megaPacket.merge(other);
        }
    }

    // Utility \\

    private boolean isCompatibleChunk(Chunk chunk) {

        if (chunk == null)
            return false;

        // Check if the chunk is valid for the current mega chunk
        int coordinateX = chunk.coordinateX / GlobalConstant.MEGA_CHUNK_SIZE;
        int coordinateY = chunk.coordinateY / GlobalConstant.MEGA_CHUNK_SIZE;

        if (coordinateX != megaX || coordinateY != megaY)
            return false;

        return true;
    }
}
