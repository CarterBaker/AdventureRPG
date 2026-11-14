package application.bootstrap.geometrypipeline.vaomanager;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.vao.VAOData;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

public class VAOManager extends ManagerPackage {

    /*
     * Owns the VAO layout palette for the engine lifetime. Handles bootstrap
     * registration via VAOBuilder and drives VAOInstance creation and
     * deletion. Auto-triggers a mesh load on miss for external callers. The
     * per-window clone cache is keyed by source VAO first specifically so
     * removing every clone of one mesh's VAO — which happens on every chunk
     * render-bucket rebuild, not just on chunk unload — costs O(windows that
     * cloned it) instead of a full scan of every clone ever created across
     * the whole session. The source is keyed by its VAOData rather than its
     * GL name, since a name is only unique within the context that made it
     * and two meshes must never share a clone. Every context switch made to
     * delete clones ends back on the main window's context.
     */

    // Internal
    private MeshManager meshManager;
    private WindowManager windowManager;

    // Palette
    private Object2IntOpenHashMap<String> vaoName2VAOID;
    private ObjectArrayList<VAOHandle> vaoID2VAOHandle;

    // Runtime Window Cache — source VAO data -> (windowID -> clonedVAO)
    private Reference2ObjectOpenHashMap<VAOData, Int2IntOpenHashMap> vaoData2WindowClones;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.vaoName2VAOID = RegistryUtility.createNameIndex();
        this.vaoID2VAOHandle = RegistryUtility.createPalette();

        // Runtime Window Cache
        this.vaoData2WindowClones = new Reference2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.meshManager = get(MeshManager.class);
        this.windowManager = get(WindowManager.class);
    }

    // Management \\

    void registerVAO(String resourceName, VAOHandle handle) {
        RegistryUtility.registerHandle(
                vaoName2VAOID, vaoID2VAOHandle, resourceName, handle, EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    // Accessible \\

    public boolean hasVAO(String vaoName) {
        return getVAOHandleDirect(vaoName) != null;
    }

    public short getVAOIDFromVAOName(String vaoName) {

        if (!hasVAO(vaoName))
            meshManager.request(vaoName);

        return (short) vaoName2VAOID.getInt(vaoName);
    }

    public VAOHandle getVAOHandleFromVAOID(short vaoID) {
        return RegistryUtility.getHandle(vaoID2VAOHandle, vaoID);
    }

    public VAOHandle getVAOHandleFromVAOName(String vaoName) {
        return getVAOHandleFromVAOID(getVAOIDFromVAOName(vaoName));
    }

    public VAOHandle getVAOHandleDirect(String vaoName) {
        return RegistryUtility.getHandle(vaoName2VAOID, vaoID2VAOHandle, vaoName);
    }

    // Instance Management \\

    public VAOInstance createVAOInstance(VAOHandle template) {
        return VAOGLSLUtility.createVAOInstance(create(VAOInstance.class), template);
    }

    public int getVAOForWindow(MeshData meshData, int windowID) {

        VAOData sourceVAOData = meshData.getVAOData();

        if (sourceVAOData.getAttributeHandle() == 0)
            throwException("Mesh drawn without a source VAO. Upload the mesh before submitting it for render.");

        Int2IntOpenHashMap windowClones = vaoData2WindowClones.get(sourceVAOData);

        if (windowClones == null) {
            windowClones = new Int2IntOpenHashMap();
            windowClones.defaultReturnValue(0);
            vaoData2WindowClones.put(sourceVAOData, windowClones);
        }

        int cachedVAO = windowClones.get(windowID);

        if (cachedVAO != 0)
            return cachedVAO;

        int clonedVAO = VAOGLSLUtility.cloneVAO(
                meshData.getVAOData().getAttrSizes(),
                meshData.getVertexHandle(),
                meshData.getIndexHandle());

        windowClones.put(windowID, clonedVAO);

        return clonedVAO;
    }

    public void removeWindowVAOs(int windowID) {

        if (windowID == 0)
            return;

        WindowInstance window = getWindowByID(windowID);

        if (window == null || !window.hasNativeHandle())
            return;

        internal.windowPlatform.makeContextCurrent(window);

        ObjectIterator<Reference2ObjectMap.Entry<VAOData, Int2IntOpenHashMap>> sourceIterator = vaoData2WindowClones
                .reference2ObjectEntrySet().fastIterator();

        while (sourceIterator.hasNext()) {

            Reference2ObjectMap.Entry<VAOData, Int2IntOpenHashMap> sourceEntry = sourceIterator.next();
            Int2IntOpenHashMap windowClones = sourceEntry.getValue();
            int clonedVAO = windowClones.remove(windowID);

            if (clonedVAO != 0)
                VAOGLSLUtility.removeVAOHandle(clonedVAO);

            if (windowClones.isEmpty())
                sourceIterator.remove();
        }

        restoreMainContext();
    }

    public void removeSourceVAOClones(VAOData sourceVAOData) {

        Int2IntOpenHashMap windowClones = vaoData2WindowClones.remove(sourceVAOData);

        if (windowClones == null)
            return;

        int currentWindowID = Integer.MIN_VALUE;

        for (Int2IntMap.Entry entry : windowClones.int2IntEntrySet()) {

            int windowID = entry.getIntKey();
            int clonedVAO = entry.getIntValue();

            if (windowID != currentWindowID) {

                WindowInstance window = getWindowByID(windowID);

                if (window == null || !window.hasNativeHandle())
                    continue;

                internal.windowPlatform.makeContextCurrent(window);
                currentWindowID = windowID;
            }

            VAOGLSLUtility.removeVAOHandle(clonedVAO);
        }

        restoreMainContext();
    }

    private void restoreMainContext() {

        WindowInstance mainWindow = windowManager.getMainWindow();

        if (mainWindow != null && mainWindow.hasNativeHandle())
            internal.windowPlatform.makeContextCurrent(mainWindow);
    }

    public void removeVAOData(VAOData vaoData) {
        VAOGLSLUtility.removeVAOData(vaoData);
    }

    public void removeVAOInstance(VAOInstance vaoInstance) {
        VAOGLSLUtility.removeVAOInstance(vaoInstance);
    }

    private WindowInstance getWindowByID(int windowID) {

        if (windowID == 0)
            return windowManager.getMainWindow();

        for (int i = 0; i < windowManager.getWindows().size(); i++) {
            WindowInstance window = windowManager.getWindows().get(i);

            if (window.getWindowID() == windowID)
                return window;
        }

        return null;
    }
}