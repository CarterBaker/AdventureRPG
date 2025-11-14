package com.AdventureRPG.Core.Bootstrap;

public abstract class InstanceFrame extends MainFrame {

    protected EngineFrame engineManager;
    protected ManagerFrame localManager;

    // Root \\

    public final void registerLocalManager(
            EngineFrame engineManager,
            ManagerFrame localManager) {

        this.engineManager = engineManager;
        this.localManager = localManager;
    }
}
