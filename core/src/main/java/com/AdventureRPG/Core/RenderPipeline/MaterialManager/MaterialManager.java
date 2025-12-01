package com.AdventureRPG.Core.RenderPipeline.MaterialManager;

import com.AdventureRPG.Core.Bootstrap.ManagerFrame;

public class MaterialManager extends ManagerFrame {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = (InternalLoadManager) register(new InternalLoadManager());
    }

    @Override
    protected void awake() {
        compileMaterials();
    }

    @Override
    protected void freeMemory() {
        internalLoadManager = (InternalLoadManager) release(internalLoadManager);
    }

    // Material Management \\

    private void compileMaterials() {
        internalLoadManager.compileMaterials();
    }
}
