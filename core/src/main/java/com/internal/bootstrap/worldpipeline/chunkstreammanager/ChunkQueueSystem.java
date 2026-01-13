package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.internal.bootstrap.geometrypipeline.buildManager.BuildManager;
import com.internal.bootstrap.threadpipeline.ThreadSystem;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import com.internal.core.engine.SystemPackage;

class ChunkQueueSystem extends SystemPackage {

    // Internal
    private ThreadSystem threadSystem;
    private BuildManager buildManager;
    private WorldGenerationManager worldGenerationManager;

    // Chunk Queue
    private ConcurrentLinkedQueue<Long> loadRequests;
    private ConcurrentLinkedQueue<Long> unloadRequests;
    private ConcurrentLinkedQueue<ChunkInstance> loadedChunks;

    // Internal \\

    @Override
    protected void create() {

        // Stream System
        this.loadRequests = new ConcurrentLinkedQueue<>();
        this.unloadRequests = new ConcurrentLinkedQueue<>();
        this.loadedChunks = new ConcurrentLinkedQueue<>();
    }

    @Override
    protected void get() {

        // Internal
        this.threadSystem = get(ThreadSystem.class);
        this.buildManager = get(BuildManager.class);
    }

    @Override
    protected void update() {

    }

    // Chunk Position System \\

    void requestLoad(long chunkCoordinate) {

    }

    void requestUnload(long chunkCoordinate) {

    }
}