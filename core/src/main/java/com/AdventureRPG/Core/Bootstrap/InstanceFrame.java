package com.AdventureRPG.Core.Bootstrap;

public abstract class InstanceFrame extends MainFrame {

    protected EngineFrame gameEngine;
    protected SystemFrame owner;

    // Root \\

    public final void create(
            EngineFrame gameEngine,
            SystemFrame owner) {

        this.gameEngine = gameEngine;
        this.owner = owner;
    }
}
