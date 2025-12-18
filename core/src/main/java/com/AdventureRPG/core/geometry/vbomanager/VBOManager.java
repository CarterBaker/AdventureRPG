package com.AdventureRPG.core.geometry.vbomanager;

import java.io.File;

import com.AdventureRPG.core.engine.ManagerFrame;
import com.AdventureRPG.core.geometry.modelmanager.InternalLoadManager;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class VBOManager extends ManagerFrame {

    // Internal
    private InternalBuildSystem internalBuildSystem;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, VBOHandle> vboName2VBOHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());

        // Retrieval Mapping
        this.vboName2VBOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void freeMemory() {
        internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
    }

    // Utility \\

    public void addVBO(String resourceName, File file, InternalLoadManager loadManager) {
        vboName2VBOHandle.put(resourceName, internalBuildSystem.addVBO(file, loadManager));
    }

    public VBOHandle getVBOHandleFromName(String vboName) {
        return vboName2VBOHandle.get(vboName);
    }
}
