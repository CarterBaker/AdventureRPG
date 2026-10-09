package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class WorldPlacementArpgUtility extends EngineUtility {

    /*
     * The one reader and writer of a world's hand-picked placements in its
     * companion file. Each entry names its "kind", settlement or structure,
     * the "name" of what it stands up, an optional "architecture" a
     * settlement is built in, the block column "x" and "z" it stands at, and
     * the clockwise quarter turns "rotation" a structure takes.
     */

    // Read \\

    public static ObjectArrayList<WorldPlacementStruct> parsePlacements(ArpgObjectStruct worldArpg, String worldName) {

        ObjectArrayList<WorldPlacementStruct> placements = new ObjectArrayList<>();

        if (!ArpgUtility.hasArray(worldArpg, "placements"))
            return placements;

        ArpgArrayStruct placementsArpg = worldArpg.getAsArray("placements");

        for (int i = 0; i < placementsArpg.size(); i++)
            placements.add(parsePlacement(placementsArpg.get(i).getAsObject(), worldName));

        return placements;
    }

    private static WorldPlacementStruct parsePlacement(ArpgObjectStruct placementArpg, String worldName) {

        WorldPlacementKind kind = ArpgUtility.toEnum(
                ArpgUtility.validateString(placementArpg, "kind"), WorldPlacementKind.class);
        String name = ArpgUtility.validateString(placementArpg, "name");
        int quarterTurns = ArpgUtility.getInt(placementArpg, "rotation", 0);

        if (quarterTurns < 0 || quarterTurns >= EngineSetting.STRUCTURE_QUARTER_TURN_COUNT)
            throwException("World \"" + worldName + "\" placement of \"" + name + "\" has rotation " + quarterTurns
                    + " — rotation must be between 0 and " + (EngineSetting.STRUCTURE_QUARTER_TURN_COUNT - 1)
                    + " clockwise quarter turns.");

        return new WorldPlacementStruct(
                kind,
                name,
                ArpgUtility.getString(placementArpg, "architecture", ""),
                ArpgUtility.validateInt(placementArpg, "x"),
                ArpgUtility.validateInt(placementArpg, "z"),
                quarterTurns);
    }

    // Write \\

    // The world's placements replaced with these, every other field of the companion kept as it was
    public static void writePlacements(ArpgObjectStruct worldArpg, WorldPlacementStruct[] placements) {

        ArpgArrayStruct placementsArpg = new ArpgArrayStruct();

        for (int i = 0; i < placements.length; i++)
            placementsArpg.add(toArpg(placements[i]));

        worldArpg.remove("placements");
        worldArpg.add("placements", placementsArpg);
    }

    private static ArpgObjectStruct toArpg(WorldPlacementStruct placement) {

        ArpgObjectStruct placementArpg = new ArpgObjectStruct();

        placementArpg.addProperty("kind", ArpgUtility.toEnumName(placement.getKind()));
        placementArpg.addProperty("name", placement.getName());

        if (placement.hasArchitecture())
            placementArpg.addProperty("architecture", placement.getArchitectureName());

        placementArpg.addProperty("x", placement.getWorldX());
        placementArpg.addProperty("z", placement.getWorldZ());

        if (placement.getQuarterTurns() != 0)
            placementArpg.addProperty("rotation", placement.getQuarterTurns());

        return placementArpg;
    }
}
