package application.bootstrap.worldpipeline.tree;

import application.bootstrap.worldpipeline.util.TreeShapeUtility;
import application.bootstrap.worldpipeline.util.TreeSkeletonUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.extras.Coordinate3Long;

public class TreeInstance extends InstancePackage {

    /*
     * One tree standing in the world: its species, the block column its root
     * grows from and the block its trunk rises out of, the seed it grew from,
     * the game day it was planted, and whether a hand planted it or the world
     * did. Its skeleton is grown once; what stands of it is a shape rebuilt
     * on the main thread whenever it grows a stage, an axe notches it, a cut
     * severs part of it or a cluster of its leaves is knocked off, and
     * published whole, so chunk builds on any
     * thread read it without a lock. The first wound freezes its age, so it
     * grows no further and every notch stays where it was struck. Every chunk
     * its reach overlaps holds it, and the registry keeps the count.
     */

    // Identity
    private TreeHandle treeHandle;
    private WorldHandle worldHandle;
    private long anchorX;
    private long anchorZ;
    private int baseY;
    private long seed;
    private boolean planted;

    // Growth
    private double plantedDay;
    private TreeSkeletonStruct skeleton;
    private float[] severs;
    private boolean[] brokenLeaves;
    private TreeCarveStruct[] carves;
    private float frozenAge;
    private int growthStage;

    // Shape
    private volatile TreeShapeStruct shape;

    // Registry
    private long registryKey;
    private int references;

    // Query — the last space query that visited this tree, so one query tests it once
    private int queryStamp;

    // Constructor \\

    public void constructor(
            TreeHandle treeHandle,
            WorldHandle worldHandle,
            long anchorX,
            long anchorZ,
            int baseY,
            long seed,
            double plantedDay,
            boolean planted,
            double currentDay) {

        // Identity
        this.treeHandle = treeHandle;
        this.worldHandle = worldHandle;
        this.anchorX = anchorX;
        this.anchorZ = anchorZ;
        this.baseY = baseY;
        this.seed = seed;
        this.planted = planted;
        this.registryKey = toRegistryKey(anchorX, anchorZ, treeHandle.getTreeID());

        // Growth
        this.plantedDay = plantedDay;
        this.skeleton = TreeSkeletonUtility.grow(treeHandle, seed);
        this.severs = TreeShapeUtility.createSevers(skeleton);
        this.brokenLeaves = TreeShapeUtility.createBrokenLeaves(skeleton);
        this.carves = new TreeCarveStruct[0];
        this.frozenAge = Float.NaN;

        float age = resolveAge(currentDay);

        this.growthStage = toStage(age);
        this.shape = TreeShapeUtility.evaluate(treeHandle, seed, skeleton, age, severs, brokenLeaves, carves);
    }

    // Registry \\

    // One key per species and root column — the species' ID rides in the packed height a column leaves unused
    public static long toRegistryKey(long anchorX, long anchorZ, short treeID) {
        return Coordinate3Long.pack((int) anchorX, treeID, (int) anchorZ);
    }

    // Growth \\

    // The share of its full growth the tree has reached by a game day, held where its first wound froze it
    public float resolveAge(double currentDay) {

        if (!Float.isNaN(frozenAge))
            return frozenAge;

        double grownDays = currentDay - plantedDay;

        return (float) Math.max(0.0, Math.min(1.0, grownDays / treeHandle.getGrowth().getDays()));
    }

    // The growth stage an age falls in — the shape only changes when the stage does
    public static int toStage(float age) {
        return Math.min(EngineSetting.TREE_GROWTH_STAGES, (int) (age * EngineSetting.TREE_GROWTH_STAGES));
    }

    // Main thread — the shape regrown to an age, publishing a new shape
    public void regrow(float age) {
        this.growthStage = toStage(age);
        this.shape = TreeShapeUtility.evaluate(treeHandle, seed, skeleton, age, severs, brokenLeaves, carves);
    }

    // Main thread — the whole tree grown again from its seed after its species changed, every wound healed
    public void regrowSpecies(double currentDay) {

        this.skeleton = TreeSkeletonUtility.grow(treeHandle, seed);
        this.severs = TreeShapeUtility.createSevers(skeleton);
        this.brokenLeaves = TreeShapeUtility.createBrokenLeaves(skeleton);
        this.carves = new TreeCarveStruct[0];
        this.frozenAge = Float.NaN;

        regrow(resolveAge(currentDay));
    }

    // Wounds \\

    // Main thread — a notch struck into the tree, freezing its age at the first one
    public void addCarve(TreeCarveStruct carve, double currentDay) {

        freeze(currentDay);

        TreeCarveStruct[] grown = new TreeCarveStruct[carves.length + 1];

        System.arraycopy(carves, 0, grown, 0, carves.length);
        grown[carves.length] = carve;

        this.carves = grown;
        this.shape = shape.withCarves(grown);
    }

    // Main thread — one skeleton segment cut at a share of its length: everything beyond the cut leaves the tree,
    // and the piece it frees is handed back
    public TreeShapeStruct sever(int segment, float share, double currentDay) {

        freeze(currentDay);

        TreeShapeStruct piece = TreeShapeUtility.extract(
                treeHandle, seed, skeleton, frozenAge, severs, brokenLeaves, segment, share, carves);
        float[] cut = severs.clone();
        cut[segment] = Math.min(cut[segment], share);

        this.severs = cut;
        this.shape = TreeShapeUtility.evaluate(treeHandle, seed, skeleton, frozenAge, cut, brokenLeaves, carves);

        return piece;
    }

    // Main thread — one skeleton cluster knocked off for good, the tree growing on without it
    public void breakLeaf(int leaf, double currentDay) {

        boolean[] broken = brokenLeaves.clone();
        broken[leaf] = true;

        this.brokenLeaves = broken;
        this.shape = TreeShapeUtility.evaluate(
                treeHandle, seed, skeleton, resolveAge(currentDay), severs, broken, carves);
    }

    private void freeze(double currentDay) {
        if (Float.isNaN(frozenAge))
            this.frozenAge = resolveAge(currentDay);
    }

    public boolean isWounded() {
        return !Float.isNaN(frozenAge);
    }

    // Accessible \\

    public TreeHandle getTreeHandle() {
        return treeHandle;
    }

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public long getAnchorX() {
        return anchorX;
    }

    public long getAnchorZ() {
        return anchorZ;
    }

    public int getBaseY() {
        return baseY;
    }

    public long getSeed() {
        return seed;
    }

    public boolean isPlanted() {
        return planted;
    }

    public TreeSkeletonStruct getSkeleton() {
        return skeleton;
    }

    public float[] getSevers() {
        return severs;
    }

    public boolean[] getBrokenLeaves() {
        return brokenLeaves;
    }

    public TreeCarveStruct[] getCarves() {
        return carves;
    }

    public int getGrowthStage() {
        return growthStage;
    }

    public TreeShapeStruct getShape() {
        return shape;
    }

    public long getRegistryKey() {
        return registryKey;
    }

    public int getReferences() {
        return references;
    }

    public void setReferences(int references) {
        this.references = references;
    }

    // True the first time a query with this stamp visits the tree
    public boolean visit(int stamp) {

        if (queryStamp == stamp)
            return false;

        queryStamp = stamp;

        return true;
    }
}
