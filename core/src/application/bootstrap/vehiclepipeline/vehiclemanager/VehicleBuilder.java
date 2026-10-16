package application.bootstrap.vehiclepipeline.vehiclemanager;

import java.io.File;

import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import application.bootstrap.furnishingpipeline.util.FurnishingArpgUtility;
import application.bootstrap.lootpipeline.loottablemanager.LootTableManager;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCategory;
import application.bootstrap.vehiclepipeline.vehicle.VehicleDesignStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandlingStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class VehicleBuilder extends BuilderPackage {

    /*
     * Parses one vehicle ARPG file into a VehicleDesignStruct and has
     * VehicleManager assemble it into a VehicleHandle. Its "category" is a
     * ship unless it names another. Parts are listed in the order they fill
     * the model grid, each naming its role and texture and filling inclusive
     * "from"/"to" boxes or single "position"s of sub-voxels, exactly as a
     * structure lists its blocks. A yard links to its mast and a sail to its
     * yard by name, and a structure may link to the control it is a fitting
     * of. A door hinges about its pivot and axis and swings through its
     * "open_degrees". The "hull", "rig", "steering" and "anchor" groups tune
     * how it handles; the draft and centre of mass height are sub-voxels of
     * the model grid, and the drag along its length follows from the shape of
     * its bow. Each place in the optional "cargo" names the shared furnishing
     * table it is furnished from, the "corner" of the item's model grid in
     * sub-voxels, its "spin" in quarter turns about the vertical, the
     * "chance" it is furnished at all, and the "loot" table a container
     * stowed there is filled from. A malformed vehicle fails at boot.
     */

    // Internal
    private VehicleManager vehicleManager;
    private FurnishingManager furnishingManager;
    private LootTableManager lootTableManager;

    // Base \\

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
        this.furnishingManager = get(FurnishingManager.class);
        this.lootTableManager = get(LootTableManager.class);
    }

    // Build \\

    VehicleHandle build(File file, String vehicleName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        int resolution = ArpgUtility.validateInt(arpg, "resolution");

        if (resolution != EngineSetting.SUB_VOXEL_RESOLUTION)
            throwException("Vehicle '" + vehicleName + "' is built at resolution " + resolution
                    + ", but the engine's sub-voxel resolution is " + EngineSetting.SUB_VOXEL_RESOLUTION + ".");

        ArpgObjectStruct hullArpg = ArpgUtility.validateObject(arpg, "hull");
        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;

        return vehicleManager.assembleVehicle(new VehicleDesignStruct(
                vehicleName,
                ArpgUtility.getString(arpg, "display_name", resolveLocalName(vehicleName)),
                ArpgUtility.getEnum(arpg, "category", VehicleCategory.class, VehicleCategory.SHIP),
                parseParts(ArpgUtility.validateArray(arpg, "parts"), vehicleName),
                ArpgUtility.validateInt(hullArpg, "draft") * scale,
                ArpgUtility.validateInt(hullArpg, "center_of_mass_height") * scale,
                parseHandling(arpg, hullArpg),
                FurnishingArpgUtility.parseSlots(arpg, "cargo", vehicleName, furnishingManager, lootTableManager)));
    }

    // Parts \\

    private ObjectArrayList<VehiclePartStruct> parseParts(ArpgArrayStruct partsArpg, String vehicleName) {

        ObjectArrayList<VehiclePartStruct> parts = new ObjectArrayList<>(partsArpg.size());

        for (int i = 0; i < partsArpg.size(); i++)
            parts.add(parsePart(partsArpg.get(i).getAsObject(), vehicleName));

        return parts;
    }

    private VehiclePartStruct parsePart(ArpgObjectStruct partArpg, String vehicleName) {

        String partName = ArpgUtility.validateString(partArpg, "name");
        VehiclePartRole role = ArpgUtility.toEnum(ArpgUtility.validateString(partArpg, "role"), VehiclePartRole.class);
        VehiclePartStruct part = new VehiclePartStruct(
                partName,
                role,
                ArpgUtility.validateString(partArpg, "texture"),
                ArpgUtility.getString(partArpg, "link", ""),
                parsePivot(partArpg, role, partName, vehicleName),
                parseAxis(partArpg, role, partName, vehicleName),
                parseOpenAngle(partArpg, role));

        ArpgArrayStruct boxesArpg = ArpgUtility.validateArray(partArpg, "boxes");

        for (int i = 0; i < boxesArpg.size(); i++)
            parseBox(part, boxesArpg.get(i).getAsObject(), vehicleName);

        return part;
    }

    // A single "position", or an inclusive "from"/"to" box, in model sub-voxels
    private void parseBox(VehiclePartStruct part, ArpgObjectStruct boxArpg, String vehicleName) {

        if (boxArpg.has("position")) {

            ArpgArrayStruct position = ArpgUtility.validateArray(boxArpg, "position", EngineSetting.AXIS_COUNT);
            int x = position.get(0).getAsInt();
            int y = position.get(1).getAsInt();
            int z = position.get(2).getAsInt();

            addBox(part, x, y, z, x, y, z, vehicleName);
            return;
        }

        ArpgArrayStruct from = ArpgUtility.validateArray(boxArpg, "from", EngineSetting.AXIS_COUNT);
        ArpgArrayStruct to = ArpgUtility.validateArray(boxArpg, "to", EngineSetting.AXIS_COUNT);

        addBox(
                part,
                from.get(0).getAsInt(),
                from.get(1).getAsInt(),
                from.get(2).getAsInt(),
                to.get(0).getAsInt(),
                to.get(1).getAsInt(),
                to.get(2).getAsInt(),
                vehicleName);
    }

    private void addBox(
            VehiclePartStruct part,
            int fromX,
            int fromY,
            int fromZ,
            int toX,
            int toY,
            int toZ,
            String vehicleName) {

        int minX = Math.min(fromX, toX);
        int minY = Math.min(fromY, toY);
        int minZ = Math.min(fromZ, toZ);

        if (minX < 0 || minY < 0 || minZ < 0)
            throwException("Vehicle '" + vehicleName + "' part '" + part.getPartName()
                    + "' reaches below the model grid; every sub-voxel coordinate must be zero or more.");

        part.addBox(minX, minY, minZ, Math.max(fromX, toX) + 1, Math.max(fromY, toY) + 1, Math.max(fromZ, toZ) + 1);
    }

    // A mast, helm, rudder or door turns about its pivot, given in sub-voxels and kept in blocks
    private Vector3 parsePivot(ArpgObjectStruct partArpg, VehiclePartRole role, String partName, String vehicleName) {

        boolean required = role == VehiclePartRole.MAST || role == VehiclePartRole.HELM
                || role == VehiclePartRole.RUDDER || role == VehiclePartRole.DOOR;

        if (!partArpg.has("pivot")) {

            if (required)
                throwException("Vehicle '" + vehicleName + "' " + ArpgUtility.toEnumName(role) + " '" + partName
                        + "' needs a pivot.");

            return new Vector3();
        }

        ArpgArrayStruct pivotArpg = ArpgUtility.validateArray(partArpg, "pivot", EngineSetting.AXIS_COUNT);
        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;

        return new Vector3(
                pivotArpg.get(0).getAsFloat() * scale,
                pivotArpg.get(1).getAsFloat() * scale,
                pivotArpg.get(2).getAsFloat() * scale);
    }

    private int parseAxis(ArpgObjectStruct partArpg, VehiclePartRole role, String partName, String vehicleName) {

        if (role != VehiclePartRole.HELM && role != VehiclePartRole.DOOR)
            return EngineSetting.AXIS_Y;

        String axisName = ArpgUtility.validateString(partArpg, "axis");

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
            if (EngineSetting.AXIS_KEYS[axis].equalsIgnoreCase(axisName))
                return axis;

        return throwException("Vehicle '" + vehicleName + "' " + ArpgUtility.toEnumName(role) + " '" + partName
                + "' turns about axis '" + axisName + "', which is none of x, y or z.");
    }

    // How far a door swings open in radians, its sign the way it turns about its axis
    private float parseOpenAngle(ArpgObjectStruct partArpg, VehiclePartRole role) {

        if (role != VehiclePartRole.DOOR)
            return 0f;

        return (float) Math.toRadians(ArpgUtility.getFloat(
                partArpg, "open_degrees", EngineSetting.DEFAULT_VEHICLE_DOOR_OPEN_DEGREES));
    }

    // Handling \\

    private VehicleHandlingStruct parseHandling(ArpgObjectStruct arpg, ArpgObjectStruct hullArpg) {

        ArpgObjectStruct rigArpg = resolveGroup(arpg, "rig");
        ArpgObjectStruct steeringArpg = resolveGroup(arpg, "steering");
        ArpgObjectStruct anchorArpg = resolveGroup(arpg, "anchor");

        return new VehicleHandlingStruct(
                ArpgUtility.getFloat(hullArpg, "sway_drag", EngineSetting.DEFAULT_VEHICLE_SWAY_DRAG),
                ArpgUtility.getFloat(hullArpg, "heave_drag", EngineSetting.DEFAULT_VEHICLE_HEAVE_DRAG),
                ArpgUtility.getFloat(hullArpg, "flood_rate", EngineSetting.DEFAULT_VEHICLE_FLOOD_RATE),
                ArpgUtility.getFloat(hullArpg, "pump_rate", EngineSetting.DEFAULT_VEHICLE_PUMP_RATE),
                ArpgUtility.getFloat(rigArpg, "sail_force", EngineSetting.DEFAULT_VEHICLE_SAIL_FORCE),
                (float) Math.toRadians(ArpgUtility.getFloat(
                        rigArpg, "brace_limit_degrees", EngineSetting.DEFAULT_VEHICLE_BRACE_LIMIT_DEGREES)),
                (float) Math.toRadians(ArpgUtility.getFloat(
                        rigArpg, "brace_rate_degrees", EngineSetting.DEFAULT_VEHICLE_BRACE_RATE_DEGREES)),
                1f / Math.max(EngineSetting.DIVISION_EPSILON, ArpgUtility.getFloat(
                        rigArpg, "hoist_seconds", EngineSetting.DEFAULT_VEHICLE_HOIST_SECONDS)),
                (float) Math.toRadians(ArpgUtility.getFloat(
                        steeringArpg, "rudder_limit_degrees", EngineSetting.DEFAULT_VEHICLE_RUDDER_LIMIT_DEGREES)),
                (float) Math.toRadians(ArpgUtility.getFloat(
                        steeringArpg, "rudder_rate_degrees", EngineSetting.DEFAULT_VEHICLE_RUDDER_RATE_DEGREES)),
                ArpgUtility.getFloat(steeringArpg, "rudder_force", EngineSetting.DEFAULT_VEHICLE_RUDDER_FORCE),
                ArpgUtility.getFloat(anchorArpg, "hold", EngineSetting.DEFAULT_VEHICLE_ANCHOR_HOLD),
                ArpgUtility.getFloat(anchorArpg, "drop", EngineSetting.DEFAULT_VEHICLE_ANCHOR_DROP));
    }

    // An optional group, read as empty so every field in it falls back to its default
    private ArpgObjectStruct resolveGroup(ArpgObjectStruct arpg, String key) {
        return ArpgUtility.hasObject(arpg, key) ? arpg.getAsObject(key) : new ArpgObjectStruct();
    }

    // Utility \\

    // The last segment of a vehicle's resource name, used as its display name when it gives none
    private String resolveLocalName(String vehicleName) {
        return vehicleName.substring(vehicleName.lastIndexOf(EngineSetting.PATH_SEPARATOR) + 1);
    }
}
