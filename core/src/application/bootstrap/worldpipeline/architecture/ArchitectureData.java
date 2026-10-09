package application.bootstrap.worldpipeline.architecture;

import application.bootstrap.worldpipeline.layout.LayoutRoadKind;
import application.bootstrap.worldpipeline.road.RoadHandle;
import application.bootstrap.worldpipeline.structurelist.StructureListHandle;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

public class ArchitectureData extends DataPackage {

    /*
     * Immutable architecture built from ARPG by ArchitectureBuilder: a
     * building style biomes name to say what may grow on them. It holds the
     * road it lays for each kind of road a layout asks for, the structure
     * list it fills each role of a layout from, the settlement types it
     * builds, every type when it names none, and the curtain wall it rings
     * walled settlements with, if any.
     */

    // Identity
    private final String architectureName;
    private final short architectureID;
    private final int nameSeed;
    private final String displayName;

    // Roads — indexed by LayoutRoadKind ordinal
    private final RoadHandle[] roads;

    // Roles
    private final Object2ObjectOpenHashMap<String, StructureListHandle> role2StructureList;

    // Settlements — empty builds every type
    private final ShortOpenHashSet settlementIDs;

    // Wall
    private final ArchitectureWallStruct wall;

    // Constructor \\

    public ArchitectureData(
            String architectureName,
            short architectureID,
            int nameSeed,
            String displayName,
            RoadHandle[] roads,
            Object2ObjectOpenHashMap<String, StructureListHandle> role2StructureList,
            ShortOpenHashSet settlementIDs,
            ArchitectureWallStruct wall) {

        // Identity
        this.architectureName = architectureName;
        this.architectureID = architectureID;
        this.nameSeed = nameSeed;
        this.displayName = displayName;

        // Roads
        this.roads = roads;

        // Roles
        this.role2StructureList = role2StructureList;

        // Settlements
        this.settlementIDs = settlementIDs;

        // Wall
        this.wall = wall;
    }

    // Accessible \\

    public String getArchitectureName() {
        return architectureName;
    }

    public short getArchitectureID() {
        return architectureID;
    }

    public int getNameSeed() {
        return nameSeed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RoadHandle getRoad(LayoutRoadKind kind) {
        return roads[kind.ordinal()];
    }

    public StructureListHandle getStructureList(String role) {
        return role2StructureList.get(role);
    }

    public boolean hasRole(String role) {
        return role2StructureList.containsKey(role);
    }

    public boolean buildsSettlement(short settlementID) {
        return settlementIDs.isEmpty() || settlementIDs.contains(settlementID);
    }

    public ArchitectureWallStruct getWall() {
        return wall;
    }

    public boolean hasWall() {
        return wall != null;
    }
}
