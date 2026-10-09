package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class VehicleGeometryBuilder extends BuilderPackage {

    /*
     * Turns a vehicle's parts into sub-voxels and meshes. Parts fill their
     * boxes in data order, a later part taking a cell from an earlier one.
     * Every solid part fills the solid grid riders and cargo collide with;
     * every part that never moves is meshed together into the hull meshes,
     * and every moving part is meshed alone. Meshing is SubVoxelManager's one
     * greedy mesher, which merges each part's faces across block boundaries
     * so a long plank is one quad end to end; a face carries only its part's
     * texture corner and the vehicle shader repeats the texture once per
     * block. A mesh is cut before it passes the mesh vertex limit.
     */

    // Internal
    private SubVoxelManager subVoxelManager;

    // Base \\

    @Override
    protected void get() {
        this.subVoxelManager = get(SubVoxelManager.class);
    }

    // Grid \\

    SubVoxelGridStruct buildSolidGrid(ObjectArrayList<VehiclePartStruct> parts) {

        SubVoxelGridStruct grid = new SubVoxelGridStruct();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            if (parts.get(partIndex).getRole().isSolid())
                rasterize(grid, parts.get(partIndex), partIndex);

        return grid;
    }

    private void rasterize(SubVoxelGridStruct grid, VehiclePartStruct part, int partIndex) {

        for (int box = 0; box < part.getBoxCount(); box++)
            grid.fillBox(
                    part.getBoxBound(box, EngineSetting.BOX_MIN_X),
                    part.getBoxBound(box, EngineSetting.BOX_MIN_Y),
                    part.getBoxBound(box, EngineSetting.BOX_MIN_Z),
                    part.getBoxBound(box, EngineSetting.BOX_MAX_X),
                    part.getBoxBound(box, EngineSetting.BOX_MAX_Y),
                    part.getBoxBound(box, EngineSetting.BOX_MAX_Z),
                    partIndex,
                    false);
    }

    // Meshes \\

    // Every part that never moves merged into as few meshes as the limit allows
    ObjectArrayList<MeshInstance> buildHullMeshes(ObjectArrayList<VehiclePartStruct> parts) {

        SubVoxelGridStruct grid = new SubVoxelGridStruct();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            if (parts.get(partIndex).getRole().isStatic())
                rasterize(grid, parts.get(partIndex), partIndex);

        return subVoxelManager.createGridMeshes(grid, resolveUVBounds(parts));
    }

    // Every moving part meshed alone, so it can be drawn through its own transform
    void buildPartMeshes(ObjectArrayList<VehiclePartStruct> parts) {

        float[] partUVBounds = resolveUVBounds(parts);
        SubVoxelGridStruct grid = new SubVoxelGridStruct();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {

            VehiclePartStruct part = parts.get(partIndex);

            if (part.getRole().isStatic())
                continue;

            grid.clear();
            rasterize(grid, part, partIndex);

            ObjectArrayList<MeshInstance> meshes = subVoxelManager.createGridMeshes(grid, partUVBounds);

            for (int i = 0; i < meshes.size(); i++)
                part.addMesh(meshes.get(i));
        }
    }

    // The texture bounds of every part, four floats each
    private float[] resolveUVBounds(ObjectArrayList<VehiclePartStruct> parts) {

        float[] uvBounds = new float[parts.size() * EngineSetting.SUB_VOXEL_UV_BOUNDS_FLOATS];

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            subVoxelManager.writeUVBounds(
                    parts.get(partIndex).getTextureName(),
                    uvBounds,
                    partIndex * EngineSetting.SUB_VOXEL_UV_BOUNDS_FLOATS);

        return uvBounds;
    }
}
