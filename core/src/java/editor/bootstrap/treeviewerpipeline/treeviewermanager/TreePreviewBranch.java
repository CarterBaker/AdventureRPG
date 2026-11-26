package editor.bootstrap.treeviewerpipeline.treeviewermanager;

import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeModelStruct;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.TreeLodUtility;
import application.bootstrap.worldpipeline.util.TreeModelUtility;
import application.kernel.threadpipeline.thread.ThreadHandle;
import editor.bootstrap.treeviewerpipeline.treeviewer.TreePreviewLayerStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class TreePreviewBranch extends BranchPackage {

    /*
     * Builds the Tree Viewer's preview: one tree of the selected species,
     * grown from the viewer's seed to its age, drawn at every LOD level the
     * world draws trees at, each level from the shared level on as the
     * species' shared model, and last the shared tree itself, grown in full
     * from the species' LOD seed — every layer through the very code the
     * world draws it with and with its root at the origin, so each stands
     * where the others do. A build runs on the WorldStreaming thread, one at
     * a time; the main thread uploads what it built and lets the old meshes
     * go, and a request made while a build runs is built once that one lands.
     */

    // Internal
    private TreeManager treeManager;
    private ThreadHandle threadHandle;

    // Layers
    private TreePreviewLayerStruct[] layers;

    // Build — the worker's scratch and what it publishes, read by the main thread once built is set
    private TreeModelStruct model;
    private float builtHeight;
    private volatile boolean built;
    private boolean building;

    // Request
    private boolean requested;
    private String requestedTreeName;
    private long requestedSeed;
    private float requestedAge;
    private int requestedSharedLevel;

    // Frame — the height of the tallest tree of the preview, in blocks
    private float height;

    // Revision
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Layers
        this.layers = new TreePreviewLayerStruct[EditorSetting.TREE_VIEWER_LAYER_COUNT];

        for (int i = 0; i < layers.length; i++)
            layers[i] = new TreePreviewLayerStruct();

        // Build
        this.model = new TreeModelStruct();
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
    }

    // Request \\

    // The preview built again for a species, seed, age and shared level — a species of null leaves it empty
    void request(String treeName, long seed, float age, int sharedLevel) {

        this.requested = true;
        this.requestedTreeName = treeName;
        this.requestedSeed = seed;
        this.requestedAge = age;
        this.requestedSharedLevel = sharedLevel;
    }

    // Refresh \\

    // Main thread — a finished build uploaded, then the latest request begun once no build runs
    void refresh() {

        if (built)
            upload();

        if (requested && !building)
            beginBuild();
    }

    // Main thread — the requested tree and its species' shared tree created and handed to the worker, or the
    // preview emptied when there is none
    private void beginBuild() {

        requested = false;

        TreeHandle treeHandle = requestedTreeName != null ? treeManager.findTreeHandle(requestedTreeName) : null;

        if (treeHandle == null) {
            clearLayers();
            revision++;
            return;
        }

        TreeInstance tree = createTree(treeHandle, requestedSeed, requestedAge);
        TreeInstance sharedTree = createTree(treeHandle, treeHandle.getShared().getSeed(), 1f);
        int sharedLevel = requestedSharedLevel;

        building = true;
        executeAsync(threadHandle, () -> build(tree, sharedTree, sharedLevel));
    }

    private TreeInstance createTree(TreeHandle treeHandle, long seed, float age) {

        TreeInstance tree = create(TreeInstance.class);
        tree.constructor(treeHandle, null, 0L, 0L, 0, seed, -age * treeHandle.getGrowth().getDays(), false, 0.0);

        return tree;
    }

    // Worker — every layer's vertices, each tree's root centre at the origin of its own frame
    private void build(TreeInstance tree, TreeInstance sharedTree, int sharedLevel) {

        try {

            for (int level = 0; level < EngineSetting.TREE_LOD_LEVEL_COUNT; level++)
                buildLevel(tree, level, sharedLevel, layers[level]);

            TreePreviewLayerStruct shared = layers[EditorSetting.TREE_VIEWER_LAYER_SHARED];

            shared.getBuiltBark().clear();
            shared.getBuiltLeaves().clear();
            treeManager.buildShapeGeometry(sharedTree.getShape(), shared.getBuiltBark(), shared.getBuiltLeaves());

            builtHeight = Math.max(
                    Math.max(tree.getShape().getMaxY(), sharedTree.getShape().getMaxY()),
                    EditorSetting.TREE_VIEWER_MIN_FRAME_BLOCKS);
        } finally {
            built = true;
        }
    }

    private void buildLevel(TreeInstance tree, int level, int sharedLevel, TreePreviewLayerStruct layer) {

        layer.getBuiltBark().clear();
        layer.getBuiltLeaves().clear();

        if (level == EngineSetting.TREE_LOD_FULL) {
            treeManager.buildShapeGeometry(tree.getShape(), layer.getBuiltBark(), layer.getBuiltLeaves());
            return;
        }

        TreeModelUtility.emit(
                tree, 0f, 0f, TreeLodUtility.encode(level, level >= sharedLevel),
                model, layer.getBuiltBark(), layer.getBuiltLeaves());
    }

    // Main thread — every layer's old meshes let go and its new vertices uploaded, then given back
    private void upload() {

        built = false;
        building = false;

        for (int i = 0; i < layers.length; i++) {

            TreePreviewLayerStruct layer = layers[i];

            treeManager.disposeTreeMeshes(layer.getBarkMeshes());
            treeManager.disposeTreeMeshes(layer.getLeafMeshes());
            treeManager.uploadTreeMeshes(layer.getBuiltBark(), layer.getBarkMeshes());
            treeManager.uploadTreeMeshes(layer.getBuiltLeaves(), layer.getLeafMeshes());
            layer.setVertexCount((layer.getBuiltBark().size() + layer.getBuiltLeaves().size())
                    / EngineSetting.CHUNK_VERTEX_FLOAT_COUNT);
            layer.getBuiltBark().clear();
            layer.getBuiltBark().trim();
            layer.getBuiltLeaves().clear();
            layer.getBuiltLeaves().trim();
        }

        height = builtHeight;
        revision++;
    }

    private void clearLayers() {

        for (int i = 0; i < layers.length; i++) {
            treeManager.disposeTreeMeshes(layers[i].getBarkMeshes());
            treeManager.disposeTreeMeshes(layers[i].getLeafMeshes());
            layers[i].setVertexCount(0);
        }
    }

    // Accessible \\

    TreePreviewLayerStruct getLayer(int layer) {
        return layers[layer];
    }

    float getHeight() {
        return height;
    }

    int getRevision() {
        return revision;
    }
}
