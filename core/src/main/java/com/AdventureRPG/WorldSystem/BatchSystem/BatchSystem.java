package com.AdventureRPG.WorldSystem.BatchSystem;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.ThreadManager.ThreadManager;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.MegaChunk.MegaChunk;
import com.AdventureRPG.WorldSystem.MegaChunk.MegaState;
import com.AdventureRPG.WorldSystem.RenderManager.RenderManager;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class BatchSystem {

    // Game Manager
    public final Settings settings;
    public final ThreadManager threadManager;
    public final MaterialManager materialManager;
    public final PlayerSystem playerSystem;
    public final RenderManager renderManager;

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
        this.materialManager = worldSystem.materialManager;
        this.playerSystem = worldSystem.playerSystem;
        this.renderManager = worldSystem.renderManager;

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
        updateLoadedmegas();

        renderManager.update();
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
                addQueue(chunk);
            });
        }

        // Process removals
        while (!removeRequests.isEmpty()) {

            Chunk chunk = removeRequests.poll();

            if (chunk == null)
                continue;

            threadManager.submitGeneral(() -> {
                removalQueue(chunk);
            });
        }

        // Process assessments
        while (!assessRequests.isEmpty()) {

            Chunk chunk = assessRequests.poll();

            if (chunk == null)
                continue;

            threadManager.submitGeneral(() -> {
                assessmentQueue(chunk);
            });
        }
    }

    private void updateLoadedmegas() {

        for (ObjectIterator<MegaChunk> it = loadedMegas.values().iterator(); it.hasNext();) {

            MegaChunk mega = it.next();

            if (mega.state() != MegaState.COMPLETE)
                continue;

            mega.update();
            renderManager.assessMega(mega);
        }
    }

    // Main \\

    private void addQueue(Chunk chunk) {

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
                        megaCoordinate,
                        megaX, megaY);

                loadedMegas.put(megaCoordinate, megaChunk);
            }

            megaChunk.addChunk(chunk);
        }
    }

    private void removalQueue(Chunk chunk) {

        synchronized (megaLock) {

            MegaChunk megaChunk = getMegaChunk(chunk);

            if (megaChunk == null)
                return;

            megaChunk.removeChunk(chunk);
            renderManager.removeMega(megaChunk);
        }
    }

    private void assessmentQueue(Chunk chunk) {

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
