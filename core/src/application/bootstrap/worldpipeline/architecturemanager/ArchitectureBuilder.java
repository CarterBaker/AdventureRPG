package application.bootstrap.worldpipeline.architecturemanager;

import java.io.File;

import application.bootstrap.worldpipeline.architecture.ArchitectureData;
import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import application.bootstrap.worldpipeline.architecture.ArchitectureWallStruct;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.layout.LayoutRoadKind;
import application.bootstrap.worldpipeline.road.RoadHandle;
import application.bootstrap.worldpipeline.roadmanager.RoadManager;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.structurelist.StructureListHandle;
import application.bootstrap.worldpipeline.structurelistmanager.StructureListManager;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

class ArchitectureBuilder extends BuilderPackage {

    /*
     * Parses one architecture ARPG file into an ArchitectureData and wraps it
     * in an ArchitectureHandle. It names the "street" its settlements are
     * laid with, the "road" that joins larger settlements, the street when it
     * names none, and the "path" that joins small ones and wanders off to
     * outposts, the road when it names none. Its "roles" map each role a
     * layout asks for to a structure list, its optional "settlements" the
     * settlement types it builds, and its optional "wall" the block, cap,
     * height and thickness of the curtain wall around walled settlements.
     * Every road, list, settlement and block is resolved here, so a
     * malformed architecture fails at boot.
     */

    // Internal
    private ArchitectureManager architectureManager;
    private RoadManager roadManager;
    private StructureListManager structureListManager;
    private SettlementManager settlementManager;
    private BlockManager blockManager;

    // Base \\

    @Override
    protected void get() {
        this.architectureManager = get(ArchitectureManager.class);
        this.roadManager = get(RoadManager.class);
        this.structureListManager = get(StructureListManager.class);
        this.settlementManager = get(SettlementManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Build \\

    ArchitectureHandle build(File file, String architectureName) {

        short architectureID = architectureManager.registerArchitectureName(architectureName);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        RoadHandle street = roadManager.getRoadHandleFromRoadName(ArpgUtility.validateString(arpg, "street"));
        RoadHandle road = arpg.has("road")
                ? roadManager.getRoadHandleFromRoadName(arpg.get("road").getAsString())
                : street;
        RoadHandle path = arpg.has("path")
                ? roadManager.getRoadHandleFromRoadName(arpg.get("path").getAsString())
                : road;

        RoadHandle[] roads = new RoadHandle[LayoutRoadKind.values().length];
        roads[LayoutRoadKind.STREET.ordinal()] = street;
        roads[LayoutRoadKind.ROAD.ordinal()] = road;
        roads[LayoutRoadKind.PATH.ordinal()] = path;

        ArchitectureData architectureData = new ArchitectureData(
                architectureName,
                architectureID,
                RegistryUtility.toNameSeed(architectureName),
                ArpgUtility.getString(arpg, "display_name", architectureName),
                roads,
                parseRoles(arpg),
                parseSettlements(arpg),
                parseWall(arpg, architectureName));

        ArchitectureHandle architectureHandle = create(ArchitectureHandle.class);
        architectureHandle.constructor(architectureData);

        return architectureHandle;
    }

    // Roles \\

    private Object2ObjectOpenHashMap<String, StructureListHandle> parseRoles(ArpgObjectStruct arpg) {

        Object2ObjectOpenHashMap<String, StructureListHandle> role2StructureList = new Object2ObjectOpenHashMap<>();
        ArpgObjectStruct rolesArpg = ArpgUtility.validateObject(arpg, "roles");

        for (String role : rolesArpg.keySet())
            role2StructureList.put(role, structureListManager.getStructureListHandleFromStructureListName(
                    rolesArpg.get(role).getAsString()));

        return role2StructureList;
    }

    // Settlements \\

    private ShortOpenHashSet parseSettlements(ArpgObjectStruct arpg) {

        ShortOpenHashSet settlementIDs = new ShortOpenHashSet();

        if (!ArpgUtility.hasArray(arpg, "settlements"))
            return settlementIDs;

        for (ArpgElementStruct element : arpg.getAsArray("settlements"))
            settlementIDs.add(settlementManager.getSettlementIDFromSettlementName(element.getAsString()));

        return settlementIDs;
    }

    // Wall \\

    private ArchitectureWallStruct parseWall(ArpgObjectStruct arpg, String architectureName) {

        if (!ArpgUtility.hasObject(arpg, "wall"))
            return null;

        ArpgObjectStruct wallArpg = arpg.getAsObject("wall");
        BlockHandle wallBlock = blockManager.getSolidBlockHandleFromBlockName(
                ArpgUtility.validateString(wallArpg, "block"), architectureName);
        BlockHandle capBlock = wallArpg.has("cap_block")
                ? blockManager.getSolidBlockHandleFromBlockName(
                        wallArpg.get("cap_block").getAsString(), architectureName)
                : wallBlock;
        int heightBlocks = ArpgUtility.validateInt(wallArpg, "height_blocks");
        float thicknessBlocks = ArpgUtility.validateFloat(wallArpg, "thickness_blocks");

        if (heightBlocks < 1 || thicknessBlocks < 1f)
            throwException("Architecture \"" + architectureName
                    + "\" wall must stand at least 1 block high and 1 block thick.");

        return new ArchitectureWallStruct(
                wallBlock.getBlockID(),
                capBlock.getBlockID(),
                heightBlocks,
                thicknessBlocks,
                capBlock.getMapColorForFace(Direction3Vector.UP),
                wallBlock.getMapColorForFace(Direction3Vector.NORTH));
    }
}
