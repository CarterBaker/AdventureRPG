package com.internal.bootstrap.worldpipeline.megachunk;

import java.util.Arrays;
import com.internal.core.engine.SyncContainerPackage;

public class MegaDataSyncContainer extends SyncContainerPackage {
    // Internal
    public boolean[] data;
    // Internal \\

    @Override
    public void create() {
        this.data = new boolean[MegaData.LENGTH];
    }

    public void resetData() {
        Arrays.fill(data, false);
    }

    // Accessible \\
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