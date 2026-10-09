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
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class VAOManager extends ManagerPackage {

    /*
     * Owns the VAO layout palette for the engine lifetime. Handles bootstrap
     * registration via VAOBuilder and drives VAOInstance creation and
     * deletion. Auto-triggers a mesh load on miss for external callers. The
     * per-window clone cache is keyed by source VAO first specifically so
     * removing every clone of one mesh's VAO — which happens on every chunk
     * render-bucket rebuild, not just on chunk unload — costs O(windows that
     * cloned it) instead of a full scan of every clone ever created across
     * the whole session.
     */

    // Internal
    private MeshManager meshManager;
    private WindowManager windowManager;

    // Palette
    private Object2IntOpenHashMap<String> vaoName2VAOID;
    private ObjectArrayList<VAOHandle> vaoID2VAOHandle;

    // Runtime Window Cache — sourceVAO -> (windowID -> clonedVAO)
    private Int2ObjectOpenHashMap<Int2IntOpenHashMap> sourceVAO2WindowClones;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.vaoName2VAOID = RegistryUtility.createNameIndex();
        this.vaoID2VAOHandle = RegistryUtility.createPalette();

        // Runtime Window Cache
        this.sourceVAO2WindowClones = new Int2ObjectOpenHashMap<>();
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

        int sourceVAO = meshData.getAttributeHandle();

        if (sourceVAO == 0)
            throwException("Mesh drawn without a source VAO. Upload the mesh before submitting it for render.");

        Int2IntOpenHashMap windowClones = sourceVAO2WindowClones.get(sourceVAO);

        if (windowClones == null) {
            windowClones = new Int2IntOpenHashMap();
            windowClones.defaultReturnValue(0);
            sourceVAO2WindowClones.put(sourceVAO, windowClones);
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

        ObjectIterator<Int2ObjectMap.Entry<Int2IntOpenHashMap>> sourceIterator = sourceVAO2WindowClones
                .int2ObjectEntrySet().fastIterator();

        while (sourceIterator.hasNext()) {

            Int2ObjectMap.Entry<Int2IntOpenHashMap> sourceEntry = sourceIterator.next();
            Int2IntOpenHashMap windowClones = sourceEntry.getValue();
            int clonedVAO = windowClones.remove(windowID);

            if (clonedVAO != 0)
                VAOGLSLUtility.removeVAOHandle(clonedVAO);

            if (windowClones.isEmpty())
                sourceIterator.remove();
        }
    }

    public void removeSourceVAOClones(int sourceVAO) {

        if (sourceVAO == 0)
            return;

        Int2IntOpenHashMap windowClones = sourceVAO2WindowClones.remove(sourceVAO);

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