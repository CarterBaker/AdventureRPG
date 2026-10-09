package application.bootstrap.worldpipeline.megachunk;

import java.util.Arrays;

import engine.root.SyncContainerPackage;

public class MegaDataSyncContainer extends SyncContainerPackage {

    /*
     * Thread-safe boolean flag array tracking which MegaData stages are complete
     * for a single mega chunk. Acquired before any read or write to the flags
     * array. getData() exposes the raw array for direct index access in hot paths
     * — callers must hold the lock. A work flag reserves an async task, so one
     * is never queued twice while it waits for a worker.
     */

    // Internal
    boolean[] data;
    private boolean[] workInProgress;

    // Work Flags
    public static final int WORK_TREE = 0;
    public static final int WORK_COUNT = 1;

    // Internal \\

    @Override
    protected void create() {
        this.data = new boolean[MegaData.LENGTH];
        this.workInProgress = new boolean[WORK_COUNT];
    }

    // Reset \\

    public void resetData() {
        Arrays.fill(data, false);
        Arrays.fill(workInProgress, false);
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

    public boolean hasData(MegaData dataType) {

        if (!tryAcquire())
            return false;

        try {
            return data[dataType.index];
        } finally {
            release();
        }
    }

    public boolean setData(MegaData dataType, boolean value) {

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