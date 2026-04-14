package application.bootstrap.geometrypipeline.ibomanager;

import application.bootstrap.geometrypipeline.ibo.IBOData;
import application.bootstrap.geometrypipeline.ibo.IBOHandle;
import application.bootstrap.geometrypipeline.ibo.IBOInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class IBOManager extends ManagerPackage {

    /*
     * Owns the IBO palette for the engine lifetime. Handles bootstrap
     * registration via InternalBuilder, runtime IBOInstance creation, and
     * deletion. Auto-triggers a mesh load on miss for external callers.
     */

    // Internal
    private MeshManager meshManager;

    // Palette
    private Object2ObjectOpenHashMap<String, IBOHandle> iboName2IBOHandle;
    private Short2ObjectOpenHashMap<IBOHandle> iboID2IBOHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.iboName2IBOHandle = new Object2ObjectOpenHashMap<>();
        this.iboID2IBOHandle = new Short2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.meshManager = get(MeshManager.class);
    }

    // Management \\

    void registerIBO(String resourceName, IBOHandle handle) {

        short id = RegistryUtility.toShortID(resourceName);

        iboName2IBOHandle.put(resourceName, handle);
        iboID2IBOHandle.put(id, handle);
    }

    /*
     * Bypasses JSON parsing — used when index data was assembled by quad
     * expansion inside the mesh builder.
     */
    public IBOHandle addIBOFromData(
            String resourceName,
            short[] indices,
            VAOInstance vaoInstance) {

        IBOHandle handle = GLSLUtility.uploadIndexData(
                vaoInstance,
                create(IBOHandle.class),
                indices);

        registerIBO(resourceName, handle);

        return handle;
    }

    // Accessible \\

    public boolean hasIBO(String iboName) {
        return iboName2IBOHandle.containsKey(iboName);
    }

    public short getIBOIDFromIBOName(String iboName) {

        if (!iboName2IBOHandle.containsKey(iboName))
            meshManager.request(iboName);

        return RegistryUtility.toShortID(iboName);
    }

    public IBOHandle getIBOHandleFromIBOID(short iboID) {
        return iboID2IBOHandle.get(iboID);
    }

    public IBOHandle getIBOHandleFromIBOName(String iboName) {
        return getIBOHandleFromIBOID(getIBOIDFromIBOName(iboName));
    }

    /*
     * Direct registry lookup — no load trigger. Safe to call from inside any
     * builder that is already executing within a load() call.
     */
    public IBOHandle getIBOHandleDirect(String iboName) {
        return iboName2IBOHandle.get(iboName);
    }

    // Runtime \\

    public IBOInstance createIBOInstance(VAOInstance vaoInstance, ShortArrayList indices) {
        return GLSLUtility.uploadIndexData(
                vaoInstance,
                create(IBOInstance.class),
                indices.toShortArray());
    }

    // Removal \\

    public void removeIBO(IBOData iboData) {
        GLSLUtility.removeIndexData(iboData);
    }

    public void removeIBO(IBOHandle iboHandle) {
        GLSLUtility.removeIndexData(iboHandle.getIBOData());
    }

    public void removeIBOInstance(IBOInstance iboInstance) {
        GLSLUtility.removeIndexData(iboInstance.getIBOData());
    }
}