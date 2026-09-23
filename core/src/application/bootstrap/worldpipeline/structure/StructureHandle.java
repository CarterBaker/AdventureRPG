package application.bootstrap.worldpipeline.structure;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureHandle extends HandlePackage {

    /*
     * Persistent structure record. Wraps StructureData and delegates all access
     * through it.
     */

    // Internal
    private StructureData structureData;

    // Constructor \\

    public void constructor(StructureData structureData) {
        this.structureData = structureData;
    }

    // Accessible \\

    public StructureData getStructureData() {
        return structureData;
    }

    public String getStructureName() {
        return structureData.getStructureName();
    }

    public short getStructureID() {
        return structureData.getStructureID();
    }

    public int getBlockCount() {
        return structureData.getBlockCount();
    }

    public int[] getBlockOffsetX() {
        return structureData.getBlockOffsetX();
    }

    public int[] getBlockOffsetY() {
        return structureData.getBlockOffsetY();
    }

    public int[] getBlockOffsetZ() {
        return structureData.getBlockOffsetZ();
    }

    public short[] getBlockIDs() {
        return structureData.getBlockIDs();
    }

    public short[] getBlockOrientations() {
        return structureData.getBlockOrientations();
    }

    public DynamicGeometryType[] getBlockGeometry() {
        return structureData.getBlockGeometry();
    }

    public int getMinOffsetX() {
        return structureData.getMinOffsetX();
    }

    public int getMaxOffsetX() {
        return structureData.getMaxOffsetX();
    }

    public int getMinOffsetZ() {
        return structureData.getMinOffsetZ();
    }

    public int getMaxOffsetZ() {
        return structureData.getMaxOffsetZ();
    }

    public int getHorizontalReachBlocks() {
        return structureData.getHorizontalReachBlocks();
    }

    public int getYOffsetBlocks() {
        return structureData.getYOffsetBlocks();
    }

    public short getFoundationBlockID() {
        return structureData.getFoundationBlockID();
    }

    public boolean hasFoundation() {
        return structureData.hasFoundation();
    }

    public StructureRulesStruct getRules() {
        return structureData.getRules();
    }

    public StructureFrequencyStruct getFrequency() {
        return structureData.getFrequency();
    }

    public boolean hasFrequency() {
        return structureData.hasFrequency();
    }

    public ObjectArrayList<StructureFixedPlacementStruct> getFixedPlacements() {
        return structureData.getFixedPlacements();
    }
}
