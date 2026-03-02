package com.internal.bootstrap.geometrypipeline.ibomanager;

import java.io.File;

import com.internal.bootstrap.geometrypipeline.ibo.IBOInstance;
import com.internal.bootstrap.geometrypipeline.ibo.IBOStruct;
import com.internal.bootstrap.geometrypipeline.meshmanager.InternalLoadManager;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
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
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.iboName2IBOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void release() {
        internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Bootstrap \\

    public void addIBO(String resourceName, File file, InternalLoadManager loadManager, VAOInstance vaoInstance) {
        iboName2IBOHandle.put(resourceName, internalBuildSystem.addIBO(file, loadManager, vaoInstance));
    }

    public IBOHandle getIBOHandleFromName(String iboName) {
        return iboName2IBOHandle.get(iboName);
    }

    // Runtime \\

    public IBOInstance createIBOInstance(VAOInstance vaoInstance, ShortArrayList indices) {
        return GLSLUtility.uploadIndexData(vaoInstance, create(IBOInstance.class), indices.toShortArray());
    }

    // Removal \\

    public void removeIBO(IBOStruct iboStruct) {
        GLSLUtility.removeIndexData(iboStruct);
    }

    public void removeIBO(IBOHandle iboHandle) {
        GLSLUtility.removeIndexData(iboHandle.getIBOStruct());
    }

    public void removeIBOInstance(IBOInstance iboInstance) {
        GLSLUtility.removeIndexData(iboInstance.getIBOStruct());
    }
}