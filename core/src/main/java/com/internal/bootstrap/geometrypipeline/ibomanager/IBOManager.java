package com.internal.bootstrap.geometrypipeline.ibomanager;

import java.io.File;

import com.internal.bootstrap.geometrypipeline.meshmanager.InternalLoadManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class IBOManager extends ManagerPackage {

    // Internal
    private InternalBuildSystem internalBuildSystem;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, IBOHandle> iboName2IBOHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalBuildSystem = create(InternalBuildSystem.class);

        // Retrieval Mapping
        this.iboName2IBOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void release() {
        internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Utility \\

    public void addIBO(String resourceName, File file, InternalLoadManager loadManager) {
        iboName2IBOHandle.put(resourceName, internalBuildSystem.addIBO(file, loadManager));
    }

    public IBOHandle getIBOHandleFromName(String iboName) {
        return iboName2IBOHandle.get(iboName);
    }

    // Accessible \\

    public IBOHandle createIBO(VAOHandle vaoHandle, ShortArrayList indices) {
        IBOHandle iboHandle = create(IBOHandle.class);
        short[] indexArray = indices.toShortArray();
        return GLSLUtility.uploadIndexData(vaoHandle, iboHandle, indexArray);
    }

    public void removeIBO(IBOHandle iboHandle) {
        GLSLUtility.removeIndexData(iboHandle);
    }
}
