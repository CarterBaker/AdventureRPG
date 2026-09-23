package application.bootstrap.worldpipeline.structuremanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.structure.StructureFixedPlacementStruct;
import application.bootstrap.worldpipeline.structure.StructureFrequencyStruct;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structure.StructureRulesStruct;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StructurePlacementBranch extends BranchPackage {

    /*
     * Async — stamps every structure that reaches one freshly generated chunk
     * into it, on that chunk's own worker thread. Each chunk independently
     * re-derives every candidate that could reach it and writes only its own
     * blocks, so a structure spanning several chunks lands identically in
     * each without any chunk touching another. Cheap rejections run first:
     * the chance roll, then reach, then biome, then the ground probes.
     */

    // Internal
    private BiomeManager biomeManager;
    private WorldGenerationManager worldGenerationManager;
    private StructurePlacementAsyncContainer placementContainer;

    // Settings
    private int chunkSize;
    private int worldHeightBlocks;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.placementContainer = create(StructurePlacementAsyncContainer.class);

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.worldHeightBlocks = EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.biomeManager = get(BiomeManager.class);
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Generation \\

    void generateStructures(
            WorldHandle worldHandle,
            long chunkCoordinate,
            SubChunkInstance[] subChunks,
            StructureHandle[] structureHandles) {

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
        short structureID = structureHandle.getStructureID();
        long seed = scratch.worldHandle.getSeed();

        float chanceRoll = StructurePlacementUtility.rollCell(
                seed, structureID, cellX, cellZ, EngineSetting.STRUCTURE_CHANCE_SALT);

        if (chanceRoll >= frequency.getChance())
            return;

        long anchorX = StructurePlacementUtility.computeCellAnchor(
                cellX, frequency.getSpacingBlocks(), frequency.getSeparationBlocks(),
                StructurePlacementUtility.rollCell(seed, structureID, cellX, cellZ,
                        EngineSetting.STRUCTURE_OFFSET_X_SALT));

        long anchorZ = StructurePlacementUtility.computeCellAnchor(
                cellZ, frequency.getSpacingBlocks(), frequency.getSeparationBlocks(),
                StructurePlacementUtility.rollCell(seed, structureID, cellX, cellZ,
                        EngineSetting.STRUCTURE_OFFSET_Z_SALT));

        int quarterTurns = frequency.hasRandomRotation()
                ? StructurePlacementUtility.rollQuarterTurns(StructurePlacementUtility.rollCell(
                        seed, structureID, cellX, cellZ, EngineSetting.STRUCTURE_ROTATION_SALT))
                : 0;

        if (!reachesChunk(scratch, structureHandle, anchorX, anchorZ))
            return;

        if (!passesRules(scratch, structureHandle, anchorX, anchorZ, quarterTurns))
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

        long relativeX = WorldWrapUtility.wrappedBlockDeltaX(scratch.worldHandle, anchorX, scratch.chunkOriginX);
        long relativeZ = WorldWrapUtility.wrappedBlockDeltaZ(scratch.worldHandle, anchorZ, scratch.chunkOriginZ);

        int[] offsetX = structureHandle.getBlockOffsetX();
        int[] offsetY = structureHandle.getBlockOffsetY();
        int[] offsetZ = structureHandle.getBlockOffsetZ();
        short[] blockIDs = structureHandle.getBlockIDs();
        short[] blockOrientations = structureHandle.getBlockOrientations();
        DynamicGeometryType[] blockGeometry = structureHandle.getBlockGeometry();

        boolean foundation = structureHandle.hasFoundation();

        if (foundation)
            Arrays.fill(scratch.columnFloorY, Integer.MAX_VALUE);

        for (int i = 0; i < blockIDs.length; i++) {

            long localX = relativeX + StructurePlacementUtility.rotateX(offsetX[i], offsetZ[i], quarterTurns);
            long localZ = relativeZ + StructurePlacementUtility.rotateZ(offsetX[i], offsetZ[i], quarterTurns);

            if (localX < 0 || localX >= chunkSize || localZ < 0 || localZ >= chunkSize)
                continue;

            int worldY = anchorY + offsetY[i];
            DynamicGeometryType geometry = blockGeometry[i];

            writeBlock(
                    scratch.subChunks, (int) localX, worldY, (int) localZ, blockIDs[i],
                    StructurePlacementUtility.rotateOrientation(blockOrientations[i], quarterTurns),
                    geometry == DynamicGeometryType.LIQUID);

            if (foundation && geometry != DynamicGeometryType.NONE && geometry != DynamicGeometryType.LIQUID) {
                int columnIndex = (int) localZ * chunkSize + (int) localX;
                scratch.columnFloorY[columnIndex] = Math.min(scratch.columnFloorY[columnIndex], worldY);
            }
        }

        if (foundation)
            stampFoundation(scratch, structureHandle.getFoundationBlockID());
    }

    private void stampFoundation(StructurePlacementAsyncContainer scratch, short foundationBlockID) {

        for (int localZ = 0; localZ < chunkSize; localZ++) {
            for (int localX = 0; localX < chunkSize; localX++) {

                int floorY = scratch.columnFloorY[localZ * chunkSize + localX];

                if (floorY == Integer.MAX_VALUE)
                    continue;

                int groundHeight = worldGenerationManager.getColumnGroundHeight(
                        scratch.chunkCoordinate, localX, localZ);

                for (int worldY = floorY - 1; worldY > groundHeight; worldY--)
                    writeBlock(
                            scratch.subChunks, localX, worldY, localZ, foundationBlockID,
                            EngineSetting.DEFAULT_BLOCK_ORIENTATION, false);
            }
        }
    }

    private void writeBlock(
            SubChunkInstance[] subChunks,
            int localX,
            int worldY,
            int localZ,
            short blockID,
            short orientation,
            boolean liquid) {

        if (worldY < 0 || worldY >= worldHeightBlocks)
            return;

        SubChunkInstance subChunk = subChunks[worldY / chunkSize];
        int localY = worldY % chunkSize;

        subChunk.setBlock(localX, localY, localZ, blockID);
        subChunk.getBlockRotationPaletteHandle().setBlock(localX, localY, localZ, orientation);
        subChunk.setLiquidLevel(
                localX, localY, localZ,
                liquid ? EngineSetting.LIQUID_LEVEL_MAX : EngineSetting.LIQUID_LEVEL_EMPTY);
    }
}
