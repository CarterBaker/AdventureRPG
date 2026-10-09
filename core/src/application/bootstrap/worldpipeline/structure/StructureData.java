package application.bootstrap.worldpipeline.structure;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import engine.root.DataPackage;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureData extends DataPackage {

    /*
     * Persistent structure record. Blocks are parallel arrays of origin-relative
     * offsets with names already resolved to ID, orientation, sub-block mask
     * and geometry, so stamping is a straight walk over primitives. Unlisted
     * positions are left untouched; listed air carves, and a structure that
     * clears terrain also carves the ground standing inside its footprint.
     * The footprint grid spans the horizontal bounds and holds, per column,
     * whether any block is listed there and the top solid block with the map
     * colors distant terrain draws the column in. The front names the side
     * its entrance faces as authored, so a layout can turn it to face a road.
     * Placement rolls are salted by the name seed, so a structure lands in
     * the same cells whatever order it loads in.
     */

    // Identity
    private final String structureName;
    private final short structureID;
    private final int nameSeed;

    // Blocks
    private final int[] blockOffsetX;
    private final int[] blockOffsetY;
    private final int[] blockOffsetZ;
    private final short[] blockIDs;
    private final short[] blockOrientations;
    private final byte[] blockMasks;
    private final DynamicGeometryType[] blockGeometry;

    // Bounds
    private final int minOffsetX;
    private final int maxOffsetX;
    private final int minOffsetY;
    private final int maxOffsetY;
    private final int minOffsetZ;
    private final int maxOffsetZ;
    private final int horizontalReachBlocks;

    // Footprint — one entry per column of the horizontal bounds, x innermost
    private final boolean[] footprint;
    private final int[] footprintTopOffsetY;
    private final int[] footprintTopColors;
    private final int[] footprintSideColors;

    // Ground
    private final int yOffsetBlocks;
    private final short foundationBlockID;
    private final boolean foundation;
    private final boolean clearTerrain;

    // Orientation
    private final Direction3Vector front;

    // Furnishing
    private final ObjectArrayList<FurnishingSlotStruct> furnishings;

    // Placement
    private final StructureRulesStruct rules;
    private final StructureFrequencyStruct frequency;
    private final ObjectArrayList<StructureFixedPlacementStruct> fixedPlacements;

    // Constructor \\

    public StructureData(
            String structureName,
            short structureID,
            int nameSeed,
            int[] blockOffsetX,
            int[] blockOffsetY,
            int[] blockOffsetZ,
            short[] blockIDs,
            short[] blockOrientations,
            byte[] blockMasks,
            DynamicGeometryType[] blockGeometry,
            int minOffsetX,
            int maxOffsetX,
            int minOffsetY,
            int maxOffsetY,
            int minOffsetZ,
            int maxOffsetZ,
            int horizontalReachBlocks,
            boolean[] footprint,
            int[] footprintTopOffsetY,
            int[] footprintTopColors,
            int[] footprintSideColors,
            int yOffsetBlocks,
            short foundationBlockID,
            boolean foundation,
            boolean clearTerrain,
            Direction3Vector front,
            ObjectArrayList<FurnishingSlotStruct> furnishings,
            StructureRulesStruct rules,
            StructureFrequencyStruct frequency,
            ObjectArrayList<StructureFixedPlacementStruct> fixedPlacements) {

        // Identity
        this.structureName = structureName;
        this.structureID = structureID;
        this.nameSeed = nameSeed;

        // Blocks
        this.blockOffsetX = blockOffsetX;
        this.blockOffsetY = blockOffsetY;
        this.blockOffsetZ = blockOffsetZ;
        this.blockIDs = blockIDs;
        this.blockOrientations = blockOrientations;
        this.blockMasks = blockMasks;
        this.blockGeometry = blockGeometry;

        // Bounds
        this.minOffsetX = minOffsetX;
        this.maxOffsetX = maxOffsetX;
        this.minOffsetY = minOffsetY;
        this.maxOffsetY = maxOffsetY;
        this.minOffsetZ = minOffsetZ;
        this.maxOffsetZ = maxOffsetZ;
        this.horizontalReachBlocks = horizontalReachBlocks;

        // Footprint
        this.footprint = footprint;
        this.footprintTopOffsetY = footprintTopOffsetY;
        this.footprintTopColors = footprintTopColors;
        this.footprintSideColors = footprintSideColors;

        // Ground
        this.yOffsetBlocks = yOffsetBlocks;
        this.foundationBlockID = foundationBlockID;
        this.foundation = foundation;
        this.clearTerrain = clearTerrain;

        // Orientation
        this.front = front;

        // Furnishing
        this.furnishings = furnishings;

        // Placement
        this.rules = rules;
        this.frequency = frequency;
        this.fixedPlacements = fixedPlacements;
    }

    // Accessible \\

    public String getStructureName() {
        return structureName;
    }

    public short getStructureID() {
        return structureID;
    }

    public int getNameSeed() {
        return nameSeed;
    }

    public int getBlockCount() {
        return blockIDs.length;
    }

    public int[] getBlockOffsetX() {
        return blockOffsetX;
    }

    public int[] getBlockOffsetY() {
        return blockOffsetY;
    }

    public int[] getBlockOffsetZ() {
        return blockOffsetZ;
    }

    public short[] getBlockIDs() {
        return blockIDs;
    }

    public short[] getBlockOrientations() {
        return blockOrientations;
    }

    public byte[] getBlockMasks() {
        return blockMasks;
    }

    public DynamicGeometryType[] getBlockGeometry() {
        return blockGeometry;
    }

    public int getMinOffsetX() {
        return minOffsetX;
    }

    public int getMaxOffsetX() {
        return maxOffsetX;
    }

    public int getMinOffsetY() {
        return minOffsetY;
    }

    public int getMaxOffsetY() {
        return maxOffsetY;
    }

    public int getMinOffsetZ() {
        return minOffsetZ;
    }

    public int getMaxOffsetZ() {
        return maxOffsetZ;
    }

    public int getHorizontalReachBlocks() {
        return horizontalReachBlocks;
    }

    public boolean[] getFootprint() {
        return footprint;
    }

    public int[] getFootprintTopOffsetY() {
        return footprintTopOffsetY;
    }

    public int[] getFootprintTopColors() {
        return footprintTopColors;
    }

    public int[] getFootprintSideColors() {
        return footprintSideColors;
    }

    public int getYOffsetBlocks() {
        return yOffsetBlocks;
    }

    public short getFoundationBlockID() {
        return foundationBlockID;
    }

    public boolean hasFoundation() {
        return foundation;
    }

    public boolean isClearTerrain() {
        return clearTerrain;
    }

    public Direction3Vector getFront() {
        return front;
    }

    public ObjectArrayList<FurnishingSlotStruct> getFurnishings() {
        return furnishings;
    }

    public StructureRulesStruct getRules() {
        return rules;
    }

    public StructureFrequencyStruct getFrequency() {
        return frequency;
    }

    public boolean hasFrequency() {
        return frequency != null;
    }

    public ObjectArrayList<StructureFixedPlacementStruct> getFixedPlacements() {
        return fixedPlacements;
    }
}
