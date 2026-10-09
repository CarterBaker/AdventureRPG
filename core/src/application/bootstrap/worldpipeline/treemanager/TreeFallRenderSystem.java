package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.itempipeline.itemmodelmanager.ItemModelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.worldpipeline.tree.TreeFallInstance;
import application.bootstrap.worldpipeline.tree.TreeFallState;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeFallRenderSystem extends SystemPackage {

    /*
     * Draws every falling piece of tree near a viewer into a window's world
     * target, placed relative to the viewer's chunk by its own fall: its wood
     * with the falling bark material and its leaves with the falling leaf
     * material, both writing the same G-buffer as the trees still standing,
     * so a felled trunk looks no different as it comes down. Every draw takes
     * a model of its own from ItemModelManager.
     */

    // Internal
    private RenderManager renderManager;
    private MaterialManager materialManager;
    private ItemModelManager itemModelManager;
    private TreeFallBranch treeFallBranch;

    // Material
    private int barkMaterialID;
    private int leafMaterialID;

    // Settings
    private int chunkSize;

    // Scratch
    private Matrix4 fallMatrix;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;

        // Scratch
        this.fallMatrix = new Matrix4();
    }

    @Override
    protected void get() {
        this.renderManager = get(RenderManager.class);
        this.materialManager = get(MaterialManager.class);
        this.itemModelManager = get(ItemModelManager.class);
        this.treeFallBranch = get(TreeFallBranch.class);
    }

    @Override
    protected void awake() {

        // Material
        this.barkMaterialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.TREE_BARK_FALLING_MATERIAL);
        this.leafMaterialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.TREE_LEAF_FALLING_MATERIAL);
    }

    // Render \\

    public void pushFallingTrees(
            WorldHandle worldHandle,
            long viewerChunkCoordinate,
            FBOInstance targetFbo,
            WindowInstance window) {

        ObjectArrayList<TreeFallInstance> falls = treeFallBranch.getFalls();

        for (int i = 0; i < falls.size(); i++) {

            TreeFallInstance fall = falls.get(i);
            TreeInstance tree = fall.getTree();

            if (tree.getWorldHandle() != worldHandle
                    || fall.getState() == TreeFallState.BUILDING || fall.getState() == TreeFallState.DONE)
                continue;

            long originX = (long) Coordinate2Long.unpackX(viewerChunkCoordinate) * chunkSize;
            long originZ = (long) Coordinate2Long.unpackY(viewerChunkCoordinate) * chunkSize;

            fall.composeMatrix(
                    WorldWrapUtility.wrappedBlockDeltaX(worldHandle, tree.getAnchorX(), originX)
                            + EngineSetting.TREE_ROOT_CENTER_BLOCKS,
                    tree.getBaseY(),
                    WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, tree.getAnchorZ(), originZ)
                            + EngineSetting.TREE_ROOT_CENTER_BLOCKS,
                    fallMatrix);

            pushMeshes(fall.getBarkMeshes(), barkMaterialID, targetFbo, window);
            pushMeshes(fall.getLeafMeshes(), leafMaterialID, targetFbo, window);
        }
    }

    private void pushMeshes(
            ObjectArrayList<MeshInstance> meshes,
            int materialID,
            FBOInstance targetFbo,
            WindowInstance window) {

        for (int i = 0; i < meshes.size(); i++) {

            ModelInstance model = itemModelManager.acquireModel(meshes.get(i).getMeshData(), materialID);

            model.getMaterial().setUniform(EngineSetting.UNIFORM_ITEM_MODEL, fallMatrix);
            renderManager.pushRenderCall(model, targetFbo, EngineSetting.EQUIPMENT_RENDER_DEPTH, window);
        }
    }
}
