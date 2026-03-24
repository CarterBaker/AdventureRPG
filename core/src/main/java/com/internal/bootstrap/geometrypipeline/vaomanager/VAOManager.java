package com.internal.bootstrap.geometrypipeline.vaomanager;

import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.vao.VAOData;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class VAOManager extends ManagerPackage {

    /*
     * Owns the VAO layout palette for the engine lifetime. Handles bootstrap
     * registration via InternalBuilder and drives VAOInstance creation and
     * deletion. Auto-triggers a mesh load on miss for external callers.
     *
     * VAO instances are window-specific — each (window, template) pair gets
     * its own GPU VAO object. Instances are created lazily on first render
     * and cached for subsequent frames. Window cleanup removes all instances
     * for that window.
     */

    // Internal
    private MeshManager meshManager;

    // Palette
    private Object2ObjectOpenHashMap<String, VAOHandle> vaoName2VAOHandle;
    private Short2ObjectOpenHashMap<VAOHandle> vaoID2VAOHandle;

    // Window Tracking
    private Int2ObjectOpenHashMap<Short2ObjectOpenHashMap<VAOInstance>> window2Template2VAOInstance;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.vaoName2VAOHandle = new Object2ObjectOpenHashMap<>();
        this.vaoID2VAOHandle = new Short2ObjectOpenHashMap<>();

        // Window Tracking
        this.window2Template2VAOInstance = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.meshManager = get(MeshManager.class);
    }

    // Management \\

    void registerVAO(String resourceName, VAOHandle handle) {

        short id = RegistryUtility.toShortID(resourceName);
        handle.setTemplateID(id);
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

    /*
     * Retrieves or creates a VAOInstance for the given template and window.
     * Each window gets its own VAO object for each template — VAOs are GL
     * context-specific and cannot be shared across windows. Instances are
     * created lazily on first request and cached for subsequent frames.
     */
    public VAOInstance getOrCreateVAOInstance(VAOHandle template, WindowInstance window) {

        int windowID = window.getWindowID();
        short templateID = template.getTemplateID();

        Short2ObjectOpenHashMap<VAOInstance> template2Instance = window2Template2VAOInstance.get(windowID);

        if (template2Instance == null) {
            template2Instance = new Short2ObjectOpenHashMap<>();
            window2Template2VAOInstance.put(windowID, template2Instance);
        }

        VAOInstance instance = template2Instance.get(templateID);

        if (instance == null) {
            instance = GLSLUtility.createVAOInstance(create(VAOInstance.class), template);
            template2Instance.put(templateID, instance);
        }

        return instance;
    }

    public void removeVAOData(VAOData vaoData) {
        GLSLUtility.removeVAOData(vaoData);
    }

    public void removeVAOInstance(VAOInstance vaoInstance) {
        GLSLUtility.removeVAOInstance(vaoInstance);
    }

    /*
     * Removes all VAO instances for a specific window. Called by
     * WindowInstance.dispose() when a window closes. Frees all GPU VAO
     * objects created for that window's GL context.
     */
    public void removeWindowVAOs(int windowID) {

        Short2ObjectOpenHashMap<VAOInstance> template2Instance = window2Template2VAOInstance.remove(windowID);

        if (template2Instance == null)
            return;

        Object[] instances = template2Instance.values().toArray();

        for (int i = 0; i < instances.length; i++) {
            VAOInstance instance = (VAOInstance) instances[i];
            GLSLUtility.removeVAOInstance(instance);
        }
    }
}