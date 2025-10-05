package com.AdventureRPG.WorldSystem.BatchSystem;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.SettingsSystem.GlobalConstant;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.ThreadManager.ThreadManager;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class BatchSystem {

    // Game Manager
    private final Settings settings;
    private final ThreadManager threadManager;
    private final PlayerSystem playerSystem;

    // Settings
    private int maxRenderDistance;

    // Mega Tracking
    private Long2ObjectOpenHashMap<MegaChunk> loadedMegas;

    // Async Queues
    private final Object megaLock;
    private final Queue<Chunk> addRequests;
    private final Queue<Chunk> removeRequests;
    private final Queue<Chunk> assessRequests;

    // Base \\

    public BatchSystem(WorldSystem worldSystem) {

        // Game Manager
        this.settings = worldSystem.settings;
        this.threadManager = worldSystem.threadManager;
        this.playerSystem = worldSystem.playerSystem;

        // Settings
        this.maxRenderDistance = settings.maxRenderDistance;

        // Mega Tracking
        this.loadedMegas = new Long2ObjectOpenHashMap<>(maxRenderDistance * maxRenderDistance);

        // Async Queues
        this.megaLock = new Object();
        this.addRequests = new ConcurrentLinkedQueue<>();
        this.removeRequests = new ConcurrentLinkedQueue<>();
        this.assessRequests = new ConcurrentLinkedQueue<>();
    }

    public void awake() {

    }

    public void start() {

    }

    public void update() {

        processAsyncRequests();
    }

    public void render() {

    }

    // Update \\

    private void processAsyncRequests() {

        // Process adds
        while (!addRequests.isEmpty()) {

            Chunk chunk = addRequests.poll();

            if (chunk == null)
                continue;

            threadManager.submitGeneral(() -> {
                addChunkInternal(chunk);
            });
        }

        // Process removals
        while (!removeRequests.isEmpty()) {

            Chunk chunk = removeRequests.poll();

            if (chunk == null)
                continue;

            threadManager.submitGeneral(() -> {
                removeChunkInternal(chunk);
            });
        }

        // Process assessments
        while (!assessRequests.isEmpty()) {

            Chunk chunk = assessRequests.poll();

            if (chunk == null)
                continue;

            threadManager.submitGeneral(() -> {
                assessChunkInternal(chunk);
            });
        }
    }

    // Main \\

    private void addChunkInternal(Chunk chunk) {

        synchronized (megaLock) {

            int chunkX = chunk.coordinateX;
            int chunkY = chunk.coordinateY;

            int megaX = chunkX / GlobalConstant.MEGA_CHUNK_SIZE;
            int megaY = chunkY / GlobalConstant.MEGA_CHUNK_SIZE;
            long megaCoordinate = Coordinate2Int.pack(megaX, megaY);

            MegaChunk megaChunk = loadedMegas.get(megaCoordinate);

            if (megaChunk == null) {

                megaChunk = new MegaChunk(
                        this,
                        playerSystem,
                        megaCoordinate,
                        megaX, megaY);

                loadedMegas.put(megaCoordinate, megaChunk);
            }

            megaChunk.addChunk(chunk);
        }
    }

    private void removeChunkInternal(Chunk chunk) {

        synchronized (megaLock) {

            MegaChunk megaChunk = getMegaChunk(chunk);

            if (megaChunk != null)
                megaChunk.removeChunk(chunk);
        }
    }

    private void assessChunkInternal(Chunk chunk) {

        synchronized (megaLock) {

            MegaChunk megaChunk = getMegaChunk(chunk);

            if (megaChunk != null)
                megaChunk.assessChunk(chunk);
        }
    }

    public void removeMegaChunk(long mega) {

        loadedMegas.remove(mega);
    }

    // Utility \\

    private MegaChunk getMegaChunk(Chunk chunk) {

        int chunkX = chunk.coordinateX;
        int chunkY = chunk.coordinateY;

        int megaX = chunkX / GlobalConstant.MEGA_CHUNK_SIZE;
        int megaY = chunkY / GlobalConstant.MEGA_CHUNK_SIZE;
        long megaCoordinate = Coordinate2Int.pack(megaX, megaY);

        MegaChunk megaChunk = loadedMegas.get(megaCoordinate);

        return megaChunk;
    }

    // Accessible \\

    public void addChunk(Chunk chunk) {

        addRequests.add(chunk);
    }

    public void removeChunk(Chunk chunk) {

        removeRequests.add(chunk);
    }

    public void assessChunk(Chunk chunk) {

        assessRequests.add(chunk);
    }

    public void rebuildGrid() {

        this.maxRenderDistance = settings.maxRenderDistance;
        this.loadedMegas = new Long2ObjectOpenHashMap<>(maxRenderDistance * maxRenderDistance);
    }
}
