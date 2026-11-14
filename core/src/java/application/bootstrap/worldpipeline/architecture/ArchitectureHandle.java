package application.bootstrap.worldpipeline.architecture;

import application.bootstrap.worldpipeline.layout.LayoutRoadKind;
import application.bootstrap.worldpipeline.road.RoadHandle;
import application.bootstrap.worldpipeline.structurelist.StructureListHandle;
import engine.root.HandlePackage;

public class ArchitectureHandle extends HandlePackage {

    /*
     * Persistent architecture. Wraps ArchitectureData and delegates all
     * access through it.
     */

    // Internal
    private ArchitectureData architectureData;

    // Constructor \\

    public void constructor(ArchitectureData architectureData) {
        this.architectureData = architectureData;
    }

    // Accessible \\

    public ArchitectureData getArchitectureData() {
        return architectureData;
    }

    public String getArchitectureName() {
        return architectureData.getArchitectureName();
    }

    public short getArchitectureID() {
        return architectureData.getArchitectureID();
    }

    public int getNameSeed() {
        return architectureData.getNameSeed();
    }

    public String getDisplayName() {
        return architectureData.getDisplayName();
    }

    public RoadHandle getRoad(LayoutRoadKind kind) {
        return architectureData.getRoad(kind);
    }

    public StructureListHandle getStructureList(String role) {
        return architectureData.getStructureList(role);
    }

    public boolean hasRole(String role) {
        return architectureData.hasRole(role);
    }

    public boolean buildsSettlement(short settlementID) {
        return architectureData.buildsSettlement(settlementID);
    }

    public ArchitectureWallStruct getWall() {
        return architectureData.getWall();
    }

    public boolean hasWall() {
        return architectureData.hasWall();
    }
}
