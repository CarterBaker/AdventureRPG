package application.bootstrap.geometrypipeline.meshmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.ibo.IBOHandle;
import application.bootstrap.geometrypipeline.ibo.IBOInstance;
import application.bootstrap.geometrypipeline.ibomanager.IBOManager;
import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.geometrypipeline.vbo.VBOHandle;
import application.bootstrap.geometrypipeline.vbo.VBOInstance;
import application.bootstrap.geometrypipeline.vbomanager.VBOManager;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class MeshManager extends ManagerPackage {

    /*
     * Central registry for all GPU-resident mesh data. Owns the name-to-ID
     * and ID-to-handle palettes, with IDs assigned in registration order,
     * drives the mesh load pipeline via MeshLoader, and handles runtime mesh
     * creation, in-place updating, and removal by delegating buffer
     * operations to VAOManager, VBOManager, and IBOManager. createMeshHandle()
     * registers a mesh generated at runtime under a name, exactly like a
     * loaded one. Rigged meshes pass through RiggedMeshStruct: parsed from
     * and written to ARPG, built into shared, packed skinned vertex data with
     * one reusable scratch (indexed by unsigned shorts unless the mesh has
     * more vertices than they reach), and rebuilt in place on their registered handle when an editor
     * changes one, so every view drawing it shows the change at once; their
     * morph targets ride along in a texture on the handle.
     */

    // Internal
    private VAOManager vaoManager;
    private VBOManager vboManager;
    private IBOManager iboManager;
    private RigManager rigManager;
    private TextureManager textureManager;

    // Rigged
    private RiggedMeshScratchStruct riggedScratch;
    private IntArrayList riggedVertices;
    private IntArrayList riggedIndices;
    private ShortArrayList riggedShortIndices;
    private FloatArrayList riggedMorphEntries;

    // Palette
    private Object2IntOpenHashMap<String> meshName2MeshID;
    private ObjectArrayList<MeshHandle> meshID2MeshHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.meshName2MeshID = RegistryUtility.createNameIndex();
        this.meshID2MeshHandle = RegistryUtility.createPalette();

        // Rigged
        this.riggedScratch = new RiggedMeshScratchStruct();
        this.riggedVertices = new IntArrayList();
        this.riggedIndices = new IntArrayList();
        this.riggedShortIndices = new ShortArrayList();
        this.riggedMorphEntries = new FloatArrayList();
        create(MeshLoader.class);
    }

    @Override
    protected void get() {

        // Internal
        this.vaoManager = get(VAOManager.class);
        this.vboManager = get(VBOManager.class);
        this.iboManager = get(IBOManager.class);
        this.rigManager = get(RigManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Management \\

    void addMeshHandle(String meshName, MeshHandle meshHandle) {
        RegistryUtility.registerHandle(
                meshName2MeshID, meshID2MeshHandle, meshName, meshHandle, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    // Bounds \\

    void computeBounds(FloatArrayList vertices, int vertStride, Vector3 outMin, Vector3 outMax) {

        int vertexCount = vertices.size() / vertStride;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < vertexCount; i++) {

            int base = i * vertStride;

            float x = vertices.getFloat(base);
            float y = vertices.getFloat(base + 1);
            float z = vertices.getFloat(base + 2);

            if (x < minX)
                minX = x;
            if (y < minY)
                minY = y;
            if (z < minZ)
                minZ = z;
            if (x > maxX)
                maxX = x;
            if (y > maxY)
                maxY = y;
            if (z > maxZ)
                maxZ = z;
        }

        outMin.set(minX, minY, minZ);
        outMax.set(maxX, maxY, maxZ);
    }

    // Accessible \\

    public void request(String resourceName) {
        ((MeshLoader) internalLoader).request(resourceName);
    }

    // A mesh file from outside the mesh directory made known under a name, loaded on demand like any other
    public void addMeshFile(String meshName, File file) {
        ((MeshLoader) internalLoader).registerFile(meshName, file);
    }

    public boolean hasMesh(String meshName) {
        return RegistryUtility.getHandle(meshName2MeshID, meshID2MeshHandle, meshName) != null;
    }

    public int getMeshIDFromMeshName(String meshName) {

        if (!hasMesh(meshName))
            request(meshName);

        return meshName2MeshID.getInt(meshName);
    }

    public MeshHandle getMeshHandleFromMeshID(int meshID) {
        return RegistryUtility.getHandle(meshID2MeshHandle, meshID);
    }

    public MeshHandle getMeshHandleFromMeshName(String meshName) {
        return getMeshHandleFromMeshID(getMeshIDFromMeshName(meshName));
    }

    // Runtime Mesh Creation \\

    public MeshInstance createMesh(
            VAOHandle vaoTemplate,
            FloatArrayList vertices,
            ShortArrayList indices) {

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoTemplate);
        VBOInstance vboInstance = vboManager.createVBOInstance(vaoInstance, vertices);
        IBOInstance iboInstance = iboManager.createIBOInstance(vaoInstance, indices);

        MeshInstance meshInstance = create(MeshInstance.class);
        meshInstance.constructor(vaoInstance, vboInstance, iboInstance);

        return meshInstance;
    }

    public void updateMesh(MeshInstance meshInstance, FloatArrayList vertices, ShortArrayList indices) {

        VAOInstance vaoInstance = meshInstance.getVAOInstance();
        VBOInstance vboInstance = vboManager.updateVBOInstance(vaoInstance, meshInstance.getVBOInstance(), vertices);
        IBOInstance iboInstance = iboManager.updateIBOInstance(meshInstance.getIBOInstance(), indices);

        meshInstance.constructor(vaoInstance, vboInstance, iboInstance);
    }

    // A runtime mesh indexed by unsigned ints, for one holding more vertices than a short reaches
    public MeshInstance createMesh(
            VAOHandle vaoTemplate,
            FloatArrayList vertices,
            IntArrayList indices) {

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoTemplate);
        VBOInstance vboInstance = vboManager.createVBOInstance(vaoInstance, vertices);
        IBOInstance iboInstance = iboManager.createIBOInstance(vaoInstance, indices);

        MeshInstance meshInstance = create(MeshInstance.class);
        meshInstance.constructor(vaoInstance, vboInstance, iboInstance);

        return meshInstance;
    }

    public void updateMesh(MeshInstance meshInstance, FloatArrayList vertices, IntArrayList indices) {

        VAOInstance vaoInstance = meshInstance.getVAOInstance();
        VBOInstance vboInstance = vboManager.updateVBOInstance(vaoInstance, meshInstance.getVBOInstance(), vertices);
        IBOInstance iboInstance = iboManager.updateIBOInstance(meshInstance.getIBOInstance(), indices);

        meshInstance.constructor(vaoInstance, vboInstance, iboInstance);
    }

    public MeshHandle createMeshHandle(
            String meshName,
            VAOHandle vaoTemplate,
            FloatArrayList vertices,
            ShortArrayList indices) {

        if (hasMesh(meshName))
            throwException("Mesh \"" + meshName + "\" is already registered.");

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoTemplate);
        VBOHandle vboHandle = vboManager.addVBOFromData(meshName, vertices.toFloatArray(), vaoInstance);
        IBOHandle iboHandle = iboManager.addIBOFromData(meshName, indices.toShortArray(), vaoInstance);

        Vector3 boundsMin = new Vector3();
        Vector3 boundsMax = new Vector3();
        computeBounds(vertices, vaoInstance.getVAOData().getVertStride(), boundsMin, boundsMax);

        MeshHandle meshHandle = create(MeshHandle.class);
        meshHandle.constructor(meshName, vaoInstance, vboHandle, iboHandle, null, boundsMin, boundsMax);
        addMeshHandle(meshName, meshHandle);

        return meshHandle;
    }

    // Rigged Meshes \\

    public boolean isRiggedMesh(ArpgObjectStruct meshArpg) {
        return RiggedMeshArpgUtility.isRigged(meshArpg);
    }

    public RiggedMeshStruct parseRiggedMesh(ArpgObjectStruct meshArpg, String sourceName) {

        RigHandle rigHandle = rigManager.getRigHandleFromRigName(RiggedMeshArpgUtility.getRigName(meshArpg));
        return RiggedMeshArpgUtility.parse(meshArpg, rigHandle, sourceName);
    }

    // A rigged mesh read against a rig given rather than the one it names, as a body part reads against its own bones
    public RiggedMeshStruct parseRiggedMesh(ArpgObjectStruct meshArpg, RigHandle rigHandle, String sourceName) {
        return RiggedMeshArpgUtility.parse(meshArpg, rigHandle, sourceName);
    }

    // The source file with its geometry rewritten from the mesh in the compact layout, every other field kept
    public ArpgObjectStruct toRiggedMeshArpg(RiggedMeshStruct mesh, ArpgObjectStruct sourceArpg) {
        return RiggedMeshArpgUtility.write(mesh, sourceArpg);
    }

    // The most quads the character tools edit
    public boolean fitsRiggedMeshLimit(RiggedMeshStruct mesh) {
        return fitsRiggedMeshLimit(mesh.getFaceCount());
    }

    public boolean fitsRiggedMeshLimit(int faceCount) {
        return faceCount * EngineSetting.QUAD_VERTEX_COUNT <= EngineSetting.RIGGED_MESH_VERT_LIMIT;
    }

    // Every corner's packed smooth normal, face by face, as a mesh file stores them and an export reads them
    public void packRiggedNormals(RiggedMeshStruct mesh, int[] normals) {
        RiggedMeshGeometryUtility.packNormals(mesh, riggedScratch, normals);
    }

    // The normals a rigged mesh file stores for the mesh just parsed from it, or null when they must be computed
    int[] parseRiggedNormals(ArpgObjectStruct meshArpg, RiggedMeshStruct mesh) {
        return RiggedMeshArpgUtility.parseNormals(meshArpg, mesh);
    }

    // Builds a rigged mesh into the manager's reusable vertex, index and morph entry lists, which the methods below
    // upload and measure until the next build; stored normals are those its file holds for it exactly as parsed
    void buildRiggedGeometry(RiggedMeshStruct mesh, int[] storedNormals) {

        if (mesh.getRigHandle().getBoneCount() > EngineSetting.RIGGED_MESH_BONE_LIMIT)
            throwException("Rig \"" + mesh.getRigHandle().getRigName() + "\" has more than "
                    + EngineSetting.RIGGED_MESH_BONE_LIMIT + " bones — a rigged vertex names its bones in a byte.");

        RiggedMeshGeometryUtility.build(
                mesh, resolveTextureTiles(mesh), riggedScratch, storedNormals,
                riggedVertices, riggedIndices, riggedMorphEntries);
    }

    boolean hasRiggedIndices() {
        return !riggedIndices.isEmpty();
    }

    VBOHandle addRiggedVBO(String meshName, VAOInstance vaoInstance) {
        return vboManager.addVBOFromData(meshName, riggedVertices.toIntArray(), vaoInstance);
    }

    IBOHandle addRiggedIBO(String meshName, VAOInstance vaoInstance) {

        if (fitsShortIndices())
            return iboManager.addIBOFromData(meshName, narrowRiggedIndices().toShortArray(), vaoInstance);

        return iboManager.addIBOFromData(meshName, riggedIndices.toIntArray(), vaoInstance);
    }

    private IBOHandle updateRiggedIBO(IBOHandle iboHandle) {

        if (fitsShortIndices())
            return iboManager.updateIBOHandle(iboHandle, narrowRiggedIndices());

        return iboManager.updateIBOHandle(iboHandle, riggedIndices);
    }

    private boolean fitsShortIndices() {
        return riggedVertices.size() / EngineSetting.RIGGED_MESH_VERTEX_INTS <= EngineSetting.RIGGED_MESH_VERT_LIMIT;
    }

    private ShortArrayList narrowRiggedIndices() {

        int indexCount = riggedIndices.size();
        int[] indices = riggedIndices.elements();

        riggedShortIndices.size(indexCount);

        short[] narrowed = riggedShortIndices.elements();

        for (int i = 0; i < indexCount; i++)
            narrowed[i] = (short) indices[i];

        return riggedShortIndices;
    }

    // The extent of the built vertices' positions, the first words of each
    void computeRiggedBounds(Vector3 outMin, Vector3 outMax) {

        int[] vertices = riggedVertices.elements();
        int intCount = riggedVertices.size();

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;

        for (int base = 0; base < intCount; base += EngineSetting.RIGGED_MESH_VERTEX_INTS) {

            float x = Float.intBitsToFloat(vertices[base]);
            float y = Float.intBitsToFloat(vertices[base + 1]);
            float z = Float.intBitsToFloat(vertices[base + 2]);

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        outMin.set(minX, minY, minZ);
        outMax.set(maxX, maxY, maxZ);
    }

    // Gives a rigged mesh's handle the morph texture made from the entries just built, releasing the one it held, and
    // the bones its vertices weigh onto
    void applyRiggedMesh(MeshHandle meshHandle, RiggedMeshStruct mesh) {

        String[] morphNames = new String[mesh.getMorphCount()];

        for (int m = 0; m < morphNames.length; m++)
            morphNames[m] = mesh.getMorphName(m);

        RiggedMeshGLSLUtility.deleteTexture(meshHandle.getMorphTexture());
        meshHandle.setMorphTargets(RiggedMeshGLSLUtility.createMorphTexture(riggedMorphEntries), morphNames);
        meshHandle.setWeighedBones(resolveWeighedBones(mesh));
        meshHandle.getTiles().clear();

        for (int t = 0; t < mesh.getTextureCount(); t++)
            meshHandle.getTiles().add(textureManager.getTileIDFromTextureName(mesh.getTextureName(t)));
    }

    private boolean[] resolveWeighedBones(RiggedMeshStruct mesh) {

        boolean[] weighed = new boolean[mesh.getRigHandle().getBoneCount()];

        for (int v = 0; v < mesh.getVertexCount(); v++)
            for (int i = 0; i < mesh.getInfluenceCount(v); i++)
                if (mesh.getInfluenceWeight(v, i) > 0f)
                    weighed[mesh.getInfluenceBone(v, i)] = true;

        return weighed;
    }

    // Rebuilds a registered rigged mesh from a struct, its buffers respecified in place on the same handles
    public void updateRiggedMesh(MeshHandle meshHandle, RiggedMeshStruct mesh) {

        if (meshHandle.getRigHandle() != mesh.getRigHandle())
            throwException("Mesh \"" + meshHandle.getMeshName() + "\" cannot be rebuilt on a different rig.");

        buildRiggedGeometry(mesh, null);

        VAOInstance vaoInstance = meshHandle.getVAOInstance();
        VBOHandle vboHandle = vboManager.updateVBOHandle(vaoInstance, meshHandle.getVBOHandle(), riggedVertices);
        IBOHandle iboHandle = updateRiggedIBO(meshHandle.getIBOHandle());
        Vector3 boundsMin = new Vector3();
        Vector3 boundsMax = new Vector3();

        computeRiggedBounds(boundsMin, boundsMax);
        meshHandle.constructor(
                meshHandle.getMeshName(),
                vaoInstance,
                vboHandle,
                iboHandle,
                meshHandle.getRigHandle(),
                boundsMin,
                boundsMax);
        applyRiggedMesh(meshHandle, mesh);
    }

    // Each palette texture's tile ID and size, in the scratch's reusable array
    private int[] resolveTextureTiles(RiggedMeshStruct mesh) {

        int intCount = mesh.getTextureCount() * EngineSetting.RIGGED_MESH_TILE_INTS;

        if (riggedScratch.textureTiles.length < intCount)
            riggedScratch.textureTiles = new int[intCount];

        int[] tiles = riggedScratch.textureTiles;

        for (int t = 0; t < mesh.getTextureCount(); t++) {

            TextureHandle textureHandle = textureManager.getTextureHandleFromTextureName(mesh.getTextureName(t));
            int base = t * EngineSetting.RIGGED_MESH_TILE_INTS;

            if (textureHandle.getTileID() >= EngineSetting.RIGGED_MESH_TILE_LIMIT)
                throwException("Texture \"" + mesh.getTextureName(t) + "\" has tile " + textureHandle.getTileID()
                        + " — a rigged vertex names its tile in an unsigned short.");

            tiles[base] = textureHandle.getTileID();
            tiles[base + 1] = textureHandle.getTileWidth();
        }

        return tiles;
    }

    // Removal \\

    public void removeMesh(MeshData meshData) {
        vaoManager.removeSourceVAOClones(meshData.getVAOData());
        vaoManager.removeVAOData(meshData.getVAOData());
        vboManager.removeVBO(meshData.getVBOData());
        iboManager.removeIBO(meshData.getIBOData());
    }

    public void removeMesh(MeshHandle meshHandle) {
        RiggedMeshGLSLUtility.deleteTexture(meshHandle.getMorphTexture());
        removeMesh(meshHandle.getMeshData());
    }

    public void removeMesh(MeshInstance meshInstance) {
        removeMesh(meshInstance.getMeshData());
    }
}