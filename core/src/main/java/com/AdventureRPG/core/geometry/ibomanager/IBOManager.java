package com.AdventureRPG.core.geometry.ibomanager;

import java.io.File;

import com.AdventureRPG.core.engine.ManagerFrame;
import com.AdventureRPG.core.geometry.modelmanager.InternalLoadManager;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class IBOManager extends ManagerFrame {

    // Internal
    private InternalBuildSystem internalBuildSystem;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, IBOHandle> iboName2IBOHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());

        // Retrieval Mapping
        this.iboName2IBOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void freeMemory() {
        internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
    }

    // Utility \\

    public void addIBO(String resourceName, File file, InternalLoadManager loadManager) {
        iboName2IBOHandle.put(resourceName, internalBuildSystem.addIBO(file, loadManager));
    }

    public IBOHandle getIBOHandleFromName(String iboName) {
        return iboName2IBOHandle.get(iboName);
    }
}
