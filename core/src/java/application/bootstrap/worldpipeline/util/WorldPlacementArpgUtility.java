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
     * settlement is built in, an optional numbered "layout" a settlement is
     * planned from, the block column "x" and "z" it stands at, the
     * clockwise quarter turns "rotation" a structure takes, and a camera's
     * view: the "height" above the ground it looks out from, its "yaw" and
     * its "pitch". Every optional field left at its default is not written.
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
        int layout = ArpgUtility.getInt(placementArpg, "layout", EngineSetting.WORLD_PLACEMENT_LAYOUT_CELL);
        float height = ArpgUtility.getFloat(placementArpg, "height", 0f);
        float yaw = ArpgUtility.getFloat(placementArpg, "yaw", 0f);
        float pitch = ArpgUtility.getFloat(placementArpg, "pitch", 0f);

        if (quarterTurns < 0 || quarterTurns >= EngineSetting.STRUCTURE_QUARTER_TURN_COUNT)
            throwException("World \"" + worldName + "\" placement of \"" + name + "\" has rotation " + quarterTurns
                    + " — rotation must be between 0 and " + (EngineSetting.STRUCTURE_QUARTER_TURN_COUNT - 1)
                    + " clockwise quarter turns.");

        if (layout < EngineSetting.WORLD_PLACEMENT_LAYOUT_CELL || layout > EngineSetting.WORLD_PLACEMENT_LAYOUT_MAX)
            throwException("World \"" + worldName + "\" placement of \"" + name + "\" has layout " + layout
                    + " — layout must be between " + EngineSetting.WORLD_PLACEMENT_LAYOUT_CELL + " and "
                    + EngineSetting.WORLD_PLACEMENT_LAYOUT_MAX + ", 0 growing the layout its cell would.");

        if (height < 0f || height > EngineSetting.MENU_CAMERA_HEIGHT_MAX)
            throwException("World \"" + worldName + "\" placement of \"" + name + "\" has height " + height
                    + " — height must be between 0 and " + EngineSetting.MENU_CAMERA_HEIGHT_MAX
                    + " blocks above the ground.");

        if (yaw < 0f || yaw >= EngineSetting.MENU_CAMERA_YAW_TURN)
            throwException("World \"" + worldName + "\" placement of \"" + name + "\" has yaw " + yaw
                    + " — yaw must be at least 0 and below " + EngineSetting.MENU_CAMERA_YAW_TURN + " degrees.");

        if (Math.abs(pitch) > EngineSetting.CAMERA_MAX_PITCH_DEGREES)
            throwException("World \"" + worldName + "\" placement of \"" + name + "\" has pitch " + pitch
                    + " — pitch must be between " + -EngineSetting.CAMERA_MAX_PITCH_DEGREES + " and "
                    + EngineSetting.CAMERA_MAX_PITCH_DEGREES + " degrees.");

        return new WorldPlacementStruct(
                kind,
                name,
                ArpgUtility.getString(placementArpg, "architecture", ""),
                layout,
                ArpgUtility.validateInt(placementArpg, "x"),
                ArpgUtility.validateInt(placementArpg, "z"),
                quarterTurns,
                height,
                yaw,
                pitch);
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

        if (placement.hasLayout())
            placementArpg.addProperty("layout", placement.getLayout());

        placementArpg.addProperty("x", placement.getWorldX());
        placementArpg.addProperty("z", placement.getWorldZ());

        if (placement.getQuarterTurns() != 0)
            placementArpg.addProperty("rotation", placement.getQuarterTurns());

        if (placement.getHeight() != 0f)
            placementArpg.addProperty("height", placement.getHeight());

        if (placement.getYaw() != 0f)
            placementArpg.addProperty("yaw", placement.getYaw());

        if (placement.getPitch() != 0f)
            placementArpg.addProperty("pitch", placement.getPitch());

        return placementArpg;
    }
}
