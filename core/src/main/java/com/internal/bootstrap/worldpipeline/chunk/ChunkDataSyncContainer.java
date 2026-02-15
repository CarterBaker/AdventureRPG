package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.core.engine.SyncContainerPackage;

public class ChunkDataSyncContainer extends SyncContainerPackage {

    // Internal
    public boolean[] data;

    // Internal \\

    @Override
    public void create() {

        // Internal
        this.data = new boolean[ChunkData.LENGTH];
    }

    // Accessible \\

    public boolean hasData(ChunkData dataType) {
        if (!tryAcquire())
            return false;

        try {
            return data[dataType.index];
        } finally {
            release();
        }
    }

    public boolean setData(ChunkData dataType, boolean value) {
        if (!tryAcquire())
            return false;

        try {
            data[dataType.index] = value;
            return true;
        } finally {
            release();
        }
    }
}
