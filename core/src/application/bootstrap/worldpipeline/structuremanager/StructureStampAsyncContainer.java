package application.bootstrap.worldpipeline.structuremanager;

import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

public class StructureStampAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for stamping one structure into one chunk: the
     * lowest solid structure block per column of the chunk, which a
     * foundation is filled up to.
     */

    static final int COLUMN_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE;

    // Foundation
    int[] columnFloorY;

    @Override
    protected void create() {
        this.columnFloorY = new int[COLUMN_COUNT];
    }
}
