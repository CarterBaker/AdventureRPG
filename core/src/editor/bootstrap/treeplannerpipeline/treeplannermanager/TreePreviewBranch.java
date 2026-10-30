package editor.bootstrap.treeplannerpipeline.treeplannermanager;

import application.bootstrap.worldpipeline.tree.TreeDetail;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeModelStruct;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.TreeModelUtility;
import application.kernel.threadpipeline.thread.ThreadHandle;
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePreviewTierStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class TreePreviewBranch extends BranchPackage {

    /*
     * Builds the Tree Planner's preview: one tree of the selected species,
     * grown from the planner's seed to its age, drawn at every detail the
     * game draws trees at — in full, as a model, a sketch and an outline —
     * side by side in a row, each through the very code the world draws it
     * with, so the Tree Viewer shows exactly what the world shows. A build
     * runs on the WorldStreaming thread, one at a time; the main thread
     * uploads what it built and lets the old meshes go, and a request made
     * while a build runs is built once that one lands.
     */

    // Internal
    private TreeManager treeManager;
    private ThreadHandle threadHandle;

    // Tiers
    private TreePreviewTierStruct[] tiers;

    // Build — the worker's scratch and what it publishes, read by the main thread once built is set
    private TreeModelStruct model;
    private float builtSpacing;
    private float builtHeight;
    private volatile boolean built;
    private boolean building;

    // Request
    private boolean requested;
    private String requestedTreeName;
    private long requestedSeed;
    private float requestedAge;

    // Frame — the room between the tiers of the row and the height of its tallest tree, in blocks
    private float spacing;
    private float height;

    // Revision
    private int revision;

    // Base \\

    @Override
    protected void create() {

        TreeDetail[] details = TreeDetail.values();

        // Tiers
        this.tiers = new TreePreviewTierStruct[details.length];

        for (int i = 0; i < details.length; i++)
            tiers[i] = new TreePreviewTierStruct(details[i]);

        // Build
        this.model = new TreeModelStruct();
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
    }

    // Request \\

    // The row built again for a species, seed and age — a species of null leaves the row empty
    void request(String treeName, long seed, float age) {

        this.requested = true;
        this.requestedTreeName = treeName;
        this.requestedSeed = seed;
        this.requestedAge = age;
    }

    // Refresh \\

    // Main thread — a finished build uploaded, then the latest request begun once no build runs
    void refresh() {

        if (built)
            upload();

        if (requested && !building)
            beginBuild();
    }

    // Main thread — the requested tree created and handed to the worker, or the row emptied when there is none
    private void beginBuild() {

        requested = false;

        TreeHandle treeHandle = requestedTreeName != null ? treeManager.findTreeHandle(requestedTreeName) : null;

        if (treeHandle == null) {
            clearTiers();
            revision++;
            return;
        }

        TreeInstance tree = create(TreeInstance.class);
        tree.constructor(
                treeHandle, null, 0L, 0L, 0, requestedSeed,
                -requestedAge * treeHandle.getGrowth().getDays(), false, 0.0);

        building = true;
        executeAsync(threadHandle, () -> build(tree));
    }

    // Worker — every tier's vertices, the tree's root centre at the origin of its own frame
    private void build(TreeInstance tree) {

        try {

            TreeShapeStruct shape = tree.getShape();

            for (int i = 0; i < tiers.length; i++)
                buildTier(tree, shape, tiers[i]);

            builtSpacing = Math.max(
                    tree.getTreeHandle().getReachBlocks() * EditorSetting.TREE_PLANNER_TIER_SPACING_REACHES,
                    EditorSetting.TREE_PLANNER_TIER_MIN_SPACING_BLOCKS);
            builtHeight = Math.max(shape.getMaxY(), EditorSetting.TREE_PLANNER_MIN_FRAME_BLOCKS);
        } finally {
            built = true;
        }
    }

    private void buildTier(TreeInstance tree, TreeShapeStruct shape, TreePreviewTierStruct tier) {

        tier.getBuiltBark().clear();
        tier.getBuiltLeaves().clear();

        if (tier.getDetail() == TreeDetail.FULL) {
            treeManager.buildShapeGeometry(shape, tier.getBuiltBark(), tier.getBuiltLeaves());
            return;
        }

        TreeModelUtility.emit(
                tree, 0f, 0f, tier.getDetail(), model, tier.getBuiltBark(), tier.getBuiltLeaves());
    }

    // Main thread — every tier's old meshes let go and its new vertices uploaded, then given back
    private void upload() {

        built = false;
        building = false;

        for (int i = 0; i < tiers.length; i++) {

            TreePreviewTierStruct tier = tiers[i];

            treeManager.disposeTreeMeshes(tier.getBarkMeshes());
            treeManager.disposeTreeMeshes(tier.getLeafMeshes());
            treeManager.uploadTreeMeshes(tier.getBuiltBark(), tier.getBarkMeshes());
            treeManager.uploadTreeMeshes(tier.getBuiltLeaves(), tier.getLeafMeshes());
            tier.setVertexCount((tier.getBuiltBark().size() + tier.getBuiltLeaves().size())
                    / EngineSetting.CHUNK_VERTEX_FLOAT_COUNT);
            tier.getBuiltBark().clear();
            tier.getBuiltBark().trim();
            tier.getBuiltLeaves().clear();
            tier.getBuiltLeaves().trim();
        }

        spacing = builtSpacing;
        height = builtHeight;
        revision++;
    }

    private void clearTiers() {

        for (int i = 0; i < tiers.length; i++) {
            treeManager.disposeTreeMeshes(tiers[i].getBarkMeshes());
            treeManager.disposeTreeMeshes(tiers[i].getLeafMeshes());
            tiers[i].setVertexCount(0);
        }
    }

    // Accessible \\

    int getTierCount() {
        return tiers.length;
    }

    TreePreviewTierStruct getTier(int tier) {
        return tiers[tier];
    }

    // Where a tier's root stands along the row, the row centred on the origin
    float getTierOffsetX(int tier) {
        return (tier - (tiers.length - 1) * 0.5f) * spacing;
    }

    float getHeight() {
        return height;
    }

    int getRevision() {
        return revision;
    }
}
