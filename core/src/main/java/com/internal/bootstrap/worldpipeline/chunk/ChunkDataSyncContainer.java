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
    private boolean[] workInProgress;

    // Work Flags
    public static final int WORK_LOAD = 0;
    public static final int WORK_BUILD = 1;
    public static final int WORK_MERGE = 2;
    public static final int WORK_ITEM_LOAD = 3;

    // Internal \\

    @Override
    public void create() {
        this.data = new boolean[ChunkData.LENGTH];
        this.workInProgress = new boolean[4];
    }

    // Reset \\

    public void resetData() {
        Arrays.fill(data, false);
        Arrays.fill(workInProgress, false);
    }

    public boolean beginWork(int workType) {

        if (!tryAcquire())
            return false;

        try {
            return beginWorkLocked(workType);
        } finally {
            release();
        }
    }

    public boolean beginWorkLocked(int workType) {

        if (workInProgress[workType])
            return false;

        workInProgress[workType] = true;
        return true;
    }

    public void endWork(int workType) {
        acquire();
        try {
            workInProgress[workType] = false;
        } finally {
            release();
        }
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