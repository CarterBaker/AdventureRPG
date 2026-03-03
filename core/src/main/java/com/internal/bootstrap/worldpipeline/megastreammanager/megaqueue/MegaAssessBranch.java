package com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue;

import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.core.engine.BranchPackage;

/*
 * Checks whether all expected chunks have contributed geometry to the mega.
 * Sets BATCH_DATA once isReadyToRender() returns true inside the same
 * lock acquisition — readiness check and flag write are atomic.
 */
public class MegaAssessBranch extends BranchPackage {

    public void assessMega(MegaChunkInstance mega) {
        MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();
        if (!sync.tryAcquire())
            return;
        try {
            if (mega.isReadyToRender())
                sync.data[MegaData.BATCH_DATA.index] = true;
        } finally {
            sync.release();
        }
    }
}