package com.internal.bootstrap.geometrypipeline.vaomanager;

import java.io.File;

import com.internal.bootstrap.geometrypipeline.meshmanager.InternalLoadManager;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class VAOManager extends ManagerPackage {

    // Internal
    private InternalBuildSystem internalBuildSystem;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, VAOHandle> vaoName2VAOHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalBuildSystem = create(InternalBuildSystem.class);

        // Retrieval Mapping
        this.vaoName2VAOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void release() {
        internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Utility \\

    public void addVAO(String resourceName, File file, InternalLoadManager loadManager) {
        vaoName2VAOHandle.put(resourceName, internalBuildSystem.addVAO(file, loadManager));
    }

    public VAOHandle getVAOHandleFromName(String vaoName) {
        return vaoName2VAOHandle.get(vaoName);
    }

    public VAOHandle cloneVAO(VAOHandle templateVAO) {
        VAOHandle newHandle = create(VAOHandle.class);
        return GLSLUtility.cloneVAO(newHandle, templateVAO);
    }

    public void removeVAO(VAOHandle vaoHandle) {
        GLSLUtility.removeVAO(vaoHandle);
    }
}
