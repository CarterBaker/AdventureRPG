package program.bootstrap.worldpipeline.megastreammanager;

import program.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import program.bootstrap.worldpipeline.megachunk.MegaData;
import program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import program.core.engine.BranchPackage;

public class MegaAssessBranch extends BranchPackage {

    /*
     * Checks whether all expected chunks have contributed geometry to the mega.
     * Sets BATCH_DATA once isReadyToRender() returns true inside the same lock
     * acquisition — readiness check and flag write are atomic.
     */

    // Assess \\

    public void assessMega(MegaChunkInstance mega) {

        MegaDataSyncContainer sync = mega.getMegaDataSyncContainer();

        if (!sync.tryAcquire())
            return;

        try {
            if (mega.isReadyToRender())
                sync.getData()[MegaData.BATCH_DATA.index] = true;
        } finally {
            sync.release();
        }
    }
}