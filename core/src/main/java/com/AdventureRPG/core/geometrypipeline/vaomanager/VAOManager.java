package com.AdventureRPG.core.geometrypipeline.vaomanager;

import java.io.File;

import com.AdventureRPG.core.kernel.ManagerFrame;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class VAOManager extends ManagerFrame {

    // Internal
    private InternalBuildSystem internalBuildSystem;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, VAOHandle> vaoName2VAOHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());

        // Retrieval Mapping
        this.vaoName2VAOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void freeMemory() {
        internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
    }

    // Utility \\

    public void addVAO(String resourceName, File file) {

    }

    public VAOHandle getVAOHandleFromName(String vaoName) {
        return vaoName2VAOHandle.get(vaoName);
    }
}
