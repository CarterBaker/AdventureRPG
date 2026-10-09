package application.bootstrap.worldpipeline.chunk;

import java.util.Arrays;

import engine.root.SyncContainerPackage;

public class ChunkDataSyncContainer extends SyncContainerPackage {

    /*
     * Thread-safe boolean flag array tracking which ChunkData stages are
     * complete for a single chunk. Acquired before any read or write to the
     * flags array. getData() exposes the raw array for direct index access
     * in hot paths — callers must hold the lock. hasWorkLocked() tells
     * whether any async task is still reserved, so a chunk is never pooled
     * with a task left to run on it.
     */

    // Internal
    boolean[] data;
    private boolean[] workInProgress;

    // Work Flags
    public static final int WORK_LOAD = 0;
    public static final int WORK_BUILD = 1;
    public static final int WORK_MERGE = 2;
    public static final int WORK_ITEM_LOAD = 3;
    public static final int WORK_BATCH = 4;
    public static final int WORK_TREE = 5;
    public static final int WORK_COUNT = 6;

    // Internal \\

    @Override
    protected void create() {
        this.data = new boolean[ChunkData.LENGTH];
        this.workInProgress = new boolean[WORK_COUNT];
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

    public boolean hasWorkLocked() {

        for (int i = 0; i < workInProgress.length; i++)
            if (workInProgress[i])
                return true;

        return false;
    }

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