package application.bootstrap.worldpipeline.megachunk;

import java.util.Arrays;

import engine.root.SyncContainerPackage;

public class MegaDataSyncContainer extends SyncContainerPackage {

    /*
     * Thread-safe boolean flag array tracking which MegaData stages are complete
     * for a single mega chunk. Acquired before any read or write to the flags
     * array. getData() exposes the raw array for direct index access in hot paths
     * — callers must hold the lock.
     */

    // Internal
    boolean[] data;

    // Internal \\

    @Override
    public void create() {
        this.data = new boolean[MegaData.LENGTH];
    }

    // Reset \\

    public void resetData() {
        Arrays.fill(data, false);
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