package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleData;
import application.bootstrap.vehiclepipeline.vehicle.VehicleDesignStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHullStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartControl;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleSailStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class VehicleAssemblyBranch extends BranchPackage {

    /*
     * Assembles a vehicle type from its design, the one way any vehicle type
     * is made, whether read from a file at boot or drawn at the drafting table
     * while the game runs. Its parts are linked by name, a yard to its mast
     * and a sail to its yard, a structure to the control it is a fitting of,
     * and every mast and door numbered in data order; there is at most one
     * helm, one rudder and one capstan. Geometry and the hull's physics are
     * worked out by VehicleGeometryBranch and VehicleHullFormBranch, each sail
     * spreads across the bounds of its part, and every ladder box can be
     * climbed from a little either side of it. A malformed design fails here,
     * before it can enter the world. Releasing a type frees every mesh it drew
     * with.
     */

    // Internal
    private VehicleGeometryBranch vehicleGeometryBranch;
    private VehicleHullFormBranch vehicleHullFormBranch;
    private MeshManager meshManager;

    // Base \\

    @Override
    protected void get() {
        this.vehicleGeometryBranch = get(VehicleGeometryBranch.class);
        this.vehicleHullFormBranch = get(VehicleHullFormBranch.class);
        this.meshManager = get(MeshManager.class);
    }

    // Assemble \\

    VehicleHandle assemble(VehicleDesignStruct design) {

        String vehicleName = design.getVehicleName();
        ObjectArrayList<VehiclePartStruct> parts = design.getParts();

        verifyParts(parts, vehicleName);

        IntArrayList doorParts = new IntArrayList();
        IntArrayList mastParts = linkParts(parts, doorParts, vehicleName);
        SubVoxelGridStruct solidGrid = vehicleGeometryBranch.buildSolidGrid(parts);
        ObjectArrayList<MeshInstance> hullMeshes = vehicleGeometryBranch.buildHullMeshes(parts);
        vehicleGeometryBranch.buildPartMeshes(parts);

        VehicleHullStruct hull = vehicleHullFormBranch.build(
                vehicleName, parts, design.getDraft(), design.getCenterOfMassHeight());
        int[] bounds = resolveBounds(parts);

        VehicleData vehicleData = new VehicleData(
                vehicleName,
                design.getDisplayName(),
                design.getCategory(),
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
                design.getHandling(),
                design.getCargoSlots());

        VehicleHandle vehicleHandle = create(VehicleHandle.class);
        vehicleHandle.constructor(vehicleData);

        return vehicleHandle;
    }

    // Survey \\

    // The hull a set of parts would float as, worked out without meshing or linking anything
    VehicleHullStruct surveyHull(
            String vehicleName,
            ObjectArrayList<VehiclePartStruct> parts,
            float draft,
            float centerOfMassHeight) {
        return vehicleHullFormBranch.build(vehicleName, parts, draft, centerOfMassHeight);
    }

    int countTriangles(ObjectArrayList<VehiclePartStruct> parts) {
        return vehicleGeometryBranch.countTriangles(parts);
    }

    SubVoxelGridStruct buildSolidGrid(ObjectArrayList<VehiclePartStruct> parts) {
        return vehicleGeometryBranch.buildSolidGrid(parts);
    }

    // Release \\

    // Every mesh the type draws with, once nothing will draw it again
    void release(VehicleHandle vehicleHandle) {

        ObjectArrayList<MeshInstance> hullMeshes = vehicleHandle.getHullMeshes();

        for (int i = 0; i < hullMeshes.size(); i++)
            meshManager.removeMesh(hullMeshes.get(i));

        for (int partIndex = 0; partIndex < vehicleHandle.getPartCount(); partIndex++) {

            ObjectArrayList<MeshInstance> meshes = vehicleHandle.getPart(partIndex).getMeshes();

            for (int i = 0; i < meshes.size(); i++)
                meshManager.removeMesh(meshes.get(i));
        }
    }

    // Parts \\

    private void verifyParts(ObjectArrayList<VehiclePartStruct> parts, String vehicleName) {

        if (parts.size() > EngineSetting.SUB_VOXEL_MAX_PARTS)
            throwException("Vehicle '" + vehicleName + "' lists " + parts.size() + " parts, over the "
                    + EngineSetting.SUB_VOXEL_MAX_PARTS + " a vehicle can hold.");

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            if (parts.get(partIndex).isEmpty())
                throwException("Vehicle '" + vehicleName + "' part '" + parts.get(partIndex).getPartName()
                        + "' fills no sub-voxels.");
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
}
