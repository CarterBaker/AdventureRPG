package application.bootstrap.worldpipeline.tree;

import engine.root.StructPackage;

public class TreeSiteStruct extends StructPackage {

    /*
     * Where one wild tree roots and what it is, before anything is grown: its
     * species, its root column, where that column lies in the region it was
     * found in, the room it keeps around itself, the seed it grows from and
     * the age it is found at. Written in place by the site search and reused
     * for the life of its thread, never allocated per tree.
     */

    // Identity
    private TreeHandle treeHandle;
    private long anchorX;
    private long anchorZ;
    private long registryKey;
    private long seed;

    // Region — blocks from the corner of the region it was found in
    private int relativeX;
    private int relativeZ;

    // Room
    private float clearanceBlocks;

    // Growth
    private float age;

    // Management \\

    public void set(
            TreeHandle treeHandle,
            long anchorX,
            long anchorZ,
            int relativeX,
            int relativeZ,
            float clearanceBlocks,
            long seed) {

        this.treeHandle = treeHandle;
        this.anchorX = anchorX;
        this.anchorZ = anchorZ;
        this.registryKey = TreeInstance.toRegistryKey(anchorX, anchorZ, treeHandle.getTreeID());
        this.relativeX = relativeX;
        this.relativeZ = relativeZ;
        this.clearanceBlocks = clearanceBlocks;
        this.seed = seed;
    }

    public void setAge(float age) {
        this.age = age;
    }

    // Room \\

    // True when this site keeps its root where the other would stand — the one keeping more room, then the higher
    // seed, then the higher key, so every site ranks every other the same way wherever it is asked
    public boolean outranks(TreeSiteStruct other) {

        if (clearanceBlocks != other.clearanceBlocks)
            return clearanceBlocks > other.clearanceBlocks;

        if (seed != other.seed)
            return seed > other.seed;

        return registryKey > other.registryKey;
    }

    // Accessible \\

    public TreeHandle getTreeHandle() {
        return treeHandle;
    }

    public long getAnchorX() {
        return anchorX;
    }

    public long getAnchorZ() {
        return anchorZ;
    }

    public long getRegistryKey() {
        return registryKey;
    }

    public long getSeed() {
        return seed;
    }

    public int getRelativeX() {
        return relativeX;
    }

    public int getRelativeZ() {
        return relativeZ;
    }

    public float getClearanceBlocks() {
        return clearanceBlocks;
    }

    public float getAge() {
        return age;
    }
}
