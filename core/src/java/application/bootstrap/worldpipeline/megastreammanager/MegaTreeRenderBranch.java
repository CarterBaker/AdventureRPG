package application.bootstrap.worldpipeline.megastreammanager;

import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaData;
import application.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.BranchPackage;

public class MegaTreeRenderBranch extends BranchPackage {

    /*
     * Main thread — uploads a mega's tree stand-ins beside its terrain and
     * lets the CPU copy go, as the mega's own geometry does. Sets
     * TREE_RENDER_DATA on success.
     */

    // Internal
    private WorldRenderManager worldRenderManager;

    // Settings
    private int treeRenderDataIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.worldRenderManager = get(WorldRenderManager.class);

        // Settings
        this.treeRenderDataIndex = MegaData.TREE_RENDER_DATA.index;
    }

    // Tree Render \\

    public void renderTrees(MegaChunkInstance mega, MegaDataSyncContainer sync) {

        if (!sync.tryAcquire())
            return;

        try {
            DynamicPacketInstance treePacketInstance = mega.getTreePacketInstance();

            if (!worldRenderManager.addMegaTrees(mega.getCoordinate(), treePacketInstance))
                return;

            treePacketInstance.clear();
            sync.getData()[treeRenderDataIndex] = true;
        } finally {
            sync.release();
        }
    }
}
