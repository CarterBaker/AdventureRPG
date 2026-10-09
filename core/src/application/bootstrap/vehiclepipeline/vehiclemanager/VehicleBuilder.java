package application.bootstrap.vehiclepipeline.vehiclemanager;

import java.io.File;

import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import application.bootstrap.furnishingpipeline.util.FurnishingArpgUtility;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCategory;
import application.bootstrap.vehiclepipeline.vehicle.VehicleData;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandlingStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHullStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartControl;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleSailStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class VehicleBuilder extends BuilderPackage {

    /*
     * Parses one vehicle ARPG file into a VehicleData and wraps it in a
     * VehicleHandle. Its "category" is a ship unless it names another. Parts
     * are listed in the order they fill the model grid, each naming its role
     * and texture and filling inclusive "from"/"to" boxes or single "position"s
     * of sub-voxels, exactly as a structure lists its blocks. A yard links to
     * its mast and a sail to its yard by name, a structure may link to the
     * control it is a fitting of, and there is at most one helm, one rudder and
     * one capstan. A door hinges about its pivot and axis and swings through
     * its "open_degrees", and every door and portcullis is numbered in data
     * order. The "hull", "rig", "steering" and "anchor" groups tune how it
     * handles; the draft and centre of mass height are sub-voxels of the model
     * grid. Each place in the optional "cargo" names the shared furnishing
     * table it is furnished from, the "corner" of the item's model grid in
     * sub-voxels, its "spin" in quarter turns about the vertical, and the
     * "chance" it is furnished at all. Geometry and the hull's physics
     * are worked out by VehicleGeometryBuilder and VehicleHullBuilder, so a
     * malformed vehicle fails at boot.
     */

    // Internal
    private VehicleGeometryBuilder vehicleGeometryBuilder;
    private VehicleHullBuilder vehicleHullBuilder;
    private FurnishingManager furnishingManager;

    // Base \\

    @Override
    protected void get() {
        this.vehicleGeometryBuilder = get(VehicleGeometryBuilder.class);
        this.vehicleHullBuilder = get(VehicleHullBuilder.class);
        this.furnishingManager = get(FurnishingManager.class);
    }

    // Build \\

    VehicleHandle build(File file, String vehicleName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        int resolution = ArpgUtility.validateInt(arpg, "resolution");

        if (resolution != EngineSetting.SUB_VOXEL_RESOLUTION)
            throwException("Vehicle '" + vehicleName + "' is built at resolution " + resolution
                    + ", but the engine's sub-voxel resolution is " + EngineSetting.SUB_VOXEL_RESOLUTION + ".");

        String displayName = ArpgUtility.getString(arpg, "display_name", resolveLocalName(vehicleName));
        VehicleCategory category = ArpgUtility.getEnum(arpg, "category", VehicleCategory.class, VehicleCategory.SHIP);
        ObjectArrayList<VehiclePartStruct> parts = parseParts(ArpgUtility.validateArray(arpg, "parts"), vehicleName);
        IntArrayList doorParts = new IntArrayList();
        IntArrayList mastParts = linkParts(parts, doorParts, vehicleName);

        ArpgObjectStruct hullArpg = ArpgUtility.validateObject(arpg, "hull");
        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;
        float draft = ArpgUtility.validateInt(hullArpg, "draft") * scale;
        float centerOfMassHeight = ArpgUtility.validateInt(hullArpg, "center_of_mass_height") * scale;

        SubVoxelGridStruct solidGrid = vehicleGeometryBuilder.buildSolidGrid(parts);
        ObjectArrayList<MeshInstance> hullMeshes = vehicleGeometryBuilder.buildHullMeshes(parts);
        vehicleGeometryBuilder.buildPartMeshes(parts);

        VehicleHullStruct hull = vehicleHullBuilder.build(vehicleName, parts, draft, centerOfMassHeight);
        int[] bounds = resolveBounds(parts);

        VehicleData vehicleData = new VehicleData(
                vehicleName,
                displayName,
                category,
                parts,
                mastParts,
                buildSails(parts),
                findSinglePart(parts, VehiclePartRole.HELM, vehicleName),
                findSinglePart(parts, VehiclePartRole.RUDDER, vehicleName),
                findSinglePart(parts, VehiclePartRole.CAPSTAN, vehicleName),
                doorParts,
                solidGrid,
                buildClimbZones(parts),
                hullMeshes,
                bounds[EngineSetting.BOX_MIN_X],
                bounds[EngineSetting.BOX_MIN_Y],
                bounds[EngineSetting.BOX_MIN_Z],
                bounds[EngineSetting.BOX_MAX_X],
                bounds[EngineSetting.BOX_MAX_Y],
                bounds[EngineSetting.BOX_MAX_Z],
                resolveBoundingRadius(bounds, hull.getCenterOfMass()),
                hull,
                parseHandling(arpg, hullArpg),
                FurnishingArpgUtility.parseSlots(arpg, "cargo", vehicleName, furnishingManager));

        VehicleHandle vehicleHandle = create(VehicleHandle.class);
        vehicleHandle.constructor(vehicleData);

        return vehicleHandle;
    }

    // Parts \\

    private ObjectArrayList<VehiclePartStruct> parseParts(ArpgArrayStruct partsArpg, String vehicleName) {

        if (partsArpg.size() > EngineSetting.SUB_VOXEL_MAX_PARTS)
            throwException("Vehicle '" + vehicleName + "' lists " + partsArpg.size() + " parts, over the "
                    + EngineSetting.SUB_VOXEL_MAX_PARTS + " a vehicle can hold.");

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

        if (part.isEmpty())
            throwException("Vehicle '" + vehicleName + "' part '" + partName + "' fills no sub-voxels.");

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

    // Links \\

    // Yards to their masts and sails to their yards, every door numbered into the doors given, returning the masts
    // in data order
    private IntArrayList linkParts(
            ObjectArrayList<VehiclePartStruct> parts,
            IntArrayList doorParts,
            String vehicleName) {

        Object2IntOpenHashMap<String> partName2PartIndex = new Object2IntOpenHashMap<>();
        partName2PartIndex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        IntArrayList mastParts = new IntArrayList();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {

            String partName = parts.get(partIndex).getPartName();

            if (partName2PartIndex.containsKey(partName))
                throwException("Vehicle '" + vehicleName + "' names two parts '" + partName + "'.");

            partName2PartIndex.put(partName, partIndex);

            if (parts.get(partIndex).getRole() == VehiclePartRole.MAST) {
                parts.get(partIndex).setMastIndex(mastParts.size());
                mastParts.add(partIndex);
            }

            if (parts.get(partIndex).getRole().getControl() == VehiclePartControl.OPEN) {
                parts.get(partIndex).setDoorIndex(doorParts.size());
                doorParts.add(partIndex);
            }
        }

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            if (parts.get(partIndex).getRole() == VehiclePartRole.YARD)
                linkYard(parts, parts.get(partIndex), partName2PartIndex, vehicleName);

        int sailCount = 0;

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            if (parts.get(partIndex).getRole() == VehiclePartRole.SAIL)
                linkSail(parts, parts.get(partIndex), partName2PartIndex, sailCount++, vehicleName);

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            linkControl(parts, partIndex, partName2PartIndex, vehicleName);

        return mastParts;
    }

    // A control works itself; a structure linked to a control is a fitting of it and works it too
    private void linkControl(
            ObjectArrayList<VehiclePartStruct> parts,
            int partIndex,
            Object2IntOpenHashMap<String> partName2PartIndex,
            String vehicleName) {

        VehiclePartStruct part = parts.get(partIndex);

        if (part.getRole().getControl() != VehiclePartControl.NONE) {
            part.setControlIndex(partIndex);
            return;
        }

        if (part.getRole() != VehiclePartRole.STRUCTURE || !part.hasLink())
            return;

        int controlIndex = partName2PartIndex.getInt(part.getLinkName());

        if (controlIndex == EngineSetting.INDEX_NOT_FOUND
                || parts.get(controlIndex).getRole().getControl() == VehiclePartControl.NONE)
            throwException("Vehicle '" + vehicleName + "' structure '" + part.getPartName()
                    + "' must link to a control by name, but links to '" + part.getLinkName() + "'.");

        part.setControlIndex(controlIndex);
    }

    private void linkYard(
            ObjectArrayList<VehiclePartStruct> parts,
            VehiclePartStruct yard,
            Object2IntOpenHashMap<String> partName2PartIndex,
            String vehicleName) {

        VehiclePartStruct mast = findLinkedPart(parts, yard, VehiclePartRole.MAST, partName2PartIndex, vehicleName);

        yard.setMastIndex(mast.getMastIndex());
    }

    private void linkSail(
            ObjectArrayList<VehiclePartStruct> parts,
            VehiclePartStruct sail,
            Object2IntOpenHashMap<String> partName2PartIndex,
            int sailIndex,
            String vehicleName) {

        VehiclePartStruct yard = findLinkedPart(parts, sail, VehiclePartRole.YARD, partName2PartIndex, vehicleName);

        sail.setYardIndex(partName2PartIndex.getInt(yard.getPartName()));
        sail.setMastIndex(yard.getMastIndex());
        sail.setSailIndex(sailIndex);
    }

    private VehiclePartStruct findLinkedPart(
            ObjectArrayList<VehiclePartStruct> parts,
            VehiclePartStruct part,
            VehiclePartRole linkedRole,
            Object2IntOpenHashMap<String> partName2PartIndex,
            String vehicleName) {

        int linkedIndex = partName2PartIndex.getInt(part.getLinkName());

        if (!part.hasLink() || linkedIndex == EngineSetting.INDEX_NOT_FOUND
                || parts.get(linkedIndex).getRole() != linkedRole)
            throwException("Vehicle '" + vehicleName + "' " + ArpgUtility.toEnumName(part.getRole()) + " '"
                    + part.getPartName() + "' must link to a " + ArpgUtility.toEnumName(linkedRole)
                    + " by name, but links to '" + part.getLinkName() + "'.");

        return parts.get(linkedIndex);
    }

    private int findSinglePart(ObjectArrayList<VehiclePartStruct> parts, VehiclePartRole role, String vehicleName) {

        int found = EngineSetting.INDEX_NOT_FOUND;

        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {

            if (parts.get(partIndex).getRole() != role)
                continue;

            if (found != EngineSetting.INDEX_NOT_FOUND)
                throwException("Vehicle '" + vehicleName + "' has more than one " + ArpgUtility.toEnumName(role)
                        + "; it can have at most one.");

            found = partIndex;
        }

        return found;
    }

    // Rig \\

    // Each sail's canvas from its bounds: the area it spreads across its yard, the centre it pulls through, its head
    private ObjectArrayList<VehicleSailStruct> buildSails(ObjectArrayList<VehiclePartStruct> parts) {

        ObjectArrayList<VehicleSailStruct> sails = new ObjectArrayList<>();
        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;

        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {

            VehiclePartStruct part = parts.get(partIndex);

            if (part.getRole() != VehiclePartRole.SAIL)
                continue;

            float height = (part.getMaxY() - part.getMinY()) * scale;
            float width = (part.getMaxZ() - part.getMinZ()) * scale;

            sails.add(new VehicleSailStruct(
                    partIndex,
                    part.getYardIndex(),
                    part.getMastIndex(),
                    height * width,
                    new Vector3(
                            (part.getMinX() + part.getMaxX()) * 0.5f * scale,
                            (part.getMinY() + part.getMaxY()) * 0.5f * scale,
                            (part.getMinZ() + part.getMaxZ()) * 0.5f * scale),
                    part.getMaxY() * scale));
        }

        return sails;
    }

    // Every box of every ladder, reaching out a little either side so a climber can take hold from beside it; a
    // zone per box keeps a ladder laid along a curved side as close to it as its rungs are
    private IntArrayList buildClimbZones(ObjectArrayList<VehiclePartStruct> parts) {

        IntArrayList climbZones = new IntArrayList();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {

            VehiclePartStruct part = parts.get(partIndex);

            if (!part.getRole().isClimbable())
                continue;

            for (int box = 0; box < part.getBoxCount(); box++)
                addClimbZone(climbZones, part, box);
        }

        return climbZones;
    }

    private void addClimbZone(IntArrayList climbZones, VehiclePartStruct part, int box) {

        int reach = EngineSetting.VEHICLE_LADDER_REACH_SUB_VOXELS;

        climbZones.add(part.getBoxBound(box, EngineSetting.BOX_MIN_X) - reach);
        climbZones.add(part.getBoxBound(box, EngineSetting.BOX_MIN_Y));
        climbZones.add(part.getBoxBound(box, EngineSetting.BOX_MIN_Z) - reach);
        climbZones.add(part.getBoxBound(box, EngineSetting.BOX_MAX_X) + reach);
        climbZones.add(part.getBoxBound(box, EngineSetting.BOX_MAX_Y) + reach);
        climbZones.add(part.getBoxBound(box, EngineSetting.BOX_MAX_Z) + reach);
    }

    // Handling \\

    private VehicleHandlingStruct parseHandling(ArpgObjectStruct arpg, ArpgObjectStruct hullArpg) {

        ArpgObjectStruct rigArpg = resolveGroup(arpg, "rig");
        ArpgObjectStruct steeringArpg = resolveGroup(arpg, "steering");
        ArpgObjectStruct anchorArpg = resolveGroup(arpg, "anchor");

        return new VehicleHandlingStruct(
                ArpgUtility.getFloat(hullArpg, "surge_drag", EngineSetting.DEFAULT_VEHICLE_SURGE_DRAG),
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

    // Bounds \\

    private int[] resolveBounds(ObjectArrayList<VehiclePartStruct> parts) {

        int[] bounds = {
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };

        for (int i = 0; i < parts.size(); i++) {

            VehiclePartStruct part = parts.get(i);

            bounds[EngineSetting.BOX_MIN_X] = Math.min(bounds[EngineSetting.BOX_MIN_X], part.getMinX());
            bounds[EngineSetting.BOX_MIN_Y] = Math.min(bounds[EngineSetting.BOX_MIN_Y], part.getMinY());
            bounds[EngineSetting.BOX_MIN_Z] = Math.min(bounds[EngineSetting.BOX_MIN_Z], part.getMinZ());
            bounds[EngineSetting.BOX_MAX_X] = Math.max(bounds[EngineSetting.BOX_MAX_X], part.getMaxX());
            bounds[EngineSetting.BOX_MAX_Y] = Math.max(bounds[EngineSetting.BOX_MAX_Y], part.getMaxY());
            bounds[EngineSetting.BOX_MAX_Z] = Math.max(bounds[EngineSetting.BOX_MAX_Z], part.getMaxZ());
        }

        return bounds;
    }

    // The farthest corner of the model grid's bounds from the centre of mass, in blocks
    private float resolveBoundingRadius(int[] bounds, Vector3 centerOfMass) {

        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;
        float reachX = Math.max(
                Math.abs(bounds[EngineSetting.BOX_MIN_X] * scale - centerOfMass.x),
                Math.abs(bounds[EngineSetting.BOX_MAX_X] * scale - centerOfMass.x));
        float reachY = Math.max(
                Math.abs(bounds[EngineSetting.BOX_MIN_Y] * scale - centerOfMass.y),
                Math.abs(bounds[EngineSetting.BOX_MAX_Y] * scale - centerOfMass.y));
        float reachZ = Math.max(
                Math.abs(bounds[EngineSetting.BOX_MIN_Z] * scale - centerOfMass.z),
                Math.abs(bounds[EngineSetting.BOX_MAX_Z] * scale - centerOfMass.z));

        return (float) Math.sqrt(reachX * reachX + reachY * reachY + reachZ * reachZ);
    }

    // Utility \\

    // The last segment of a vehicle's resource name, used as its display name when it gives none
    private String resolveLocalName(String vehicleName) {
        return vehicleName.substring(vehicleName.lastIndexOf(EngineSetting.PATH_SEPARATOR) + 1);
    }
}
