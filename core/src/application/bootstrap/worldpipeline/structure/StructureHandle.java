package application.bootstrap.worldpipeline.structure;

import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureHandle extends HandlePackage {

    /*
     * Persistent structure record owned by StructureManager. Wraps
     * StructureData and delegates all access through it.
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

    public StructureType getStructureType() {
        return structureData.getStructureType();
    }

    public StructureElevationType getElevationType() {
        return structureData.getElevationType();
    }

    public StructureSpawnData getSpawnData() {
        return structureData.getSpawnData();
    }

    public boolean spawnsProcedurally() {
        return structureData.getSpawnData() != null;
    }

    public ObjectArrayList<StructureLocationStruct> getLocations() {
        return structureData.getLocations();
    }

    public StructureRoadLinkData getRoadLinkData() {
        return structureData.getRoadLinkData();
    }

    public boolean connectsToRoads() {
        return structureData.getRoadLinkData() != null;
    }

    public StructureTemplateData getTemplateData() {
        return structureData.getTemplateData();
    }

    public RoadData getRoadData() {
        return structureData.getRoadData();
    }

    public StructureLayoutData getLayoutData() {
        return structureData.getLayoutData();
    }

    public int getBoundingRadiusBlocks() {
        return structureData.getBoundingRadiusBlocks();
    }
}
