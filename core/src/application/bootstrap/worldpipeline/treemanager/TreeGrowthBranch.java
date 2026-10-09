package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TreeGrowthBranch extends BranchPackage {

    /*
     * Main thread — grows every unwounded tree in the loaded world with the
     * game's days. A tree's shape only changes when its age crosses into its
     * next growth stage, so most checks find nothing to do; every so often
     * the registry is walked from where the last walk stopped, and only a
     * few trees are regrown and redrawn per walk, so a forest coming of age
     * together never costs a frame more than a few small rebuilds.
     */

    // Internal
    private TreeManager treeManager;
    private TreeRegistryBranch treeRegistryBranch;
    private TreeRebuildBranch treeRebuildBranch;

    // Walk
    private ObjectArrayList<TreeInstance> trees;
    private int cursor;
    private float sinceCheck;

    // Base \\

    @Override
    protected void create() {

        // Walk
        this.trees = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.treeRegistryBranch = get(TreeRegistryBranch.class);
        this.treeRebuildBranch = get(TreeRebuildBranch.class);
    }

    // Update \\

    void update(float deltaTime) {

        sinceCheck += deltaTime;

        if (sinceCheck < EngineSetting.TREE_GROWTH_CHECK_SECONDS)
            return;

        sinceCheck = 0f;
        treeRegistryBranch.collectTrees(trees);

        double currentDay = treeManager.getCurrentDay();
        int regrown = 0;

        for (int visited = 0; visited < trees.size() && regrown < EngineSetting.TREE_GROWTH_REBUILDS_PER_CHECK;
                visited++) {

            cursor = cursor >= trees.size() ? 0 : cursor;

            TreeInstance tree = trees.get(cursor++);

            if (tree.isWounded())
                continue;

            float age = tree.resolveAge(currentDay);

            if (TreeInstance.toStage(age) == tree.getGrowthStage())
                continue;

            TreeShapeStruct before = tree.getShape();

            tree.regrow(age);
            treeRebuildBranch.rebuildTree(tree, before, tree.getShape());
            regrown++;
        }
    }
}
