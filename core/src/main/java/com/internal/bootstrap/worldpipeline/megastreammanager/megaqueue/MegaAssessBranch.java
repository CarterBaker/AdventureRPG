package com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue;

import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.core.engine.BranchPackage;

/*
 * Checks whether the mega has accumulated all expected chunks.
 * Sets BATCH_DATA on success so the pipeline can proceed to merge.
 */
public class MegaAssessBranch extends BranchPackage {

    public void assessMega(MegaChunkInstance mega) {

        if (!mega.isComplete())
            return;

        mega.getMegaDataSyncContainer().setData(MegaData.BATCH_DATA, true);
    }
}