package com.AdventureRPG.core.kernel;

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

    // Accessible \\

    protected final InstanceFrame create(InstanceFrame instanceFrame) {

        instanceFrame.create(
                gameEngine,
                owner);

        return instanceFrame;
    }
}
