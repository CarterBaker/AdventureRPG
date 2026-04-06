package program.bootstrap.geometrypipeline.vaomanager;

import program.bootstrap.geometrypipeline.meshmanager.MeshManager;
import program.bootstrap.geometrypipeline.mesh.MeshData;
import program.bootstrap.geometrypipeline.vao.VAOData;
import program.bootstrap.geometrypipeline.vao.VAOHandle;
import program.bootstrap.geometrypipeline.vao.VAOInstance;
import program.core.engine.ManagerPackage;
import program.core.kernel.window.WindowInstance;
import program.core.kernel.windowmanager.WindowManager;
import program.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class VAOManager extends ManagerPackage {

    /*
     * Owns the VAO layout palette for the engine lifetime. Handles bootstrap
     * registration via InternalBuilder and drives VAOInstance creation and
     * deletion. Auto-triggers a mesh load on miss for external callers.
     */

    // Internal
    private MeshManager meshManager;
    private WindowManager windowManager;

    // Palette
    private Object2ObjectOpenHashMap<String, VAOHandle> vaoName2VAOHandle;
    private Short2ObjectOpenHashMap<VAOHandle> vaoID2VAOHandle;

    // Runtime Window Cache
    private Long2IntOpenHashMap sourceWindowKey2ClonedVAO;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.vaoName2VAOHandle = new Object2ObjectOpenHashMap<>();
        this.vaoID2VAOHandle = new Short2ObjectOpenHashMap<>();

        // Runtime Window Cache
        this.sourceWindowKey2ClonedVAO = new Long2IntOpenHashMap();
        this.sourceWindowKey2ClonedVAO.defaultReturnValue(0);
    }

    @Override
    protected void get() {

        // Internal
        this.meshManager = get(MeshManager.class);
        this.windowManager = get(WindowManager.class);
    }

    // Management \\

    void registerVAO(String resourceName, VAOHandle handle) {

        short id = RegistryUtility.toShortID(resourceName);

        vaoName2VAOHandle.put(resourceName, handle);
        vaoID2VAOHandle.put(id, handle);
    }

    // Accessible \\

    public boolean hasVAO(String vaoName) {
        return vaoName2VAOHandle.containsKey(vaoName);
    }

    public short getVAOIDFromVAOName(String vaoName) {

        if (!vaoName2VAOHandle.containsKey(vaoName))
            meshManager.request(vaoName);

        return RegistryUtility.toShortID(vaoName);
    }

    public VAOHandle getVAOHandleFromVAOID(short vaoID) {
        return vaoID2VAOHandle.get(vaoID);
    }

    public VAOHandle getVAOHandleFromVAOName(String vaoName) {
        return getVAOHandleFromVAOID(getVAOIDFromVAOName(vaoName));
    }

    /*
     * Direct registry lookup — no load trigger. Safe to call from inside any
     * builder that is already executing within a load() call.
     */
    public VAOHandle getVAOHandleDirect(String vaoName) {
        return vaoName2VAOHandle.get(vaoName);
    }

    // Instance Management \\

    public VAOInstance createVAOInstance(VAOHandle template) {
        return GLSLUtility.createVAOInstance(create(VAOInstance.class), template);
    }

    public int getVAOForWindow(MeshData meshData, int windowID) {
        int sourceVAO = meshData.getAttributeHandle();

        /*
         * Window 0 is the main engine window. In the normal bootstrap path its
         * meshes/VAOs are created while the main context is current, so we can use
         * the source VAO handle directly with no clone cost.
         *
         * Detached windows run their own contexts and therefore need a per-window
         * VAO clone that rebinds the same VBO/IBO layout in that context.
         */
        if (windowID == 0 && sourceVAO != 0)
            return sourceVAO;

        long key = composeWindowKey(sourceVAO, windowID);
        int cachedVAO = sourceWindowKey2ClonedVAO.get(key);

        if (cachedVAO != 0)
            return cachedVAO;

        int clonedVAO = GLSLUtility.cloneVAO(
                meshData.getVAOData().getAttrSizes(),
                meshData.getVertexHandle(),
                meshData.getIndexHandle());

        sourceWindowKey2ClonedVAO.put(key, clonedVAO);

        return clonedVAO;
    }

    public void removeWindowVAOs(int windowID) {

        if (windowID == 0)
            return;

        WindowInstance window = getWindowByID(windowID);

        if (window == null || !window.hasNativeHandle())
            return;

        internal.windowPlatform.makeContextCurrent(window);

        ObjectIterator<Long2IntMap.Entry> iterator = sourceWindowKey2ClonedVAO.long2IntEntrySet().fastIterator();

        while (iterator.hasNext()) {

            Long2IntMap.Entry entry = iterator.next();

            if (extractWindowID(entry.getLongKey()) != windowID)
                continue;

            GLSLUtility.removeVAOHandle(entry.getIntValue());
            iterator.remove();
        }
    }

    public void removeSourceVAOClones(int sourceVAO) {

        if (sourceVAO == 0)
            return;

        int currentWindowID = Integer.MIN_VALUE;
        ObjectIterator<Long2IntMap.Entry> iterator = sourceWindowKey2ClonedVAO.long2IntEntrySet().fastIterator();

        while (iterator.hasNext()) {

            Long2IntMap.Entry entry = iterator.next();

            if (extractSourceVAO(entry.getLongKey()) != sourceVAO)
                continue;

            int windowID = extractWindowID(entry.getLongKey());

            if (windowID != currentWindowID) {
                WindowInstance window = getWindowByID(windowID);

                if (window == null || !window.hasNativeHandle()) {
                    iterator.remove();
                    continue;
                }

                internal.windowPlatform.makeContextCurrent(window);
                currentWindowID = windowID;
            }

            GLSLUtility.removeVAOHandle(entry.getIntValue());
            iterator.remove();
        }

        WindowInstance mainWindow = windowManager.getMainWindow();

        if (mainWindow != null && mainWindow.hasNativeHandle())
            internal.windowPlatform.makeContextCurrent(mainWindow);
    }

    public void removeVAOData(VAOData vaoData) {
        GLSLUtility.removeVAOData(vaoData);
    }

    public void removeVAOInstance(VAOInstance vaoInstance) {
        GLSLUtility.removeVAOInstance(vaoInstance);
    }

    private long composeWindowKey(int sourceVAO, int windowID) {
        return ((long) windowID << 32) | (sourceVAO & 0xFFFFFFFFL);
    }

    private int extractWindowID(long key) {
        return (int) (key >>> 32);
    }

    private int extractSourceVAO(long key) {
        return (int) (key & 0xFFFFFFFFL);
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