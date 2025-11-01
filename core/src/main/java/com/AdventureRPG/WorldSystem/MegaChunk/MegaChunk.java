package com.AdventureRPG.WorldSystem.MegaChunk;

import java.util.concurrent.atomic.AtomicBoolean;

import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.WorldSystem.BatchSystem.BatchSystem;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.RenderManager.MeshPacket;
import com.AdventureRPG.WorldSystem.RenderManager.RenderConversion;
import com.AdventureRPG.WorldSystem.RenderManager.RenderPacket;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class MegaChunk {

    // Debug
    private final boolean debug = false; // TODO: Debug line

    // Game Manager
    private final MaterialManager materialManager;
    private final BatchSystem batchSystem;

    // Mega
    public final long megaCoordinate;
    public final int megaX, megaY;

    // Position
    private final int offsetX, offsetY;
    private final Vector3 currentPosition;
    private final Vector2Int chunkCoordinate;
    private final Matrix4 renderPosition;

    // Chunk Tracking
    private final int totalChunks;
    private Long2ObjectOpenHashMap<Chunk> combinedChunks;
    private volatile MegaState state;

    // Data
    private final MeshPacket megaPacket;
    private RenderPacket renderPacket;

    // Multi-Thread
    private final AtomicBoolean threadSafety;

    // Base \\

    public MegaChunk(
            BatchSystem batchSystem,
            long megaCoordinate,
            int megaX, int megaY) {

        // Game Manager
        this.materialManager = batchSystem.materialManager;
        this.batchSystem = batchSystem;

        // Mega
        this.megaCoordinate = megaCoordinate;
        this.megaX = megaX;
        this.megaY = megaY;

        // Position
        this.offsetX = megaX * GlobalConstant.MEGA_CHUNK_SIZE;
        this.offsetY = megaY * GlobalConstant.MEGA_CHUNK_SIZE;
        this.currentPosition = batchSystem.playerSystem.currentPosition();
        this.chunkCoordinate = batchSystem.playerSystem.chunkCoordinate();
        this.renderPosition = new Matrix4();

        // Chunk Tracking
        int size = GlobalConstant.MEGA_CHUNK_SIZE;
        this.totalChunks = size * size;
        this.combinedChunks = new Long2ObjectOpenHashMap<>(totalChunks);
        this.state = MegaState.INCOMPLETE;

        // Data
        this.megaPacket = new MeshPacket();

        // Multi-Thread
        this.threadSafety = new AtomicBoolean(false);
    }

    public void update() {

        if (state != MegaState.COMPLETE)
            return;

        calculateRenderPosition();
    }

    public void dispose() {

        batchSystem.removeMegaChunk(megaCoordinate);
    }

    // Render \\

    private void calculateRenderPosition() {

        float aX = (offsetX - chunkCoordinate.x) * GlobalConstant.CHUNK_SIZE;
        float aY = (offsetY - chunkCoordinate.y) * GlobalConstant.CHUNK_SIZE;

        float bX = aX + currentPosition.x;
        float bY = currentPosition.y;
        float bZ = aY + currentPosition.z;

        renderPosition.idt().translate(bX, bY, bZ);
    }

    // Batching \\

    public void addChunk(Chunk chunk) {

        if (!isCompatibleChunk(chunk))
            return;

        combinedChunks.putIfAbsent(chunk.coordinate, chunk);

        if (combinedChunks.size() != totalChunks)
            return;

        if (threadSafety.compareAndSet(false, true)) {

            synchronized (this) {

                if (state != MegaState.COMPLETE) {

                    state = MegaState.COMPLETE;
                    combineChunks();
                }
            }
        }
    }

    public void removeChunk(Chunk chunk) {

        if (!isCompatibleChunk(chunk))
            return;

        combinedChunks.remove(chunk.coordinate);

        if (combinedChunks.size() > 0)
            return;

        dispose();
    }

    public void assessChunk(Chunk chunk) {

        if (!isCompatibleChunk(chunk))
            return;

        if (!combinedChunks.containsKey(chunk.coordinate))
            addChunk(chunk);
    }

    // Data \\

    private void combineChunks() {

        megaPacket.clear();

        for (Long2ObjectMap.Entry<Chunk> entry : combinedChunks.long2ObjectEntrySet()) {

            Chunk chunk = entry.getValue();

            MeshPacket other = chunk.chunkMesh.getMeshPacket();
            megaPacket.merge(other);
        }

        renderPacket = RenderConversion.convert(megaPacket, materialManager);
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

    // Accessible \\

    public MegaState state() {
        return state;
    }

    public Matrix4 renderPosition() {
        return renderPosition;
    }

    public RenderPacket renderPacket() {
        return renderPacket;
    }

    // Debug \\

    private void debug(String input) {

        System.out.println("[MegaChunk] " + input);
    }
}
