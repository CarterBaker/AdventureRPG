package com.AdventureRPG.Core.Bootstrap;

public abstract class InstanceFrame extends MainFrame {

    protected EngineFrame engineManager;
    protected SystemFrame owner;

    // Root \\

    public final void create(
            EngineFrame engineManager,
            SystemFrame owner) {

        this.engineManager = engineManager;
        this.owner = owner;
    }
}
