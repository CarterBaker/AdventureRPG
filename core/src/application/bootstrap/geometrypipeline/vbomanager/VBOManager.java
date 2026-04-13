package application.bootstrap.geometrypipeline.vbomanager;

import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vbo.VBOData;
import application.bootstrap.geometrypipeline.vbo.VBOHandle;
import application.bootstrap.geometrypipeline.vbo.VBOInstance;
import engine.root.ManagerPackage;
import engine.util.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class VBOManager extends ManagerPackage {

    /*
     * Owns the VBO palette for the engine lifetime. Handles bootstrap
     * registration via InternalBuilder, runtime VBOInstance creation, and
     * deletion. Auto-triggers a mesh load on miss for external callers.
     */

    // Internal
    private MeshManager meshManager;

    // Palette
    private Object2ObjectOpenHashMap<String, VBOHandle> vboName2VBOHandle;
    private Short2ObjectOpenHashMap<VBOHandle> vboID2VBOHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.vboName2VBOHandle = new Object2ObjectOpenHashMap<>();
        this.vboID2VBOHandle = new Short2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.meshManager = get(MeshManager.class);
    }

    // Management \\

    void registerVBO(String resourceName, VBOHandle handle) {

        short id = RegistryUtility.toShortID(resourceName);

        vboName2VBOHandle.put(resourceName, handle);
        vboID2VBOHandle.put(id, handle);
    }

    /*
     * Bypasses JSON parsing — used when vertex data was assembled by quad
     * expansion inside the mesh builder.
     */
    public VBOHandle addVBOFromData(
            String resourceName,
            float[] vertices,
            VAOInstance vaoInstance) {

        VBOHandle handle = GLSLUtility.uploadVertexData(
                vaoInstance,
                create(VBOHandle.class),
                vertices);

        registerVBO(resourceName, handle);

        return handle;
    }

    // Accessible \\

    public boolean hasVBO(String vboName) {
        return vboName2VBOHandle.containsKey(vboName);
    }

    public short getVBOIDFromVBOName(String vboName) {

        if (!vboName2VBOHandle.containsKey(vboName))
            meshManager.request(vboName);

        return RegistryUtility.toShortID(vboName);
    }

    public VBOHandle getVBOHandleFromVBOID(short vboID) {
        return vboID2VBOHandle.get(vboID);
    }

    public VBOHandle getVBOHandleFromVBOName(String vboName) {
        return getVBOHandleFromVBOID(getVBOIDFromVBOName(vboName));
    }

    /*
     * Direct registry lookup — no load trigger. Safe to call from inside any
     * builder that is already executing within a load() call.
     */
    public VBOHandle getVBOHandleDirect(String vboName) {
        return vboName2VBOHandle.get(vboName);
    }

    // Runtime \\

    public VBOInstance createVBOInstance(VAOInstance vaoInstance, FloatArrayList vertices) {
        return GLSLUtility.uploadVertexData(
                vaoInstance,
                create(VBOInstance.class),
                vertices.toFloatArray());
    }

    // Removal \\

    public void removeVBO(VBOData vboData) {
        GLSLUtility.removeVertexData(vboData);
    }

    public void removeVBO(VBOHandle vboHandle) {
        GLSLUtility.removeVertexData(vboHandle.getVBOData());
    }

    public void removeVBOInstance(VBOInstance vboInstance) {
        GLSLUtility.removeVertexData(vboInstance.getVBOData());
    }
}