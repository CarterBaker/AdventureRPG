package application.bootstrap.worldpipeline.architecture;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.worldpipeline.layout.LayoutRoadKind;
import application.bootstrap.worldpipeline.road.RoadHandle;
import application.bootstrap.worldpipeline.structurelist.StructureListHandle;
import engine.root.DataPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

public class ArchitectureData extends DataPackage {

    /*
     * Immutable architecture built from ARPG by ArchitectureBuilder: a
     * building style biomes name to say what may grow on them. It holds the
     * road it lays for each kind of road a layout asks for, the structure
     * list it fills each role of a layout from, the settlement types it
     * builds, every type when it names none, and the curtain wall it rings
     * walled settlements with, if any. As a style blueprint it fills block
     * slots with its own blocks and furniture slots, furnishing tables, with
     * its own piece of furniture, so every structure it builds takes its look;
     * a slot it leaves empty keeps what the structure was authored with.
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

    // Slots — a block slot's block, RESERVED where unfilled, and a furnishing table's item, absent where unfilled
    private final Short2ShortOpenHashMap blockSlotID2BlockID;
    private final Short2ObjectOpenHashMap<ItemDefinitionHandle> furnishingID2Item;

    // Constructor \\

    public ArchitectureData(
            String architectureName,
            short architectureID,
            int nameSeed,
            String displayName,
            RoadHandle[] roads,
            Object2ObjectOpenHashMap<String, StructureListHandle> role2StructureList,
            ShortOpenHashSet settlementIDs,
            ArchitectureWallStruct wall,
            Short2ShortOpenHashMap blockSlotID2BlockID,
            Short2ObjectOpenHashMap<ItemDefinitionHandle> furnishingID2Item) {

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

        // Slots
        this.blockSlotID2BlockID = blockSlotID2BlockID;
        this.furnishingID2Item = furnishingID2Item;
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

    // The block a structure lays in a block slot, the one it was authored with where this leaves the slot empty
    public short resolveBlockID(short blockSlotID, short authoredBlockID) {

        short blockID = blockSlotID2BlockID.get(blockSlotID);

        return blockID == EngineSetting.REGISTRY_RESERVED_ID ? authoredBlockID : blockID;
    }

    public boolean fillsBlockSlot(short blockSlotID) {
        return blockSlotID2BlockID.containsKey(blockSlotID);
    }

    // The furniture standing wherever a structure furnishes from a table, null where this leaves the table to draw
    public ItemDefinitionHandle getFurnitureItem(short furnishingID) {
        return furnishingID2Item.get(furnishingID);
    }
}
