package com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue;

import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.core.engine.BranchPackage;

/*
 * Checks whether all expected chunks have contributed geometry to the mega.
 * Sets BATCH_DATA once isReadyToRender() returns true, signalling the
 * pipeline to proceed to GPU upload.
 */
public class MegaAssessBranch extends BranchPackage {

    public void assessMega(MegaChunkInstance mega) {
        if (!mega.isReadyToRender())
            return;
        mega.getMegaDataSyncContainer().setData(MegaData.BATCH_DATA, true);
    }
}