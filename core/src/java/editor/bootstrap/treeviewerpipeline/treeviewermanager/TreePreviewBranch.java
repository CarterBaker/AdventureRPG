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
     * species' impostor once it has one, and last the impostor itself, as
     * baked from the shared tree its LOD seed grows — every layer through the
     * very code the world draws it with and with its root at the origin, so
     * each stands where the others do. An impostor layer holds no meshes,
     * only where its quad stands and how wide, as an instance of it would.
     * A build runs on the WorldStreaming thread, one at a time; the main
     * thread uploads what it built and lets the old meshes go, and a request
     * made while a build runs is built once that one lands.
     */

    // Internal
    private TreeManager treeManager;
    private ThreadHandle threadHandle;

    // Layers
    private TreePreviewLayerStruct[] layers;

    // Build — the worker's scratch and what it publishes, read by the main thread once built is set
    private TreeModelStruct model;
    private float[] builtTreeInstance;
    private float[] builtSharedInstance;
    private boolean[] builtImpostorLayers;
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

    // Impostor — which layers draw it, and its instance for the preview's tree and for the shared tree
    private boolean[] impostorLayers;
    private float[] treeInstance;
    private float[] sharedInstance;

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
        this.builtTreeInstance = new float[EngineSetting.TREE_IMPOSTOR_INSTANCE_FLOATS];
        this.builtSharedInstance = new float[EngineSetting.TREE_IMPOSTOR_INSTANCE_FLOATS];
        this.builtImpostorLayers = new boolean[EditorSetting.TREE_VIEWER_LAYER_COUNT];

        // Impostor
        this.impostorLayers = new boolean[EditorSetting.TREE_VIEWER_LAYER_COUNT];
        this.treeInstance = new float[EngineSetting.TREE_IMPOSTOR_INSTANCE_FLOATS];
        this.sharedInstance = new float[EngineSetting.TREE_IMPOSTOR_INSTANCE_FLOATS];
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
        TreeInstance sharedTree = createTree(treeHandle, TreeLodUtility.resolveSharedSeed(treeHandle), 1f);
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
            TreeHandle treeHandle = tree.getTreeHandle();

            shared.getBuiltBark().clear();
            shared.getBuiltLeaves().clear();
            builtImpostorLayers[EditorSetting.TREE_VIEWER_LAYER_SHARED] =
                    writeInstance(sharedTree, builtSharedInstance);
            writeInstance(tree, builtTreeInstance);

            builtHeight = Math.max(
                    Math.max(tree.getShape().getMaxY(), treeHandle.getImpostor() != null
                            ? builtSharedInstance[EngineSetting.TREE_IMPOSTOR_CENTER_Y_FLOAT]
                                    + builtSharedInstance[EngineSetting.TREE_IMPOSTOR_RADIUS_FLOAT]
                            : 0f),
                    EditorSetting.TREE_VIEWER_MIN_FRAME_BLOCKS);
        } finally {
            built = true;
        }
    }

    // The preview's tree at a level, as its impostor where the world would draw it so
    private void buildLevel(TreeInstance tree, int level, int sharedLevel, TreePreviewLayerStruct layer) {

        int lod = TreeLodUtility.resolveLod(level, sharedLevel);

        layer.getBuiltBark().clear();
        layer.getBuiltLeaves().clear();
        builtImpostorLayers[level] = TreeLodUtility.isImpostor(tree.getTreeHandle(), lod, false);

        if (builtImpostorLayers[level])
            return;

        if (level == EngineSetting.TREE_LOD_FULL) {
            treeManager.buildShapeGeometry(tree.getShape(), layer.getBuiltBark(), layer.getBuiltLeaves());
            return;
        }

        TreeModelUtility.emit(tree, 0f, 0f, lod, model, layer.getBuiltBark(), layer.getBuiltLeaves());
    }

    // A tree's impostor instance with its root at the origin and never turned, false while its species has none
    private boolean writeInstance(TreeInstance tree, float[] out) {

        boolean written = TreeLodUtility.writeImpostorInstance(
                tree.getTreeHandle(), tree.getSeed(), tree.getMatureHeight(), tree.getAge(), 0L, 0L, 0f, out, 0);

        out[EngineSetting.TREE_IMPOSTOR_TURN_FLOAT] = 0f;

        return written;
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
            impostorLayers[i] = builtImpostorLayers[i];
            layer.setVertexCount(impostorLayers[i]
                    ? EngineSetting.QUAD_VERTEX_COUNT
                    : (layer.getBuiltBark().size() + layer.getBuiltLeaves().size())
                            / EngineSetting.CHUNK_VERTEX_FLOAT_COUNT);
            layer.getBuiltBark().clear();
            layer.getBuiltBark().trim();
            layer.getBuiltLeaves().clear();
            layer.getBuiltLeaves().trim();
        }

        System.arraycopy(builtTreeInstance, 0, treeInstance, 0, treeInstance.length);
        System.arraycopy(builtSharedInstance, 0, sharedInstance, 0, sharedInstance.length);
        height = builtHeight;
        revision++;
    }

    private void clearLayers() {

        for (int i = 0; i < layers.length; i++) {
            treeManager.disposeTreeMeshes(layers[i].getBarkMeshes());
            treeManager.disposeTreeMeshes(layers[i].getLeafMeshes());
            layers[i].setVertexCount(0);
            impostorLayers[i] = false;
        }
    }

    // Accessible \\

    TreePreviewLayerStruct getLayer(int layer) {
        return layers[layer];
    }

    float getHeight() {
        return height;
    }

    boolean isImpostorLayer(int layer) {
        return impostorLayers[layer];
    }

    // The instance an impostor layer draws: the shared tree's for the impostor's own layer, the preview tree's else
    float[] getImpostorInstance(int layer) {
        return layer == EditorSetting.TREE_VIEWER_LAYER_SHARED ? sharedInstance : treeInstance;
    }

    int getRevision() {
        return revision;
    }
}
