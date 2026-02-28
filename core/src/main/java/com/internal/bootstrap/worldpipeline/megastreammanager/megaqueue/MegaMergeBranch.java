package com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue;

import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.core.engine.BranchPackage;
import com.internal.core.kernel.threadmanager.SyncStructConsumer;
import com.internal.core.kernel.threadmanager.ThreadHandle;

/*
 * Submits a merge job to the worker thread. Sets MERGE_DATA on success.
 * The sync container is held for the duration so callers observing
 * isLocked() correctly skip this mega until the merge completes.
 */
public class MegaMergeBranch extends BranchPackage {

    // Internal
    private int mergeIndex;
    // Internal \\

    @Override
    protected void get() {
        this.mergeIndex = MegaData.MERGE_DATA.index;
    }

    public void mergeMega(MegaChunkInstance mega, ThreadHandle threadHandle) {
        executeAsync(
                threadHandle,
                mega.getMegaDataSyncContainer(),
                (SyncStructConsumer<MegaDataSyncContainer>) container -> {
                    boolean success = mega.merge();
                    container.data[mergeIndex] = success;
                });
    }
}