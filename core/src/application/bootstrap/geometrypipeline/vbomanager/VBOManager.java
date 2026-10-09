package application.bootstrap.geometrypipeline.vbomanager;

import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vbo.VBOData;
import application.bootstrap.geometrypipeline.vbo.VBOHandle;
import application.bootstrap.geometrypipeline.vbo.VBOInstance;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VBOManager extends ManagerPackage {

    /*
     * Owns the VBO palette for the engine lifetime. Handles bootstrap
     * registration via VBOBuilder, runtime VBOInstance creation, in-place
     * updates, and deletion. Auto-triggers a mesh load on miss for external
     * callers.
     */

    // Internal
    private MeshManager meshManager;

    // Palette
    private Object2IntOpenHashMap<String> vboName2VBOID;
    private ObjectArrayList<VBOHandle> vboID2VBOHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.vboName2VBOID = RegistryUtility.createNameIndex();
        this.vboID2VBOHandle = RegistryUtility.createPalette();
    }

    @Override
    protected void get() {

        // Internal
        this.meshManager = get(MeshManager.class);
    }

    // Management \\

    void registerVBO(String resourceName, VBOHandle handle) {
        RegistryUtility.registerHandle(
                vboName2VBOID, vboID2VBOHandle, resourceName, handle, EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    public VBOHandle addVBOFromData(
            String resourceName,
            float[] vertices,
            VAOInstance vaoInstance) {

        VBOHandle handle = VBOGLSLUtility.uploadVertexData(
                vaoInstance,
                create(VBOHandle.class),
                vertices);

        registerVBO(resourceName, handle);

        return handle;
    }

    // Accessible \\

    public boolean hasVBO(String vboName) {
        return getVBOHandleDirect(vboName) != null;
    }

    public short getVBOIDFromVBOName(String vboName) {

        if (!hasVBO(vboName))
            meshManager.request(vboName);

        return (short) vboName2VBOID.getInt(vboName);
    }

    public VBOHandle getVBOHandleFromVBOID(short vboID) {
        return RegistryUtility.getHandle(vboID2VBOHandle, vboID);
    }

    public VBOHandle getVBOHandleFromVBOName(String vboName) {
        return getVBOHandleFromVBOID(getVBOIDFromVBOName(vboName));
    }

    public VBOHandle getVBOHandleDirect(String vboName) {
        return RegistryUtility.getHandle(vboName2VBOID, vboID2VBOHandle, vboName);
    }

    // Runtime \\

    public VBOInstance createVBOInstance(VAOInstance vaoInstance, FloatArrayList vertices) {
        return VBOGLSLUtility.uploadVertexData(
                vaoInstance,
                create(VBOInstance.class),
                vertices.elements(),
                vertices.size());
    }

    public VBOInstance updateVBOInstance(VAOInstance vaoInstance, VBOInstance vboInstance, FloatArrayList vertices) {
        return VBOGLSLUtility.updateVertexData(vaoInstance, vboInstance, vertices.elements(), vertices.size());
    }

    // Removal \\

    public void removeVBO(VBOData vboData) {
        VBOGLSLUtility.removeVertexData(vboData);
    }

    public void removeVBO(VBOHandle vboHandle) {
        VBOGLSLUtility.removeVertexData(vboHandle.getVBOData());
    }

    public void removeVBOInstance(VBOInstance vboInstance) {
        VBOGLSLUtility.removeVertexData(vboInstance.getVBOData());
    }
}