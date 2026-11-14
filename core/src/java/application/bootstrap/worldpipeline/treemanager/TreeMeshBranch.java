package application.bootstrap.worldpipeline.treemanager;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelQuadListStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import application.bootstrap.worldpipeline.util.TreeMeshUtility;
import application.bootstrap.worldpipeline.util.TreeRasterUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class TreeMeshBranch extends BranchPackage {

    /*
     * Draws one tree shape whole, apart from any chunk, exactly as a chunk
     * would draw it in full — its wood laid on a grid of its own and merged
     * into quads, any wood a leaf cluster wholly hides left out, and every
     * leaf cluster — in blocks from the centre of its root, and uploads such
     * geometry as meshes in the chunk vertex layout, each cut before the
     * mesh vertex limit. A falling piece of tree and the editor's tree
     * preview are both drawn through here. buildShape() runs on any thread;
     * uploading and disposing are the main thread's.
     */

    // Internal
    private SubVoxelManager subVoxelManager;
    private MeshManager meshManager;
    private ChunkStreamManager chunkStreamManager;

    // Settings
    private int resolution;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
    }

    @Override
    protected void get() {
        this.subVoxelManager = get(SubVoxelManager.class);
        this.meshManager = get(MeshManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
    }

    // Build \\

    // Any thread — a shape's wood and leaves appended in blocks from the centre of its root
    void buildShape(TreeShapeStruct shape, FloatArrayList bark, FloatArrayList leaves) {

        SubVoxelGridStruct grid = new SubVoxelGridStruct();
        SubVoxelQuadListStruct quads = new SubVoxelQuadListStruct();
        ObjectArrayList<TreeHandle> partSpecies = new ObjectArrayList<>();
        FloatArrayList hiders = new FloatArrayList();
        int rootX = (int) Math.ceil(-shape.getMinX()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS;
        int rootY = (int) Math.ceil(-shape.getMinY()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS;
        int rootZ = (int) Math.ceil(-shape.getMinZ()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS;
        int sizeX = (rootX + (int) Math.ceil(shape.getMaxX()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS) * resolution;
        int sizeY = (rootY + (int) Math.ceil(shape.getMaxY()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS) * resolution;
        int sizeZ = (rootZ + (int) Math.ceil(shape.getMaxZ()) + EngineSetting.TREE_GEOMETRY_MARGIN_BLOCKS) * resolution;

        partSpecies.add(shape.getTreeHandle());

        if (!shape.getNodes().isEmpty()) {

            TreeRasterUtility.rasterize(shape, rootX, rootY, rootZ, 0, 0, 0, sizeX, sizeY, sizeZ, 0, grid);
            subVoxelManager.meshGrid(grid, null, quads);
            TreeMeshUtility.collectHiders(
                    shape,
                    (rootX + EngineSetting.TREE_ROOT_CENTER_BLOCKS) * resolution, rootY * resolution,
                    (rootZ + EngineSetting.TREE_ROOT_CENTER_BLOCKS) * resolution, hiders);
            TreeMeshUtility.emitWood(
                    grid, quads, partSpecies, hiders,
                    -(rootX + EngineSetting.TREE_ROOT_CENTER_BLOCKS), -rootY,
                    -(rootZ + EngineSetting.TREE_ROOT_CENTER_BLOCKS), bark);
        }

        TreeMeshUtility.emitLeaves(
                shape, 0f, 0f, 0f,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE,
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, leaves);
    }

    // Meshes \\

    // Geometry in the chunk vertex layout uploaded as meshes, each cut before the mesh vertex limit
    void uploadMeshes(FloatArrayList vertices, ObjectArrayList<MeshInstance> out) {

        int stride = EngineSetting.CHUNK_VERTEX_FLOAT_COUNT;
        int quadFloats = stride * EngineSetting.QUAD_VERTEX_COUNT;
        int quadsPerMesh = EngineSetting.MESH_VERT_LIMIT / EngineSetting.QUAD_VERTEX_COUNT;
        int quadCount = vertices.size() / quadFloats;

        for (int first = 0; first < quadCount; first += quadsPerMesh) {

            int count = Math.min(quadsPerMesh, quadCount - first);
            FloatArrayList meshVertices = new FloatArrayList(count * quadFloats);
            ShortArrayList indices = new ShortArrayList(count * EngineSetting.QUAD_INDEX_COUNT);

            meshVertices.addElements(0, vertices.elements(), first * quadFloats, count * quadFloats);

            for (int quad = 0; quad < count; quad++) {

                int base = quad * EngineSetting.QUAD_VERTEX_COUNT;

                indices.add((short) base);
                indices.add((short) (base + 1));
                indices.add((short) (base + 2));
                indices.add((short) base);
                indices.add((short) (base + 2));
                indices.add((short) (base + 3));
            }

            out.add(meshManager.createMesh(chunkStreamManager.getChunkVAO(), meshVertices, indices));
        }
    }

    void disposeMeshes(ObjectArrayList<MeshInstance> meshes) {

        for (int i = 0; i < meshes.size(); i++)
            meshManager.removeMesh(meshes.get(i));

        meshes.clear();
    }
}
