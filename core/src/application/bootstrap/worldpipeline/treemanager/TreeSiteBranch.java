package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.BiomeTreeStruct;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeSiteStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.util.TreeDistributionUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TreeSiteBranch extends BranchPackage {

    /*
     * Async — where the world roots its wild trees, the one answer chunk
     * placement and distant terrain both read, on whichever thread asks. The
     * biomes around a region are sampled first, so only the kinds of tree
     * they grow are walked. Each kind lays its placement cells over the
     * region: a scattered kind rolls one tree per cell, a clustered kind one
     * grove per cell spread inside its cluster radius, and a field kind a
     * tree in nearly every cell, thinned by a slow patch noise into woods and
     * clearings; a site stands only where its own biome holds its root and no
     * settlement or road claims the ground around it. Every tree then keeps a
     * clearance around its root, and of two sites closer than their
     * clearances together only the one that outranks the other stands, so no
     * two trees ever root inside one another however their kinds and groves
     * overlap. A site is judged against every site within a full clearance of
     * it, rolled past the region asked for, and every roll is a pure function
     * of the world seed, the tree and the place, so every caller asking about
     * one site finds the same answer on its own. Sites are handed back from
     * the thread's own scratch, read before its next search.
     */

    // Internal
    private TreeManager treeManager;
    private BiomeManager biomeManager;
    private SettlementManager settlementManager;
    private TreeSiteAsyncContainer siteContainer;

    // Base \\

    @Override
    protected void create() {
        this.siteContainer = create(TreeSiteAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.settlementManager = get(SettlementManager.class);
    }

    // Sites \\

    // Every wild tree rooted in a block region of the wrapping world that stands once its neighbours are judged
    void collectSites(
            WorldHandle worldHandle,
            long minX,
            long minZ,
            long maxX,
            long maxZ,
            ObjectArrayList<TreeSiteStruct> out) {

        TreeSiteAsyncContainer scratch = siteContainer.getInstance();
        int margin = (int) Math.ceil(treeManager.getMaxClearanceBlocks() * 2f);

        scratch.reset();
        scratch.worldHandle = worldHandle;
        scratch.minX = minX - margin;
        scratch.minZ = minZ - margin;
        scratch.spanX = (int) (maxX - minX + 1) + margin * 2;
        scratch.spanZ = (int) (maxZ - minZ + 1) + margin * 2;

        out.clear();
        gatherBiomes(scratch);

        for (int i = 0; i < scratch.biomes.size(); i++) {

            BiomeHandle biome = scratch.biomes.get(i);
            ObjectArrayList<BiomeTreeStruct> kinds = biome.getTrees();

            for (int k = 0; k < kinds.size(); k++)
                placeKind(scratch, biome, kinds.get(k));
        }

        bucketSites(scratch, Math.max(margin, 1));

        for (int i = 0; i < scratch.siteCount; i++) {

            TreeSiteStruct site = scratch.sites.get(i);

            if (!isInside(site, margin, scratch.spanX - margin, scratch.spanZ - margin) || !stands(scratch, i))
                continue;

            site.setAge(resolveWildAge(site));
            out.add(site);
        }
    }

    // Every biome with trees standing within reach of the widened region, sampled on a lattice the whole world shares
    private void gatherBiomes(TreeSiteAsyncContainer scratch) {

        int spread = EngineSetting.BIOME_MAX_TREE_CLUSTER_RADIUS_BLOCKS;
        int spacing = EngineSetting.TREE_BIOME_SAMPLE_SPACING_BLOCKS;
        long firstX = Math.floorDiv(scratch.minX - spread, spacing) * spacing;
        long firstZ = Math.floorDiv(scratch.minZ - spread, spacing) * spacing;
        long lastX = scratch.minX + scratch.spanX + spread;
        long lastZ = scratch.minZ + scratch.spanZ + spread;

        for (long z = firstZ; z <= lastZ; z += spacing)
            for (long x = firstX; x <= lastX; x += spacing) {

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

    private void placeKind(TreeSiteAsyncContainer scratch, BiomeHandle biome, BiomeTreeStruct kind) {

        TreeHandle treeHandle = treeManager.findTreeHandle(kind.getTreeName());

        if (treeHandle == null)
            return;

        int spread = (int) Math.ceil(kind.getSpreadBlocks());
        int spacing = kind.getSpacingBlocks();
        float clearance = TreeDistributionUtility.resolveClearance(treeHandle);

        StructurePlacementUtility.collectCells(
                scratch.minX - spread, scratch.minX + scratch.spanX - 1 + spread,
                scratch.worldHandle.getWorldScale().x, spacing, scratch.cellsX);

        StructurePlacementUtility.collectCells(
                scratch.minZ - spread, scratch.minZ + scratch.spanZ - 1 + spread,
                scratch.worldHandle.getWorldScale().y, spacing, scratch.cellsZ);

        for (int indexZ = 0; indexZ < scratch.cellsZ.size(); indexZ++)
            for (int indexX = 0; indexX < scratch.cellsX.size(); indexX++)
                placeCell(scratch, biome, kind, treeHandle, clearance, scratch.cellsX.getInt(indexX),
                        scratch.cellsZ.getInt(indexZ));
    }

    private void placeCell(
            TreeSiteAsyncContainer scratch,
            BiomeHandle biome,
            BiomeTreeStruct kind,
            TreeHandle treeHandle,
            float clearance,
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
                    placeSite(scratch, biome, treeHandle, clearance, anchorX, anchorZ);
            }
            case FIELD -> {
                if (chanceRoll < kind.getChance()
                        && TreeDistributionUtility.isInPatch(scratch.worldHandle.getSeed(), kind, anchorX, anchorZ))
                    placeSite(scratch, biome, treeHandle, clearance, anchorX, anchorZ);
            }
            case CLUSTERED -> {
                if (chanceRoll < kind.getChance())
                    placeCluster(scratch, biome, kind, treeHandle, clearance, cellX, cellZ, anchorX, anchorZ);
            }
        }
    }

    // A grove's trees, each spread from its centre by its own rolls, denser toward the middle
    private void placeCluster(
            TreeSiteAsyncContainer scratch,
            BiomeHandle biome,
            BiomeTreeStruct kind,
            TreeHandle treeHandle,
            float clearance,
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

            placeSite(scratch, biome, treeHandle, clearance,
                    centerX + Math.round(Math.cos(angle) * distance),
                    centerZ + Math.round(Math.sin(angle) * distance));
        }
    }

    // Site \\

    // A rolled root kept as a site when it lies in the widened region, no site took its key, its biome holds it and
    // no settlement or road claims it
    private void placeSite(
            TreeSiteAsyncContainer scratch,
            BiomeHandle biome,
            TreeHandle treeHandle,
            float clearance,
            long rawAnchorX,
            long rawAnchorZ) {

        WorldHandle worldHandle = scratch.worldHandle;
        long anchorX = WorldWrapUtility.wrapBlockX(worldHandle, rawAnchorX);
        long anchorZ = WorldWrapUtility.wrapBlockZ(worldHandle, rawAnchorZ);
        int relativeX = (int) WorldWrapUtility.wrappedBlockDeltaX(worldHandle, anchorX, scratch.minX);
        int relativeZ = (int) WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, anchorZ, scratch.minZ);

        if (relativeX < 0 || relativeX >= scratch.spanX || relativeZ < 0 || relativeZ >= scratch.spanZ)
            return;

        if (scratch.siteKeys.contains(TreeInstance.toRegistryKey(anchorX, anchorZ, treeHandle.getTreeID())))
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

        TreeSiteStruct site = scratch.nextSite();
        site.set(treeHandle, anchorX, anchorZ, relativeX, relativeZ, clearance,
                resolveWildSeed(worldHandle, treeHandle, anchorX, anchorZ));
        scratch.siteKeys.add(site.getRegistryKey());
    }

    private boolean isInside(TreeSiteStruct site, int min, int maxX, int maxZ) {
        return site.getRelativeX() >= min && site.getRelativeX() < maxX
                && site.getRelativeZ() >= min && site.getRelativeZ() < maxZ;
    }

    // Room \\

    // Every rolled site sorted into the bucket its root lies in, each bucket a list threaded through bucketNext
    private void bucketSites(TreeSiteAsyncContainer scratch, int bucketSize) {

        scratch.prepareBuckets(bucketSize);

        for (int i = 0; i < scratch.siteCount; i++) {

            int bucket = resolveBucket(scratch, scratch.sites.get(i));

            scratch.bucketNext[i] = scratch.bucketHeads[bucket];
            scratch.bucketHeads[bucket] = i;
        }
    }

    private int resolveBucket(TreeSiteAsyncContainer scratch, TreeSiteStruct site) {
        return site.getRelativeZ() / scratch.bucketSize * scratch.bucketsX + site.getRelativeX() / scratch.bucketSize;
    }

    // True when no site close enough to crowd this one outranks it
    private boolean stands(TreeSiteAsyncContainer scratch, int index) {

        TreeSiteStruct site = scratch.sites.get(index);
        int bucketX = site.getRelativeX() / scratch.bucketSize;
        int bucketZ = site.getRelativeZ() / scratch.bucketSize;
        int lastX = Math.min(bucketX + 1, scratch.bucketsX - 1);
        int lastZ = Math.min(bucketZ + 1, scratch.bucketsZ - 1);

        for (int z = Math.max(bucketZ - 1, 0); z <= lastZ; z++)
            for (int x = Math.max(bucketX - 1, 0); x <= lastX; x++)
                for (int other = scratch.bucketHeads[z * scratch.bucketsX + x];
                        other != EngineSetting.INDEX_NOT_FOUND;
                        other = scratch.bucketNext[other])
                    if (other != index && crowds(scratch.sites.get(other), site))
                        return false;

        return true;
    }

    // True when one site stands closer to another than their clearances together allow, and outranks it
    private boolean crowds(TreeSiteStruct crowding, TreeSiteStruct site) {

        int deltaX = crowding.getRelativeX() - site.getRelativeX();
        int deltaZ = crowding.getRelativeZ() - site.getRelativeZ();
        float room = crowding.getClearanceBlocks() + site.getClearanceBlocks();

        return deltaX * deltaX + deltaZ * deltaZ < room * room && crowding.outranks(site);
    }

    // Growth \\

    // The seed a wild tree grows from, its own for its species and its root column
    private long resolveWildSeed(WorldHandle worldHandle, TreeHandle treeHandle, long anchorX, long anchorZ) {
        return BiomeFieldUtility.hashCell(
                worldHandle.getSeed() ^ EngineSetting.TREE_SEED_SALT
                        ^ (treeHandle.getNameSeed() * EngineSetting.STRUCTURE_NAME_SEED_MULTIPLIER),
                (int) anchorX, (int) anchorZ);
    }

    // The age a wild tree is found at, rolled from its seed within the species' wild range
    private float resolveWildAge(TreeSiteStruct site) {

        TreeHandle treeHandle = site.getTreeHandle();
        float ageRoll = BiomeFieldUtility.hash01(site.getSeed() ^ EngineSetting.TREE_AGE_SALT);

        return treeHandle.getGrowth().getWildMinAge()
                + (treeHandle.getGrowth().getWildMaxAge() - treeHandle.getGrowth().getWildMinAge()) * ageRoll;
    }

    // Utility \\

    private float roll(TreeSiteAsyncContainer scratch, BiomeTreeStruct kind, int cellX, int cellZ, long salt) {
        return StructurePlacementUtility.rollCell(
                scratch.worldHandle.getSeed() ^ EngineSetting.TREE_PLACEMENT_SALT, kind.getNameSeed(),
                cellX, cellZ, salt);
    }
}
