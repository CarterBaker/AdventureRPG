package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.BiomeTreeStruct;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.util.TreeDistributionUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TreePlacementBranch extends BranchPackage {

    /*
     * Async — finds every tree that reaches one chunk, on that chunk's own
     * worker thread. The biomes around the chunk are sampled first, out as far
     * as the widest tree can reach, so only the kinds of tree those biomes
     * grow are walked. Each kind lays its placement cells over the chunk and
     * its reach: a scattered kind rolls one tree per cell, a clustered kind
     * one grove per cell spread inside its cluster radius, and a field kind a
     * tree in nearly every cell, thinned by a slow patch noise into woods and
     * clearings. Cheap rejections run first: the rolls, then reach, then the
     * biome at the root, then the ground a settlement or road claims, then
     * the ground beneath it. Every roll is a pure
     * function of the world seed, the tree and the place, so every chunk a
     * tree reaches finds it on its own; the registry then hands every chunk
     * the same instance. Every tree a hand planted that reaches the chunk is
     * held for it too.
     */

    // Internal
    private TreeManager treeManager;
    private TreeRegistryBranch treeRegistryBranch;
    private BiomeManager biomeManager;
    private WorldGenerationManager worldGenerationManager;
    private SettlementManager settlementManager;
    private TreePlacementAsyncContainer placementContainer;

    // Settings
    private int chunkSize;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.placementContainer = create(TreePlacementAsyncContainer.class);

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.treeRegistryBranch = get(TreeRegistryBranch.class);
        this.biomeManager = get(BiomeManager.class);
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.settlementManager = get(SettlementManager.class);
    }

    // Placement \\

    // Every tree reaching the chunk, each held once more by the registry for it
    TreeInstance[] placeTrees(WorldHandle worldHandle, long chunkCoordinate, double currentDay) {

        TreePlacementAsyncContainer scratch = placementContainer.getInstance();

        if (scratch.worldHandle != worldHandle)
            scratch.anchor2Ground.clear();

        scratch.reset();
        scratch.worldHandle = worldHandle;
        scratch.chunkOriginX = (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize;
        scratch.chunkOriginZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize;
        scratch.currentDay = currentDay;

        gatherBiomes(scratch);

        for (int i = 0; i < scratch.biomes.size(); i++) {

            BiomeHandle biome = scratch.biomes.get(i);
            ObjectArrayList<BiomeTreeStruct> kinds = biome.getTrees();

            for (int k = 0; k < kinds.size(); k++)
                placeKind(scratch, biome, kinds.get(k));
        }

        placePlanted(scratch);

        return scratch.trees.toArray(new TreeInstance[0]);
    }

    // Every biome with trees standing within the widest reach of the chunk
    private void gatherBiomes(TreePlacementAsyncContainer scratch) {

        int reach = (int) Math.ceil(treeManager.getMaxReachBlocks())
                + EngineSetting.BIOME_MAX_TREE_CLUSTER_RADIUS_BLOCKS;
        int spacing = EngineSetting.TREE_BIOME_SAMPLE_SPACING_BLOCKS;

        for (long z = scratch.chunkOriginZ - reach; z <= scratch.chunkOriginZ + chunkSize + reach; z += spacing)
            for (long x = scratch.chunkOriginX - reach; x <= scratch.chunkOriginX + chunkSize + reach; x += spacing) {

                biomeManager.sampleBiomeField(scratch.worldHandle,
                        WorldWrapUtility.wrapBlockX(scratch.worldHandle, x),
                        WorldWrapUtility.wrapBlockZ(scratch.worldHandle, z),
                        scratch.blend);

                BiomeHandle biome = scratch.blend.getDominantBiome();

                if (biome != null && !biome.getTrees().isEmpty() && scratch.biomeIDs.add(biome.getBiomeID()))
                    scratch.biomes.add(biome);
            }
    }

    // Kind \\

    private void placeKind(TreePlacementAsyncContainer scratch, BiomeHandle biome, BiomeTreeStruct kind) {

        TreeHandle treeHandle = treeManager.findTreeHandle(kind.getTreeName());

        if (treeHandle == null)
            return;

        int reach = (int) Math.ceil(treeHandle.getReachBlocks() + kind.getSpreadBlocks());
        int spacing = kind.getSpacingBlocks();

        StructurePlacementUtility.collectCells(
                scratch.chunkOriginX - reach, scratch.chunkOriginX + chunkSize - 1 + reach,
                scratch.worldHandle.getWorldScale().x, spacing, scratch.cellsX);

        StructurePlacementUtility.collectCells(
                scratch.chunkOriginZ - reach, scratch.chunkOriginZ + chunkSize - 1 + reach,
                scratch.worldHandle.getWorldScale().y, spacing, scratch.cellsZ);

        for (int indexZ = 0; indexZ < scratch.cellsZ.size(); indexZ++)
            for (int indexX = 0; indexX < scratch.cellsX.size(); indexX++)
                placeCell(scratch, biome, kind, treeHandle, scratch.cellsX.getInt(indexX),
                        scratch.cellsZ.getInt(indexZ));
    }

    private void placeCell(
            TreePlacementAsyncContainer scratch,
            BiomeHandle biome,
            BiomeTreeStruct kind,
            TreeHandle treeHandle,
            int cellX,
            int cellZ) {

        long anchorX = StructurePlacementUtility.computeCellAnchor(cellX, kind.getSpacingBlocks(), 0,
                roll(scratch, kind, cellX, cellZ, EngineSetting.TREE_OFFSET_X_SALT));
        long anchorZ = StructurePlacementUtility.computeCellAnchor(cellZ, kind.getSpacingBlocks(), 0,
                roll(scratch, kind, cellX, cellZ, EngineSetting.TREE_OFFSET_Z_SALT));
        float chanceRoll = roll(scratch, kind, cellX, cellZ, EngineSetting.TREE_CHANCE_SALT);

        switch (kind.getDistribution()) {
            case SCATTERED -> {
                if (chanceRoll < kind.getChance())
                    placeTree(scratch, biome, treeHandle, anchorX, anchorZ);
            }
            case FIELD -> {
                if (chanceRoll < kind.getChance()
                        && TreeDistributionUtility.isInPatch(scratch.worldHandle.getSeed(), kind, anchorX, anchorZ))
                    placeTree(scratch, biome, treeHandle, anchorX, anchorZ);
            }
            case CLUSTERED -> {
                if (chanceRoll < kind.getChance())
                    placeCluster(scratch, biome, kind, treeHandle, cellX, cellZ, anchorX, anchorZ);
            }
        }
    }

    // A grove's trees, each spread from its centre by its own rolls, denser toward the middle
    private void placeCluster(
            TreePlacementAsyncContainer scratch,
            BiomeHandle biome,
            BiomeTreeStruct kind,
            TreeHandle treeHandle,
            int cellX,
            int cellZ,
            long centerX,
            long centerZ) {

        int range = kind.getMaxClusterTrees() - kind.getMinClusterTrees() + 1;
        int count = kind.getMinClusterTrees() + Math.min(range - 1,
                (int) (roll(scratch, kind, cellX, cellZ, EngineSetting.TREE_CLUSTER_COUNT_SALT) * range));

        for (int i = 0; i < count; i++) {

            long salt = EngineSetting.TREE_CLUSTER_SPREAD_SALT * (i + 1);
            float distance = kind.getClusterRadiusBlocks()
                    * (float) Math.sqrt(roll(scratch, kind, cellX, cellZ, salt));
            double angle = roll(scratch, kind, cellX, cellZ, salt ^ EngineSetting.TREE_CLUSTER_ANGLE_SALT)
                    * Math.PI * 2.0;

            placeTree(scratch, biome, treeHandle,
                    centerX + Math.round(Math.cos(angle) * distance),
                    centerZ + Math.round(Math.sin(angle) * distance));
        }
    }

    // Tree \\

    private void placeTree(
            TreePlacementAsyncContainer scratch,
            BiomeHandle biome,
            TreeHandle treeHandle,
            long rawAnchorX,
            long rawAnchorZ) {

        WorldHandle worldHandle = scratch.worldHandle;
        long anchorX = WorldWrapUtility.wrapBlockX(worldHandle, rawAnchorX);
        long anchorZ = WorldWrapUtility.wrapBlockZ(worldHandle, rawAnchorZ);

        if (!reachesChunk(scratch, treeHandle, anchorX, anchorZ))
            return;

        long key = TreeInstance.toRegistryKey(anchorX, anchorZ, treeHandle.getTreeID());

        if (holds(scratch, key))
            return;

        biomeManager.sampleBiomeField(worldHandle, anchorX + 0.5, anchorZ + 0.5, scratch.blend);

        BiomeHandle anchorBiome = scratch.blend.getDominantBiome();

        if (anchorBiome == null || anchorBiome.getBiomeID() != biome.getBiomeID())
            return;

        if (settlementManager.isClaimed(
                worldHandle,
                anchorX + EngineSetting.BLOCK_CENTER_OFFSET,
                anchorZ + EngineSetting.BLOCK_CENTER_OFFSET,
                EngineSetting.SETTLEMENT_TREE_CLEARANCE_BLOCKS))
            return;

        int ground = probeGround(scratch, anchorX, anchorZ);

        if (ground == EngineSetting.TREE_GROUND_FLOODED)
            return;

        TreeInstance tree = treeRegistryBranch.acquire(key);

        if (tree == null)
            tree = treeRegistryBranch.register(growTree(scratch, treeHandle, anchorX, anchorZ, ground + 1));

        scratch.trees.add(tree);
    }

    private boolean reachesChunk(
            TreePlacementAsyncContainer scratch,
            TreeHandle treeHandle,
            long anchorX,
            long anchorZ) {

        int reach = (int) Math.ceil(treeHandle.getReachBlocks());
        long relativeX = WorldWrapUtility.wrappedBlockDeltaX(scratch.worldHandle, anchorX, scratch.chunkOriginX);
        long relativeZ = WorldWrapUtility.wrappedBlockDeltaZ(scratch.worldHandle, anchorZ, scratch.chunkOriginZ);

        return relativeX + reach >= 0 && relativeX - reach < chunkSize
                && relativeZ + reach >= 0 && relativeZ - reach < chunkSize;
    }

    private boolean holds(TreePlacementAsyncContainer scratch, long key) {

        for (int i = 0; i < scratch.trees.size(); i++)
            if (scratch.trees.get(i).getRegistryKey() == key)
                return true;

        return false;
    }

    // A wild tree grown from its own seed to its own age within the species' wild range
    private TreeInstance growTree(
            TreePlacementAsyncContainer scratch,
            TreeHandle treeHandle,
            long anchorX,
            long anchorZ,
            int baseY) {

        long seed = BiomeFieldUtility.hashCell(
                scratch.worldHandle.getSeed() ^ EngineSetting.TREE_SEED_SALT
                        ^ (treeHandle.getNameSeed() * EngineSetting.STRUCTURE_NAME_SEED_MULTIPLIER),
                (int) anchorX, (int) anchorZ);
        float ageRoll = BiomeFieldUtility.hash01(seed ^ EngineSetting.TREE_AGE_SALT);
        float age = treeHandle.getGrowth().getWildMinAge()
                + (treeHandle.getGrowth().getWildMaxAge() - treeHandle.getGrowth().getWildMinAge()) * ageRoll;
        double plantedDay = scratch.currentDay - age * treeHandle.getGrowth().getDays();

        TreeInstance tree = create(TreeInstance.class);
        tree.constructor(treeHandle, scratch.worldHandle, anchorX, anchorZ, baseY, seed, plantedDay, false,
                scratch.currentDay);

        return tree;
    }

    // Planted \\

    // Every tree a hand planted in this world that can reach the chunk, held once more for it
    private void placePlanted(TreePlacementAsyncContainer scratch) {

        treeRegistryBranch.collectPlanted(scratch.planted);

        for (int i = 0; i < scratch.planted.size(); i++) {

            TreeInstance tree = scratch.planted.get(i);

            if (tree.getWorldHandle() != scratch.worldHandle
                    || !reachesChunk(scratch, tree.getTreeHandle(), tree.getAnchorX(), tree.getAnchorZ())
                    || holds(scratch, tree.getRegistryKey()))
                continue;

            treeRegistryBranch.hold(tree);
            scratch.trees.add(tree);
        }
    }

    // Ground \\

    // The top solid block under an anchor, TREE_GROUND_FLOODED where water stands over it
    private int probeGround(TreePlacementAsyncContainer scratch, long anchorX, long anchorZ) {

        long key = anchorX << Integer.SIZE | (anchorZ & 0xFFFFFFFFL);
        int ground = scratch.anchor2Ground.get(key);

        if (ground != EngineSetting.TREE_GROUND_UNKNOWN)
            return ground;

        ground = worldGenerationManager.probeFlooded(scratch.worldHandle, anchorX, anchorZ)
                ? EngineSetting.TREE_GROUND_FLOODED
                : worldGenerationManager.probeGroundHeight(scratch.worldHandle, anchorX, anchorZ);

        scratch.anchor2Ground.put(key, ground);

        return ground;
    }

    // Utility \\

    private float roll(TreePlacementAsyncContainer scratch, BiomeTreeStruct kind, int cellX, int cellZ, long salt) {
        return StructurePlacementUtility.rollCell(
                scratch.worldHandle.getSeed() ^ EngineSetting.TREE_PLACEMENT_SALT, kind.getNameSeed(),
                cellX, cellZ, salt);
    }
}
