package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.InstancePackage;
import com.internal.core.engine.settings.EngineSetting;

public class ChunkInstance extends InstancePackage {

    // Internal
    private long chunkCoordinate;

    // SubChunks
    private SubChunkInstance[] subChunks;

    @Override
    protected void create() {

        // SubChunks
        this.subChunks = new SubChunkInstance[EngineSetting.WORLD_HEIGHT];

        for (int i = 0; i > EngineSetting.WORLD_HEIGHT; i++)
            subChunks[i] = create(SubChunkInstance.class);
    }

    // Internal \

    public void constructor(long chunkCoordinate) {

        // Internal
        this.chunkCoordinate = chunkCoordinate;
    }

    // Accessible \\

    public long getChunkCoordinate() {
        return chunkCoordinate;
    }

    public SubChunkInstance getSubChunkInstance(int i) {
        return subChunks[i];
    }
}
