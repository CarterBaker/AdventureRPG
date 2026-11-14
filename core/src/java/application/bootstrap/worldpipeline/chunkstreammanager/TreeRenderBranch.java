package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.BranchPackage;

public class TreeRenderBranch extends BranchPackage {

    /*
     * Main thread — uploads a chunk's tree packet beside its terrain. Once on
     * the GPU the packet's CPU copy is let go, since a chunk's trees are only
     * ever drawn again from a fresh build. uploadLocked() is the one place a
     * chunk's trees reach the renderer. Sets TREE_RENDER_DATA on success.
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
        this.treeRenderDataIndex = ChunkData.TREE_RENDER_DATA.index;
    }

    // Tree Render \\

    public void renderTrees(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        try {
            syncContainer.getData()[treeRenderDataIndex] = uploadLocked(chunkInstance);
        } finally {
            syncContainer.release();
        }
    }

    // Under the chunk's lock — false when no grid draws the chunk, which keeps its packet for the next upload
    boolean uploadLocked(ChunkInstance chunkInstance) {

        DynamicPacketInstance treePacketInstance = chunkInstance.getTreePacketInstance();

        if (!worldRenderManager.addChunkTrees(chunkInstance.getCoordinate(), treePacketInstance))
            return false;

        treePacketInstance.clear();

        return true;
    }
}
