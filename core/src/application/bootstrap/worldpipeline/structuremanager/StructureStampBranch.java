package application.bootstrap.worldpipeline.structuremanager;

import java.util.Arrays;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemrotationmanager.ItemRotationBufferSystem;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.ChunkWriteUtility;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate4Long;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StructureStampBranch extends BranchPackage {

    /*
     * Async — the one way a structure is laid into a chunk, whether it placed
     * itself, was placed by hand or stands on a settlement's lot. Only the
     * chunk's own columns are written, so every chunk a structure reaches lays
     * its share independently. A structure that clears terrain first carves
     * the ground standing inside its footprint down to its lowest block; its
     * blocks are then laid turned, orientation and sub-block mask alike, a
     * foundation is filled from its lowest solid blocks down to the ground,
     * and every place it comes furnished at whose item corner falls in the
     * chunk draws its furniture as a world item. Every roll is salted by the
     * world, the structure and its anchor, so a structure furnishes alike in
     * every chunk and every session.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;
    private FurnishingManager furnishingManager;
    private ItemRotationBufferSystem itemRotationBufferSystem;
    private StructureStampAsyncContainer stampContainer;

    // Settings
    private int chunkSize;
    private int subVoxelResolution;
    private int chunkSubVoxels;
    private int worldHeightSubVoxels;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.stampContainer = create(StructureStampAsyncContainer.class);

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.subVoxelResolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        this.chunkSubVoxels = EngineSetting.CHUNK_SIZE * EngineSetting.SUB_VOXEL_RESOLUTION;
        this.worldHeightSubVoxels = EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE
                * EngineSetting.SUB_VOXEL_RESOLUTION;
    }

    @Override
    protected void get() {
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.furnishingManager = get(FurnishingManager.class);
        this.itemRotationBufferSystem = get(ItemRotationBufferSystem.class);
    }

    // Stamp \\

    void stamp(
            WorldHandle worldHandle,
            long chunkCoordinate,
            SubChunkInstance[] subChunks,
            StructureHandle structureHandle,
            long anchorX,
            long anchorZ,
            int anchorY,
            int quarterTurns) {

        long chunkOriginX = (long) Coordinate2Long.unpackX(chunkCoordinate) * chunkSize;
        long chunkOriginZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * chunkSize;
        long relativeX = WorldWrapUtility.wrappedBlockDeltaX(worldHandle, anchorX, chunkOriginX);
        long relativeZ = WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, anchorZ, chunkOriginZ);

        if (structureHandle.isClearTerrain())
            clearFootprint(chunkCoordinate, subChunks, structureHandle, relativeX, relativeZ, anchorY, quarterTurns);

        layBlocks(chunkCoordinate, subChunks, structureHandle, relativeX, relativeZ, anchorY, quarterTurns);

        long placementSeed = BiomeFieldUtility.hashCell(
                worldHandle.getSeed() ^ EngineSetting.STRUCTURE_FURNISHING_SALT
                        ^ (structureHandle.getNameSeed() * EngineSetting.STRUCTURE_NAME_SEED_MULTIPLIER),
                (int) anchorX, (int) anchorZ);

        furnish(subChunks, structureHandle, relativeX, relativeZ, anchorY, quarterTurns, placementSeed);
    }

    // Terrain \\

    // The ground standing inside the footprint, carved from the structure's lowest block up to the ground's top
    private void clearFootprint(
            long chunkCoordinate,
            SubChunkInstance[] subChunks,
            StructureHandle structureHandle,
            long relativeX,
            long relativeZ,
            int anchorY,
            int quarterTurns) {

        boolean[] footprint = structureHandle.getFootprint();
        int minOffsetX = structureHandle.getMinOffsetX();
        int minOffsetZ = structureHandle.getMinOffsetZ();
        int width = structureHandle.getMaxOffsetX() - minOffsetX + 1;
        int floorY = anchorY + structureHandle.getMinOffsetY();

        for (int column = 0; column < footprint.length; column++) {

            if (!footprint[column])
                continue;

            int offsetX = minOffsetX + column % width;
            int offsetZ = minOffsetZ + column / width;
            long localX = relativeX + StructurePlacementUtility.rotateX(offsetX, offsetZ, quarterTurns);
            long localZ = relativeZ + StructurePlacementUtility.rotateZ(offsetX, offsetZ, quarterTurns);

            if (localX < 0 || localX >= chunkSize || localZ < 0 || localZ >= chunkSize)
                continue;

            int groundHeight = worldGenerationManager.getColumnGroundHeight(
                    chunkCoordinate, (int) localX, (int) localZ);

            ChunkWriteUtility.clearColumn(subChunks, (int) localX, floorY, groundHeight, (int) localZ);
        }
    }

    // Blocks \\

    private void layBlocks(
            long chunkCoordinate,
            SubChunkInstance[] subChunks,
            StructureHandle structureHandle,
            long relativeX,
            long relativeZ,
            int anchorY,
            int quarterTurns) {

        StructureStampAsyncContainer scratch = stampContainer.getInstance();

        int[] offsetX = structureHandle.getBlockOffsetX();
        int[] offsetY = structureHandle.getBlockOffsetY();
        int[] offsetZ = structureHandle.getBlockOffsetZ();
        short[] blockIDs = structureHandle.getBlockIDs();
        short[] blockOrientations = structureHandle.getBlockOrientations();
        byte[] blockMasks = structureHandle.getBlockMasks();
        short[] blockCoverages = structureHandle.getBlockCoverages();
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

            ChunkWriteUtility.writeBlock(
                    subChunks, (int) localX, worldY, (int) localZ, blockIDs[i],
                    StructurePlacementUtility.rotateOrientation(blockOrientations[i], quarterTurns),
                    StructurePlacementUtility.rotateMask(blockMasks[i] & EngineSetting.SUB_BLOCK_MASK_FULL,
                            quarterTurns),
                    geometry == DynamicGeometryType.LIQUID);
            ChunkWriteUtility.writeCoverage(subChunks, (int) localX, worldY, (int) localZ, blockCoverages[i]);

            if (foundation && geometry != DynamicGeometryType.NONE && geometry != DynamicGeometryType.LIQUID) {
                int columnIndex = (int) localZ * chunkSize + (int) localX;
                scratch.columnFloorY[columnIndex] = Math.min(scratch.columnFloorY[columnIndex], worldY);
            }
        }

        if (foundation)
            layFoundation(scratch, chunkCoordinate, subChunks, structureHandle.getFoundationBlockID());
    }

    private void layFoundation(
            StructureStampAsyncContainer scratch,
            long chunkCoordinate,
            SubChunkInstance[] subChunks,
            short foundationBlockID) {

        for (int localZ = 0; localZ < chunkSize; localZ++) {
            for (int localX = 0; localX < chunkSize; localX++) {

                int floorY = scratch.columnFloorY[localZ * chunkSize + localX];

                if (floorY == Integer.MAX_VALUE)
                    continue;

                int groundHeight = worldGenerationManager.getColumnGroundHeight(chunkCoordinate, localX, localZ);

                for (int worldY = floorY - 1; worldY > groundHeight; worldY--)
                    ChunkWriteUtility.writeSolid(subChunks, localX, worldY, localZ, foundationBlockID);
            }
        }
    }

    // Furnishing \\

    // Every place furnished by its chance draws one item, stood as a world item where its turned corner falls
    private void furnish(
            SubChunkInstance[] subChunks,
            StructureHandle structureHandle,
            long relativeX,
            long relativeZ,
            int anchorY,
            int quarterTurns,
            long placementSeed) {

        ObjectArrayList<FurnishingSlotStruct> slots = structureHandle.getFurnishings();

        for (int i = 0; i < slots.size(); i++) {

            FurnishingSlotStruct slot = slots.get(i);

            long subX = relativeX * subVoxelResolution
                    + StructurePlacementUtility.rotateX(slot.getCornerX(), slot.getCornerZ(), quarterTurns);
            long subZ = relativeZ * subVoxelResolution
                    + StructurePlacementUtility.rotateZ(slot.getCornerX(), slot.getCornerZ(), quarterTurns);
            int subY = anchorY * subVoxelResolution + slot.getCornerY();

            if (subX < 0 || subX >= chunkSubVoxels || subZ < 0 || subZ >= chunkSubVoxels
                    || subY < 0 || subY >= worldHeightSubVoxels)
                continue;

            long slotSeed = BiomeFieldUtility.hashCell(placementSeed, i, slot.getFurnishingID());

            if (BiomeFieldUtility.hash01(slotSeed) >= slot.getChance())
                continue;

            ItemDefinitionHandle item = furnishingManager.drawItem(
                    slot.getFurnishingID(),
                    BiomeFieldUtility.hash01(slotSeed ^ EngineSetting.STRUCTURE_FURNISHING_ITEM_SALT));
            int orientation = itemRotationBufferSystem.turnOrientation(slot.getOrientation(), quarterTurns);

            subChunks[subY / subVoxelResolution / chunkSize].getWorldItemPaletteHandle().addItem(new WorldItemStruct(
                    Coordinate4Long.pack((int) subX, subY, (int) subZ, orientation), item.getItemID(), null));
        }
    }
}
