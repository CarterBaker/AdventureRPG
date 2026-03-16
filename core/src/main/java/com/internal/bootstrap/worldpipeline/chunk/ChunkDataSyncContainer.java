package com.internal.bootstrap.worldpipeline.chunk;

import java.util.Arrays;
import com.internal.core.engine.SyncContainerPackage;

public class ChunkDataSyncContainer extends SyncContainerPackage {

    /*
     * Thread-safe boolean flag array tracking which ChunkData stages are
     * complete for a single chunk. Acquired before any read or write to the
     * flags array. getData() exposes the raw array for direct index access
     * in hot paths — callers must hold the lock.
     */

    // Internal
    boolean[] data;

    // Internal \\

    @Override
    public void create() {
        this.data = new boolean[ChunkData.LENGTH];
    }

    // Reset \\

    public void resetData() {
        Arrays.fill(data, false);
    }

    // Accessible \\

    public boolean[] getData() {
        return data;
    }

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