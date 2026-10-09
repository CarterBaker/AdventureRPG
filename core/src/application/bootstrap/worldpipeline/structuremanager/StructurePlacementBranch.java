package application.bootstrap.worldpipeline.structuremanager;

import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.structure.StructureFixedPlacementStruct;
import application.bootstrap.worldpipeline.structure.StructureFrequencyStruct;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structure.StructureRulesStruct;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StructurePlacementBranch extends BranchPackage {

    /*
     * Async — stamps every structure that places itself or was placed by hand
     * and reaches one freshly generated chunk into it, on that chunk's own
     * worker thread. Each chunk independently re-derives every candidate that
     * could reach it and lays only its own share through
     * StructureStampBranch, so a structure spanning several chunks lands
     * identically in each without any chunk touching another. Cheap
     * rejections run first: the chance roll, then reach, then biome, then the
     * ground probes, and a structure that places itself never lands on ground
     * a settlement or road claims.
     */

    // Internal
    private StructureManager structureManager;
    private BiomeManager biomeManager;
    private WorldGenerationManager worldGenerationManager;
    private SettlementManager settlementManager;
    private StructureStampBranch structureStampBranch;
    private StructurePlacementAsyncContainer placementContainer;

    // Settings
    private int chunkSize;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.placementContainer = create(StructurePlacementAsyncContainer.class);

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.structureManager = get(StructureManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.settlementManager = get(SettlementManager.class);
        this.structureStampBranch = get(StructureStampBranch.class);
    }

    // Generation \\

    void generateStructures(
            WorldHandle worldHandle,
            long chunkCoordinate,
            SubChunkInstance[] subChunks,
            StructureHandle[] structureHandles,
            WorldPlacementStruct[] placements) {

        StructurePlacementAsyncContainer scratch = placementContainer.getInstance();

        scratch.worldHandle = worldHandle;
        scratch.chunkCoordinate = chunkCoordinate;
        scratch.chunkOriginX = (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize;
        scratch.chunkOriginZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize;
        scratch.subChunks = subChunks;

        for (int i = 0; i < structureHandles.length; i++) {

            StructureHandle structureHandle = structureHandles[i];

            if (structureHandle.hasFrequency())
                generateFrequencyPlacements(scratch, structureHandle);

            generateFixedPlacements(scratch, structureHandle);
        }

        generateWorldPlacements(scratch, placements);
    }

    // Frequency Placement \\

    private void generateFrequencyPlacements(
            StructurePlacementAsyncContainer scratch,
            StructureHandle structureHandle) {

        int reach = structureHandle.getHorizontalReachBlocks();
        int spacingBlocks = structureHandle.getFrequency().getSpacingBlocks();

        StructurePlacementUtility.collectCells(
                scratch.chunkOriginX - reach,
                scratch.chunkOriginX + chunkSize - 1 + reach,
                scratch.worldHandle.getWorldScale().x,
                spacingBlocks,
                scratch.cellsX);

        StructurePlacementUtility.collectCells(
                scratch.chunkOriginZ - reach,
                scratch.chunkOriginZ + chunkSize - 1 + reach,
                scratch.worldHandle.getWorldScale().y,
                spacingBlocks,
                scratch.cellsZ);

        for (int indexZ = 0; indexZ < scratch.cellsZ.size(); indexZ++)
            for (int indexX = 0; indexX < scratch.cellsX.size(); indexX++)
                generateCellPlacement(
                        scratch, structureHandle,
                        scratch.cellsX.getInt(indexX), scratch.cellsZ.getInt(indexZ));
    }

    private void generateCellPlacement(
            StructurePlacementAsyncContainer scratch,
            StructureHandle structureHandle,
            int cellX,
            int cellZ) {

        StructureFrequencyStruct frequency = structureHandle.getFrequency();
        int nameSeed = structureHandle.getNameSeed();
        long seed = scratch.worldHandle.getSeed();

        float chanceRoll = StructurePlacementUtility.rollCell(
                seed, nameSeed, cellX, cellZ, EngineSetting.STRUCTURE_CHANCE_SALT);

        if (chanceRoll >= frequency.getChance())
            return;

        long anchorX = StructurePlacementUtility.computeCellAnchor(
                cellX, frequency.getSpacingBlocks(), frequency.getSeparationBlocks(),
                StructurePlacementUtility.rollCell(seed, nameSeed, cellX, cellZ,
                        EngineSetting.STRUCTURE_OFFSET_X_SALT));

        long anchorZ = StructurePlacementUtility.computeCellAnchor(
                cellZ, frequency.getSpacingBlocks(), frequency.getSeparationBlocks(),
                StructurePlacementUtility.rollCell(seed, nameSeed, cellX, cellZ,
                        EngineSetting.STRUCTURE_OFFSET_Z_SALT));

        int quarterTurns = frequency.hasRandomRotation()
                ? StructurePlacementUtility.rollQuarterTurns(StructurePlacementUtility.rollCell(
                        seed, nameSeed, cellX, cellZ, EngineSetting.STRUCTURE_ROTATION_SALT))
                : 0;

        if (!reachesChunk(scratch, structureHandle, anchorX, anchorZ))
            return;

        if (!passesRules(scratch, structureHandle, anchorX, anchorZ, quarterTurns))
            return;

        if (settlementManager.isClaimed(
                scratch.worldHandle, anchorX, anchorZ, structureHandle.getHorizontalReachBlocks()))
            return;

        stampStructure(
                scratch, structureHandle, anchorX, anchorZ,
                resolveGroundAnchorY(scratch, structureHandle, anchorX, anchorZ), quarterTurns);
    }

    // Fixed Placement \\

    private void generateFixedPlacements(StructurePlacementAsyncContainer scratch, StructureHandle structureHandle) {

        ObjectArrayList<StructureFixedPlacementStruct> fixedPlacements = structureHandle.getFixedPlacements();

        for (int i = 0; i < fixedPlacements.size(); i++)
            generateFixedPlacement(scratch, structureHandle, fixedPlacements.get(i));
    }

    private void generateFixedPlacement(
            StructurePlacementAsyncContainer scratch,
            StructureHandle structureHandle,
            StructureFixedPlacementStruct placement) {

        long anchorX = WorldWrapUtility.wrapBlockX(scratch.worldHandle, placement.getWorldX());
        long anchorZ = WorldWrapUtility.wrapBlockZ(scratch.worldHandle, placement.getWorldZ());
        int quarterTurns = placement.getQuarterTurns();

        if (!reachesChunk(scratch, structureHandle, anchorX, anchorZ))
            return;

        if (placement.isEnforceRules() && !passesRules(scratch, structureHandle, anchorX, anchorZ, quarterTurns))
            return;

        int anchorY = placement.hasWorldY()
                ? placement.getWorldY()
                : resolveGroundAnchorY(scratch, structureHandle, anchorX, anchorZ);

        stampStructure(scratch, structureHandle, anchorX, anchorZ, anchorY, quarterTurns);
    }

    // World Placement \\

    // Every structure the world's placements stand up by hand, unruled, anchored on the ground
    private void generateWorldPlacements(StructurePlacementAsyncContainer scratch, WorldPlacementStruct[] placements) {

        for (int i = 0; i < placements.length; i++) {

            WorldPlacementStruct placement = placements[i];

            if (placement.getKind() != WorldPlacementKind.STRUCTURE)
                continue;

            StructureHandle structureHandle = structureManager.getStructureHandleFromStructureName(placement.getName());
            long anchorX = WorldWrapUtility.wrapBlockX(scratch.worldHandle, placement.getWorldX());
            long anchorZ = WorldWrapUtility.wrapBlockZ(scratch.worldHandle, placement.getWorldZ());

            if (!reachesChunk(scratch, structureHandle, anchorX, anchorZ))
                continue;

            stampStructure(
                    scratch, structureHandle, anchorX, anchorZ,
                    resolveGroundAnchorY(scratch, structureHandle, anchorX, anchorZ), placement.getQuarterTurns());
        }
    }

    // Reach \\

    private boolean reachesChunk(
            StructurePlacementAsyncContainer scratch,
            StructureHandle structureHandle,
            long anchorX,
            long anchorZ) {

        int reach = structureHandle.getHorizontalReachBlocks();

        long relativeX = WorldWrapUtility.wrappedBlockDeltaX(scratch.worldHandle, anchorX, scratch.chunkOriginX);
        long relativeZ = WorldWrapUtility.wrappedBlockDeltaZ(scratch.worldHandle, anchorZ, scratch.chunkOriginZ);

        return relativeX + reach >= 0 && relativeX - reach < chunkSize
                && relativeZ + reach >= 0 && relativeZ - reach < chunkSize;
    }

    // Rules \\

    private boolean passesRules(
            StructurePlacementAsyncContainer scratch,
            StructureHandle structureHandle,
            long anchorX,
            long anchorZ,
            int quarterTurns) {

        StructureRulesStruct rules = structureHandle.getRules();

        return passesBiomeRule(scratch, rules, anchorX, anchorZ)
                && passesSurfaceRule(scratch, rules, anchorX, anchorZ)
                && passesHeightRule(scratch, rules, anchorX, anchorZ)
                && passesSlopeRule(scratch, structureHandle, rules, anchorX, anchorZ, quarterTurns);
    }

    private boolean passesBiomeRule(
            StructurePlacementAsyncContainer scratch,
            StructureRulesStruct rules,
            long anchorX,
            long anchorZ) {

        if (!rules.hasBiomeRule())
            return true;

        biomeManager.sampleBiomeField(scratch.worldHandle, anchorX, anchorZ, scratch.anchorBlend);

        return rules.getBiomeIDs().contains(scratch.anchorBlend.getDominantBiome().getBiomeID());
    }

    private boolean passesSurfaceRule(
            StructurePlacementAsyncContainer scratch,
            StructureRulesStruct rules,
            long anchorX,
            long anchorZ) {
        return switch (rules.getSurfaceType()) {
            case LAND -> !worldGenerationManager.probeFlooded(scratch.worldHandle, anchorX, anchorZ);
            case UNDERWATER -> worldGenerationManager.probeFlooded(scratch.worldHandle, anchorX, anchorZ);
            case ANY -> true;
        };
    }

    private boolean passesHeightRule(
            StructurePlacementAsyncContainer scratch,
            StructureRulesStruct rules,
            long anchorX,
            long anchorZ) {

        int groundHeight = worldGenerationManager.probeGroundHeight(scratch.worldHandle, anchorX, anchorZ);

        return groundHeight >= rules.getMinGroundHeightBlocks() && groundHeight <= rules.getMaxGroundHeightBlocks();
    }

    private boolean passesSlopeRule(
            StructurePlacementAsyncContainer scratch,
            StructureHandle structureHandle,
            StructureRulesStruct rules,
            long anchorX,
            long anchorZ,
            int quarterTurns) {

        if (!rules.isSlopeLimited())
            return true;

        int minX = structureHandle.getMinOffsetX();
        int maxX = structureHandle.getMaxOffsetX();
        int minZ = structureHandle.getMinOffsetZ();
        int maxZ = structureHandle.getMaxOffsetZ();

        int height00 = probeFootprintHeight(scratch, anchorX, anchorZ, minX, minZ, quarterTurns);
        int height10 = probeFootprintHeight(scratch, anchorX, anchorZ, maxX, minZ, quarterTurns);
        int height01 = probeFootprintHeight(scratch, anchorX, anchorZ, minX, maxZ, quarterTurns);
        int height11 = probeFootprintHeight(scratch, anchorX, anchorZ, maxX, maxZ, quarterTurns);
        int anchorHeight = worldGenerationManager.probeGroundHeight(scratch.worldHandle, anchorX, anchorZ);

        int lowest = Math.min(Math.min(Math.min(height00, height10), Math.min(height01, height11)), anchorHeight);
        int highest = Math.max(Math.max(Math.max(height00, height10), Math.max(height01, height11)), anchorHeight);

        return highest - lowest <= rules.getMaxSlopeBlocks();
    }

    private int probeFootprintHeight(
            StructurePlacementAsyncContainer scratch,
            long anchorX,
            long anchorZ,
            int offsetX,
            int offsetZ,
            int quarterTurns) {
        return worldGenerationManager.probeGroundHeight(
                scratch.worldHandle,
                anchorX + StructurePlacementUtility.rotateX(offsetX, offsetZ, quarterTurns),
                anchorZ + StructurePlacementUtility.rotateZ(offsetX, offsetZ, quarterTurns));
    }

    // Anchor \\

    private int resolveGroundAnchorY(
            StructurePlacementAsyncContainer scratch,
            StructureHandle structureHandle,
            long anchorX,
            long anchorZ) {

        int groundHeight = worldGenerationManager.probeGroundHeight(scratch.worldHandle, anchorX, anchorZ);

        return groundHeight + 1 + structureHandle.getYOffsetBlocks();
    }

    // Stamp \\

    private void stampStructure(
            StructurePlacementAsyncContainer scratch,
            StructureHandle structureHandle,
            long anchorX,
            long anchorZ,
            int anchorY,
            int quarterTurns) {
        structureStampBranch.stamp(
                scratch.worldHandle, scratch.chunkCoordinate, scratch.subChunks,
                structureHandle, anchorX, anchorZ, anchorY, quarterTurns);
    }
}
