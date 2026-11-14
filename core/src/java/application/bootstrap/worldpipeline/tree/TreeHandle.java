package application.bootstrap.worldpipeline.tree;

import engine.root.HandlePackage;

public class TreeHandle extends HandlePackage {

    /*
     * Persistent tree species record. Wraps TreeData and delegates all access
     * through it.
     */

    // Internal
    private TreeData treeData;

    // Constructor \\

    public void constructor(TreeData treeData) {
        this.treeData = treeData;
    }

    // Accessible \\

    public TreeData getTreeData() {
        return treeData;
    }

    public String getTreeName() {
        return treeData.getTreeName();
    }

    public String getDisplayName() {
        return treeData.getDisplayName();
    }

    public short getTreeID() {
        return treeData.getTreeID();
    }

    public int getNameSeed() {
        return treeData.getNameSeed();
    }

    public TreeForm getForm() {
        return treeData.getForm();
    }

    public TreeTrunkStruct getTrunk() {
        return treeData.getTrunk();
    }

    public TreeBranchStruct getBranches() {
        return treeData.getBranches();
    }

    public TreeLeafStruct getLeaves() {
        return treeData.getLeaves();
    }

    public TreeWoodStruct getWood() {
        return treeData.getWood();
    }

    public TreeGrowthStruct getGrowth() {
        return treeData.getGrowth();
    }

    public float getReachBlocks() {
        return treeData.getReachBlocks();
    }

    public float getHeightReachBlocks() {
        return treeData.getHeightReachBlocks();
    }

    public float[] getPartCorners() {
        return treeData.getPartCorners();
    }

    public int[] getPartColors() {
        return treeData.getPartColors();
    }

    public int[] getPartAlbedos() {
        return treeData.getPartAlbedos();
    }

    public boolean[] getPartOpaque() {
        return treeData.getPartOpaque();
    }

    public boolean[] getPartSway() {
        return treeData.getPartSway();
    }
}
