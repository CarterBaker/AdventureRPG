package application.bootstrap.worldpipeline.structure;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureData extends DataPackage {

    /*
     * Persistent structure record. Blocks are parallel arrays of origin-relative
     * offsets with names already resolved to ID, orientation, and geometry, so
     * stamping is a straight walk over primitives. Unlisted positions are left
     * untouched; listed air carves.
     */

    private final String structureName;
    private final short structureID;

    private final int[] blockOffsetX;
    private final int[] blockOffsetY;
    private final int[] blockOffsetZ;
    private final short[] blockIDs;
    private final short[] blockOrientations;
    private final DynamicGeometryType[] blockGeometry;

    private final int minOffsetX;
    private final int maxOffsetX;
    private final int minOffsetZ;
    private final int maxOffsetZ;
    private final int horizontalReachBlocks;

    private final int yOffsetBlocks;
    private final short foundationBlockID;
    private final boolean foundation;

    private final StructureRulesStruct rules;
    private final StructureFrequencyStruct frequency;
    private final ObjectArrayList<StructureFixedPlacementStruct> fixedPlacements;

    public StructureData(
            String structureName,
            short structureID,
            int[] blockOffsetX,
            int[] blockOffsetY,
            int[] blockOffsetZ,
            short[] blockIDs,
            short[] blockOrientations,
            DynamicGeometryType[] blockGeometry,
            int minOffsetX,
            int maxOffsetX,
            int minOffsetZ,
            int maxOffsetZ,
            int horizontalReachBlocks,
            int yOffsetBlocks,
            short foundationBlockID,
            boolean foundation,
            StructureRulesStruct rules,
            StructureFrequencyStruct frequency,
            ObjectArrayList<StructureFixedPlacementStruct> fixedPlacements) {

        this.structureName = structureName;
        this.structureID = structureID;

        this.blockOffsetX = blockOffsetX;
        this.blockOffsetY = blockOffsetY;
        this.blockOffsetZ = blockOffsetZ;
        this.blockIDs = blockIDs;
        this.blockOrientations = blockOrientations;
        this.blockGeometry = blockGeometry;

        this.minOffsetX = minOffsetX;
        this.maxOffsetX = maxOffsetX;
        this.minOffsetZ = minOffsetZ;
        this.maxOffsetZ = maxOffsetZ;
        this.horizontalReachBlocks = horizontalReachBlocks;

        this.yOffsetBlocks = yOffsetBlocks;
        this.foundationBlockID = foundationBlockID;
        this.foundation = foundation;

        this.rules = rules;
        this.frequency = frequency;
        this.fixedPlacements = fixedPlacements;
    }

    public String getStructureName() {
        return structureName;
    }

    public short getStructureID() {
        return structureID;
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

    public DynamicGeometryType[] getBlockGeometry() {
        return blockGeometry;
    }

    public int getMinOffsetX() {
        return minOffsetX;
    }

    public int getMaxOffsetX() {
        return maxOffsetX;
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

    public int getYOffsetBlocks() {
        return yOffsetBlocks;
    }

    public short getFoundationBlockID() {
        return foundationBlockID;
    }

    public boolean hasFoundation() {
        return foundation;
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
