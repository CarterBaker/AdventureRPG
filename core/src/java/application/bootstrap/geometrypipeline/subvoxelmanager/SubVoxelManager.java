package application.bootstrap.geometrypipeline.subvoxelmanager;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelQuadListStruct;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Int;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class SubVoxelManager extends ManagerPackage {

    /*
     * Engine entry point for sub-voxel surfaces. meshGrid() is the one greedy
     * mesher every sub-voxel surface goes through — item models, vehicles and
     * trees alike — merging faces across block boundaries into format-free
     * quads, and any thread may call it, each meshing on its own scratch.
     * Item-format meshes are built here from those quads: a model's single
     * mesh, a grid's meshes cut at the mesh vertex limit, a pocket's open box,
     * and live editor meshes, each face carrying its part's texture corner for
     * the item shaders to repeat once per block. Also the single access point
     * for sub-voxel raycasting, the mesh format, converting authored quad
     * meshes into sub-voxel models, and registering generated models as named
     * meshes.
     */

    // Internal
    private TextureManager textureManager;
    private VAOManager vaoManager;
    private MeshManager meshManager;
    private SubVoxelMeshAsyncContainer meshContainer;

    // Model Scratch — main thread only
    private SubVoxelGridStruct modelGrid;
    private SubVoxelQuadListStruct modelQuads;
    private float[] minScratch;
    private float[] maxScratch;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.meshContainer = create(SubVoxelMeshAsyncContainer.class);

        // Model Scratch
        this.modelGrid = new SubVoxelGridStruct();
        this.modelQuads = new SubVoxelQuadListStruct();
        this.minScratch = new float[EngineSetting.AXIS_COUNT];
        this.maxScratch = new float[EngineSetting.AXIS_COUNT];
    }

    @Override
    protected void get() {

        // Internal
        this.textureManager = get(TextureManager.class);
        this.vaoManager = get(VAOManager.class);
        this.meshManager = get(MeshManager.class);
    }

    // Grid \\

    // Every face of the grid merged into quads — partOpaque marks the parts that hide what lies behind them, null
    // when every part does
    public void meshGrid(SubVoxelGridStruct grid, boolean[] partOpaque, SubVoxelQuadListStruct out) {
        meshGrid(grid, partOpaque, 0, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, out);
    }

    // The faces of the blocks inside an inclusive block region merged into quads; the blocks around it are only
    // read to decide what shows
    public void meshGrid(
            SubVoxelGridStruct grid,
            boolean[] partOpaque,
            int minBlockX,
            int minBlockY,
            int minBlockZ,
            int maxBlockX,
            int maxBlockY,
            int maxBlockZ,
            SubVoxelQuadListStruct out) {

        out.clear();

        SubVoxelGridMeshUtility.mesh(
                grid, partOpaque,
                minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ,
                meshContainer.getInstance(), out);
    }

    // Every face of the grid as item-format meshes, each cut before it passes the mesh vertex limit
    public ObjectArrayList<MeshInstance> createGridMeshes(SubVoxelGridStruct grid, int[] partTiles) {

        ObjectArrayList<MeshInstance> meshes = new ObjectArrayList<>();
        SubVoxelQuadListStruct quads = new SubVoxelQuadListStruct();
        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();
        int quadsPerMesh = EngineSetting.MESH_VERT_LIMIT / EngineSetting.QUAD_VERTEX_COUNT;

        meshGrid(grid, null, quads);

        for (int first = 0; first < quads.size(); first += quadsPerMesh) {

            vertices.clear();
            indices.clear();

            SubVoxelMeshUtility.emitQuads(
                    quads, first, Math.min(first + quadsPerMesh, quads.size()), partTiles,
                    minScratch, maxScratch, vertices, indices);

            meshes.add(createMesh(vertices, indices));
        }

        return meshes;
    }

    // Geometry \\

    public int getVertexCount(SubVoxelModelStruct model) {

        meshModel(model);

        return modelQuads.size() * EngineSetting.QUAD_VERTEX_COUNT;
    }

    public boolean fitsMeshLimit(SubVoxelModelStruct model) {
        return getVertexCount(model) <= EngineSetting.MESH_VERT_LIMIT;
    }

    public void buildGeometry(
            SubVoxelModelStruct model,
            FloatArrayList vertices,
            ShortArrayList indices) {

        int vertexCount = getVertexCount(model);

        if (vertexCount > EngineSetting.MESH_VERT_LIMIT)
            throwException("Sub-voxel model needs " + vertexCount + " vertices, over the mesh limit of "
                    + EngineSetting.MESH_VERT_LIMIT + ".");

        vertices.clear();
        indices.clear();

        SubVoxelMeshUtility.emitQuads(
                modelQuads, 0, modelQuads.size(), resolvePartTiles(model),
                minScratch, maxScratch, vertices, indices);
    }

    // The model's cubes and walls meshed into the model quads
    private void meshModel(SubVoxelModelStruct model) {
        SubVoxelMeshUtility.writeGrid(model, modelGrid);
        meshGrid(modelGrid, null, modelQuads);
    }

    // Every part's tile ID, indexed by part
    private int[] resolvePartTiles(SubVoxelModelStruct model) {

        int[] partTiles = new int[model.getPartCount()];

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            partTiles[partIndex] = resolveTile(model.getPart(partIndex).getTextureName());

        return partTiles;
    }

    public int resolveTile(String textureName) {
        return textureManager.getTileIDFromTextureName(textureName);
    }

    // Runtime Mesh \\

    public MeshInstance createMesh(SubVoxelModelStruct model) {

        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();
        buildGeometry(model, vertices, indices);

        return createMesh(vertices, indices);
    }

    // A generated model registered with MeshManager under its own name, drawn like any loaded mesh
    public MeshHandle createMeshHandle(String meshName, SubVoxelModelStruct model) {

        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();
        buildGeometry(model, vertices, indices);

        VAOHandle vaoTemplate = vaoManager.getVAOHandleFromVAOName(EngineSetting.SUB_VOXEL_VAO);
        MeshHandle meshHandle = meshManager.createMeshHandle(meshName, vaoTemplate, vertices, indices);
        SubVoxelMeshUtility.collectTiles(vertices.elements(), vertices.size(), meshHandle.getTiles());

        return meshHandle;
    }

    // A pocket's open box around a space of the given size in sub-voxels, its corner at the origin
    public MeshInstance createPocketMesh(Vector3Int size, String textureName) {

        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();
        SubVoxelMeshUtility.buildPocket(size.x, size.y, size.z, resolveTile(textureName), vertices, indices);

        return createMesh(vertices, indices);
    }

    // Geometry already written through emitFace(), uploaded as a mesh of the sub-voxel format
    public MeshInstance createMesh(FloatArrayList vertices, ShortArrayList indices) {

        VAOHandle vaoTemplate = vaoManager.getVAOHandleFromVAOName(EngineSetting.SUB_VOXEL_VAO);
        return meshManager.createMesh(vaoTemplate, vertices, indices);
    }

    public void updateMesh(MeshInstance meshInstance, SubVoxelModelStruct model) {

        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList indices = new ShortArrayList();
        buildGeometry(model, vertices, indices);

        meshManager.updateMesh(meshInstance, vertices, indices);
    }

    // Faces \\

    // One face spanning min to max in block units carrying its tile — the same vertices every item-format sub-voxel
    // mesh is made of
    public void emitFace(
            int face,
            float[] min,
            float[] max,
            int tile,
            FloatArrayList vertices,
            ShortArrayList indices) {
        SubVoxelMeshUtility.emitFace(face, min, max, tile, vertices, indices);
    }

    // One quad through four corners in block units, wound as given, carrying its tile — a face laid at any slant,
    // read by the shader as the face given
    public void emitQuad(
            int face,
            float[] corners,
            int tile,
            FloatArrayList vertices,
            ShortArrayList indices) {
        SubVoxelMeshUtility.emitCorners(face, corners, tile, vertices, indices);
    }

    // The face whose normal runs along an axis, toward its positive or negative side
    public int resolveFace(int axis, boolean positive) {
        return SubVoxelMeshUtility.resolveFace(axis, positive);
    }

    // One component of a face's outward normal, faces numbered as the item shader numbers them
    public int getFaceNormal(int face, int axis) {
        return SubVoxelMeshUtility.getFaceNormal(face, axis);
    }

    // The axis a face's normal runs along
    public int getFaceAxis(int face) {
        return SubVoxelMeshUtility.resolveAxis(face);
    }

    // Raycast \\

    public boolean raycast(
            SubVoxelModelStruct model,
            Vector3 origin,
            Vector3 direction,
            SubVoxelHitStruct hit) {
        return SubVoxelRaycastUtility.raycast(model, origin, direction, hit);
    }

    // Format \\

    public boolean hasSubVoxels(ArpgObjectStruct meshArpg) {
        return SubVoxelArpgUtility.hasSubVoxels(meshArpg);
    }

    public SubVoxelModelStruct parseModel(ArpgObjectStruct meshArpg) {
        return SubVoxelArpgUtility.parse(meshArpg);
    }

    public ArpgObjectStruct toMeshArpg(SubVoxelModelStruct model) {
        return SubVoxelArpgUtility.toMeshArpg(model);
    }

    // Parts \\

    public int findTexturePart(SubVoxelModelStruct model, String textureName) {
        return SubVoxelPartUtility.findTexturePart(model, textureName);
    }

    public int addTexturePart(SubVoxelModelStruct model, String textureName) {
        return SubVoxelPartUtility.addTexturePart(model, textureName);
    }

    // Import \\

    public boolean hasQuads(ArpgObjectStruct meshArpg) {
        return SubVoxelImportUtility.hasQuads(meshArpg);
    }

    public SubVoxelModelStruct importQuadMesh(ArpgObjectStruct meshArpg, String fallbackTextureName) {
        return SubVoxelImportUtility.importQuads(meshArpg, fallbackTextureName);
    }

    // Either format as sub-voxels — null when the mesh holds neither cubes nor quads
    public SubVoxelModelStruct resolveModel(ArpgObjectStruct meshArpg, String fallbackTextureName) {

        if (hasSubVoxels(meshArpg))
            return parseModel(meshArpg);

        if (hasQuads(meshArpg))
            return importQuadMesh(meshArpg, fallbackTextureName);

        return null;
    }
}
