package application.bootstrap.worldpipeline.structure;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureData extends DataPackage {

    /*
     * Persistent structure definition. Which of template, road, and layout
     * is present follows from type: STRUCTURE carries a template, ROAD a
     * road, SETTLEMENT and DUNGEON a layout. spawn is null for a definition
     * that never places itself procedurally — a road, a building that only
     * ever appears inside a layout, or a unique landmark that only appears
     * at its hand-authored locations. roadLink is null when placements of
     * this structure stay off the road network. boundingRadiusBlocks is the
     * horizontal radius around the placement point that every block this
     * structure can ever write stays inside, resolved once at load time and
     * used for overlap rejection and for deciding which chunks it touches.
     */

    private final String structureName;
    private final short structureID;
    private final StructureType structureType;
    private final StructureElevationType elevationType;

    private final StructureSpawnData spawnData;
    private final ObjectArrayList<StructureLocationStruct> locations;
    private final StructureRoadLinkData roadLinkData;

    private final StructureTemplateData templateData;
    private final RoadData roadData;
    private final StructureLayoutData layoutData;

    private final int boundingRadiusBlocks;

    public StructureData(
            String structureName,
            short structureID,
            StructureType structureType,
            StructureElevationType elevationType,
            StructureSpawnData spawnData,
            ObjectArrayList<StructureLocationStruct> locations,
            StructureRoadLinkData roadLinkData,
            StructureTemplateData templateData,
            RoadData roadData,
            StructureLayoutData layoutData,
            int boundingRadiusBlocks) {

        this.structureName = structureName;
        this.structureID = structureID;
        this.structureType = structureType;
        this.elevationType = elevationType;
        this.spawnData = spawnData;
        this.locations = locations;
        this.roadLinkData = roadLinkData;
        this.templateData = templateData;
        this.roadData = roadData;
        this.layoutData = layoutData;
        this.boundingRadiusBlocks = boundingRadiusBlocks;
    }

    public String getStructureName() {
        return structureName;
    }

    public short getStructureID() {
        return structureID;
    }

    public StructureType getStructureType() {
        return structureType;
    }

    public StructureElevationType getElevationType() {
        return elevationType;
    }

    public StructureSpawnData getSpawnData() {
        return spawnData;
    }

    public ObjectArrayList<StructureLocationStruct> getLocations() {
        return locations;
    }

    public StructureRoadLinkData getRoadLinkData() {
        return roadLinkData;
    }

    public StructureTemplateData getTemplateData() {
        return templateData;
    }

    public RoadData getRoadData() {
        return roadData;
    }

    public StructureLayoutData getLayoutData() {
        return layoutData;
    }

    public int getBoundingRadiusBlocks() {
        return boundingRadiusBlocks;
    }
}
