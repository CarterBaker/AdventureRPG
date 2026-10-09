package application.bootstrap.worldpipeline.tree;

import engine.root.DataPackage;

public class TreeData extends DataPackage {

    /*
     * Persistent tree species record: registry and display names, the name
     * seed every one of its trees is salted with, its form, trunk, branching,
     * leaves, wood and growth, and how far a grown tree can reach from its
     * root sideways and upward, in blocks. Its four parts — bark, heartwood,
     * leaves and accent leaves — are resolved once at load to the texture
     * corner each is drawn from, the tint it is drawn with, whether it hides
     * what lies behind it and whether it sways in the wind, so geometry can be
     * built on any thread without a lookup.
     */

    private final String treeName;
    private final String displayName;
    private final short treeID;
    private final int nameSeed;

    private final TreeForm form;
    private final TreeTrunkStruct trunk;
    private final TreeBranchStruct branches;
    private final TreeLeafStruct leaves;
    private final TreeWoodStruct wood;
    private final TreeGrowthStruct growth;

    private final float reachBlocks;
    private final float heightReachBlocks;

    private final float[] partCorners;
    private final int[] partColors;
    private final boolean[] partOpaque;
    private final boolean[] partSway;

    public TreeData(
            String treeName,
            String displayName,
            short treeID,
            int nameSeed,
            TreeForm form,
            TreeTrunkStruct trunk,
            TreeBranchStruct branches,
            TreeLeafStruct leaves,
            TreeWoodStruct wood,
            TreeGrowthStruct growth,
            float reachBlocks,
            float heightReachBlocks,
            float[] partCorners,
            int[] partColors,
            boolean[] partOpaque,
            boolean[] partSway) {

        this.treeName = treeName;
        this.displayName = displayName;
        this.treeID = treeID;
        this.nameSeed = nameSeed;

        this.form = form;
        this.trunk = trunk;
        this.branches = branches;
        this.leaves = leaves;
        this.wood = wood;
        this.growth = growth;

        this.reachBlocks = reachBlocks;
        this.heightReachBlocks = heightReachBlocks;

        this.partCorners = partCorners;
        this.partColors = partColors;
        this.partOpaque = partOpaque;
        this.partSway = partSway;
    }

    public String getTreeName() {
        return treeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public short getTreeID() {
        return treeID;
    }

    public int getNameSeed() {
        return nameSeed;
    }

    public TreeForm getForm() {
        return form;
    }

    public TreeTrunkStruct getTrunk() {
        return trunk;
    }

    public TreeBranchStruct getBranches() {
        return branches;
    }

    public TreeLeafStruct getLeaves() {
        return leaves;
    }

    public TreeWoodStruct getWood() {
        return wood;
    }

    public TreeGrowthStruct getGrowth() {
        return growth;
    }

    public float getReachBlocks() {
        return reachBlocks;
    }

    public float getHeightReachBlocks() {
        return heightReachBlocks;
    }

    public float[] getPartCorners() {
        return partCorners;
    }

    public int[] getPartColors() {
        return partColors;
    }

    public boolean[] getPartOpaque() {
        return partOpaque;
    }

    public boolean[] getPartSway() {
        return partSway;
    }
}
