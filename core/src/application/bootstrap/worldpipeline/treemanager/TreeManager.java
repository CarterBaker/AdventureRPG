package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.worldpipeline.tree.TreeCastStruct;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreePaletteHandle;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeManager extends ManagerPackage {

    /*
     * Owns the tree species palette and every tree standing in the loaded
     * world. Every species is loaded in awake(), before any chunk generates,
     * so the streaming threads only ever read the palette. World generation
     * asks it for the trees that reach a chunk, which the chunk holds until
     * it is reset; the registry shares one instance of each tree between every
     * chunk it reaches. It is the one way anything touches a tree: castTree()
     * finds what a ray meets, strikeTree() lands a swing on it,
     * collectWoodBoxes() gives movement the wood to collide with, and plant()
     * roots a seed. Each frame it publishes the game day for the streaming
     * threads, grows the trees whose stage has come, and carries every felled
     * piece through its fall. Tree IDs are assigned in registration order.
     */

    // Internal
    private ClockManager clockManager;
    private MaterialManager materialManager;
    private TreeRegistryBranch treeRegistryBranch;
    private TreePlacementBranch treePlacementBranch;
    private TreeSpaceBranch treeSpaceBranch;
    private TreeChopBranch treeChopBranch;
    private TreeFallBranch treeFallBranch;
    private TreeGrowthBranch treeGrowthBranch;
    private TreePlantBranch treePlantBranch;
    private TreeSpeciesBranch treeSpeciesBranch;

    // Palette
    private Object2IntOpenHashMap<String> treeName2TreeID;
    private ObjectArrayList<TreeHandle> treeID2TreeHandle;

    // Reach — the farthest any species can reach from its root, in blocks
    private volatile float maxReachBlocks;

    // Materials
    private int barkMaterialID;
    private int leafMaterialID;

    // Clock — the game day, published for the streaming threads
    private volatile double currentDay;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.treeName2TreeID = RegistryUtility.createNameIndex();
        this.treeID2TreeHandle = RegistryUtility.createPalette();

        create(TreeLoader.class);
        this.treeRegistryBranch = create(TreeRegistryBranch.class);
        this.treePlacementBranch = create(TreePlacementBranch.class);
        this.treeSpaceBranch = create(TreeSpaceBranch.class);
        create(TreeRebuildBranch.class);
        this.treeFallBranch = create(TreeFallBranch.class);
        this.treeChopBranch = create(TreeChopBranch.class);
        this.treeGrowthBranch = create(TreeGrowthBranch.class);
        this.treePlantBranch = create(TreePlantBranch.class);
        this.treeSpeciesBranch = create(TreeSpeciesBranch.class);
        create(TreeFallRenderSystem.class);
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.materialManager = get(MaterialManager.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        // Materials
        this.barkMaterialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.TREE_BARK_MATERIAL);
        this.leafMaterialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.TREE_LEAF_MATERIAL);
    }

    // Update \\

    // Runs before the world streams each frame, so a chunk always generates against this frame's day
    @Override
    protected void update() {

        ClockHandle clockHandle = clockManager.getClockHandle();
        float deltaTime = internal.getDeltaTime();

        currentDay = clockHandle.getTotalDaysElapsed() + clockHandle.getDayProgress();

        treeGrowthBranch.update(deltaTime);
        treeFallBranch.update(deltaTime);
    }

    // Management \\

    short registerTreeName(String treeName) {
        return (short) RegistryUtility.registerID(
                treeName2TreeID, treeID2TreeHandle, treeName, EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addTreeHandle(TreeHandle treeHandle) {

        short treeID = treeHandle.getTreeID();

        if (treeID2TreeHandle.get(treeID) != null)
            throwException("Duplicate tree name: '" + treeHandle.getTreeName()
                    + "' was registered more than once");

        treeID2TreeHandle.set(treeID, treeHandle);
        updateMaxReach(treeHandle);
    }

    // A species reaching further than any before widens how far around a chunk its trees are looked for
    void updateMaxReach(TreeHandle treeHandle) {
        maxReachBlocks = Math.max(maxReachBlocks, treeHandle.getReachBlocks());
    }

    // Live Edit \\

    // Throws a catchable InternalException, leaving the species as it was, when the edit cannot go live
    public void rebuildSpecies(String treeName, ArpgObjectStruct treeArpg) {
        treeSpeciesBranch.rebuild(treeName, treeArpg);
    }

    // On-Demand \\

    public void request(String treeName) {
        ((TreeLoader) internalLoader).request(treeName);
    }

    // Generation \\

    // Streaming thread — the trees reaching a chunk handed to its palette, any it held before let go first
    public void generateTrees(WorldHandle worldHandle, long chunkCoordinate, TreePaletteHandle treePaletteHandle) {

        releaseTrees(treePaletteHandle);
        treePaletteHandle.set(treePlacementBranch.placeTrees(worldHandle, chunkCoordinate, currentDay));
    }

    // Every tree a palette holds let go, the palette left empty
    public void releaseTrees(TreePaletteHandle treePaletteHandle) {

        TreeInstance[] released = treePaletteHandle.clear();

        for (int i = 0; i < released.length; i++)
            treeRegistryBranch.release(released[i]);
    }

    // Every tree standing in the loaded world, copied into the list
    public void collectTrees(ObjectArrayList<TreeInstance> out) {
        treeRegistryBranch.collectTrees(out);
    }

    // Space \\

    // The nearest wood or leaf cluster a ray from a point of a chunk's frame meets within its reach
    public void castTree(
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3 origin,
            Vector3 direction,
            float maxDistance,
            TreeCastStruct out) {
        treeSpaceBranch.cast(worldHandle, chunkCoordinate, origin, direction, maxDistance, out);
    }

    // Every wood box overlapping a box of a chunk's frame, appended six floats each, min corner then max
    public void collectWoodBoxes(
            WorldHandle worldHandle,
            long chunkCoordinate,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            FloatArrayList out) {
        treeSpaceBranch.collectWoodBoxes(worldHandle, chunkCoordinate, minX, minY, minZ, maxX, maxY, maxZ, out);
    }

    // Interaction \\

    // A landed swing against what castTree() last met — true when the strike counted
    public boolean strikeTree(EntityInstance entity, TreeCastStruct cast, Vector3 direction) {
        return treeChopBranch.strike(entity, cast, direction);
    }

    // A seed of a species set into the ground, rooting in the block above — false when one already stands there
    public boolean plant(TreeHandle treeHandle, WorldHandle worldHandle, long anchorX, long anchorZ, int baseY) {
        return treePlantBranch.plant(treeHandle, worldHandle, anchorX, anchorZ, baseY);
    }

    // Accessible \\

    public boolean hasTree(String treeName) {
        return findTreeHandle(treeName) != null;
    }

    // A loaded species by name, null when none carries it — never loads, so any thread may ask
    public TreeHandle findTreeHandle(String treeName) {
        return RegistryUtility.getHandle(treeName2TreeID, treeID2TreeHandle, treeName);
    }

    public TreeHandle getTreeHandleFromTreeName(String treeName) {

        TreeHandle handle = findTreeHandle(treeName);

        if (handle == null) {
            request(treeName);
            handle = findTreeHandle(treeName);
        }

        if (handle == null)
            throwException("Tree \"" + treeName + "\" was not registered after its on-demand load completed — "
                    + "check for a resource-name/path mismatch in the tree directory.");

        return handle;
    }

    public TreeHandle getTreeHandleFromTreeID(short treeID) {

        TreeHandle handle = RegistryUtility.getHandle(treeID2TreeHandle, treeID);

        if (handle == null)
            throwException("No handle registered for tree ID: " + treeID);

        return handle;
    }

    public float getMaxReachBlocks() {
        return maxReachBlocks;
    }

    public int getBarkMaterialID() {
        return barkMaterialID;
    }

    public int getLeafMaterialID() {
        return leafMaterialID;
    }

    public double getCurrentDay() {
        return currentDay;
    }
}
