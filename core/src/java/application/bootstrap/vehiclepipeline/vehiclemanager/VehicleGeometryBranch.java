package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelQuadListStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class VehicleGeometryBranch extends BranchPackage {

    /*
     * Turns a vehicle's parts into sub-voxels and meshes. Parts fill their
     * boxes in data order, a later part taking a cell from an earlier one.
     * Every solid part fills the solid grid riders and cargo collide with;
     * every part that never moves is meshed together into the hull meshes,
     * and every moving part is meshed alone. Meshing is SubVoxelManager's one
     * greedy mesher, which merges each part's faces across block boundaries
     * so a long plank is one quad end to end; a face carries only its part's
     * texture corner and the vehicle shader repeats the texture once per
     * block. Rope and netting are flat quads laid alongside, each drawn from
     * both sides with its texture's cut-outs left open, so a whole shroud is
     * a handful of quads however many ratlines its texture shows. A mesh is
     * cut before it passes the mesh vertex limit. The
     * triangles a set of parts would draw are counted through the same mesher
     * without building a mesh, so a design is measured exactly as it will be
     * drawn. Lives for the engine lifetime, so a vehicle designed while the
     * game runs is meshed exactly as one loaded at boot.
     */

    // Internal
    private SubVoxelManager subVoxelManager;

    // Scratch — main thread only
    private SubVoxelGridStruct countGrid;
    private SubVoxelQuadListStruct countQuads;
    private FloatArrayList quadVertices;
    private ShortArrayList quadIndices;
    private float[] cornerScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.countGrid = new SubVoxelGridStruct();
        this.countQuads = new SubVoxelQuadListStruct();
        this.quadVertices = new FloatArrayList();
        this.quadIndices = new ShortArrayList();
        this.cornerScratch = new float[EngineSetting.QUAD_VERTEX_COUNT * EngineSetting.AXIS_COUNT];
    }

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
        float[] partUVBounds = resolveUVBounds(parts);

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            if (parts.get(partIndex).getRole().isStatic())
                rasterize(grid, parts.get(partIndex), partIndex);

        ObjectArrayList<MeshInstance> meshes = subVoxelManager.createGridMeshes(grid, partUVBounds);

        quadVertices.clear();
        quadIndices.clear();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            if (parts.get(partIndex).getRole().isStatic())
                emitPartQuads(parts.get(partIndex), partIndex, partUVBounds, meshes);

        flushQuads(meshes);

        return meshes;
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

            quadVertices.clear();
            quadIndices.clear();
            emitPartQuads(part, partIndex, partUVBounds, meshes);
            flushQuads(meshes);

            for (int i = 0; i < meshes.size(); i++)
                part.addMesh(meshes.get(i));
        }
    }

    // Quads \\

    // Every quad of a part written from both sides, a mesh cut off whenever the next would pass the vertex limit
    private void emitPartQuads(
            VehiclePartStruct part,
            int partIndex,
            float[] partUVBounds,
            ObjectArrayList<MeshInstance> meshes) {

        int uvBase = partIndex * EngineSetting.SUB_VOXEL_UV_BOUNDS_FLOATS;
        int sideVertices = EngineSetting.QUAD_VERTEX_COUNT * EngineSetting.VEHICLE_QUAD_SIDES;

        for (int quad = 0; quad < part.getQuadCount(); quad++) {

            if (quadVertices.size() / EngineSetting.SUB_VOXEL_VERTEX_STRIDE + sideVertices
                    > EngineSetting.MESH_VERT_LIMIT)
                flushQuads(meshes);

            int face = part.getQuadFace(quad);
            int back = subVoxelManager.resolveFace(subVoxelManager.getFaceAxis(face),
                    subVoxelManager.getFaceNormal(face, subVoxelManager.getFaceAxis(face)) < 0);

            writeCorners(part, quad, false);
            subVoxelManager.emitQuad(face, cornerScratch, partUVBounds, uvBase, quadVertices, quadIndices);
            writeCorners(part, quad, true);
            subVoxelManager.emitQuad(back, cornerScratch, partUVBounds, uvBase, quadVertices, quadIndices);
        }
    }

    // A quad's corners in blocks, in its own winding or the reverse for its back
    private void writeCorners(VehiclePartStruct part, int quad, boolean reversed) {

        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;

        for (int corner = 0; corner < EngineSetting.QUAD_VERTEX_COUNT; corner++) {

            int source = reversed ? EngineSetting.QUAD_VERTEX_COUNT - 1 - corner : corner;

            for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
                cornerScratch[corner * EngineSetting.AXIS_COUNT + axis] =
                        part.getQuadCorner(quad, source, axis) * scale;
        }
    }

    // The quads gathered so far uploaded as one mesh
    private void flushQuads(ObjectArrayList<MeshInstance> meshes) {

        if (quadIndices.isEmpty())
            return;

        meshes.add(subVoxelManager.createMesh(quadVertices, quadIndices));
        quadVertices.clear();
        quadIndices.clear();
    }

    // Count \\

    // The triangles every part draws, its unmoving parts merged together and each moving part on its own
    int countTriangles(ObjectArrayList<VehiclePartStruct> parts) {

        countGrid.clear();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            if (parts.get(partIndex).getRole().isStatic())
                rasterize(countGrid, parts.get(partIndex), partIndex);

        subVoxelManager.meshGrid(countGrid, null, countQuads);
        int quads = countQuads.size();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {

            if (parts.get(partIndex).getRole().isStatic())
                continue;

            countGrid.clear();
            rasterize(countGrid, parts.get(partIndex), partIndex);
            subVoxelManager.meshGrid(countGrid, null, countQuads);
            quads += countQuads.size();
        }

        countGrid.clear();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++)
            quads += parts.get(partIndex).getQuadCount() * EngineSetting.VEHICLE_QUAD_SIDES;

        return quads * EngineSetting.QUAD_TRIANGLE_COUNT;
    }

    // Utility \\

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
