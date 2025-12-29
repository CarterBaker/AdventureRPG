package com.AdventureRPG.core.engine;

public abstract class InstanceFrame extends EngineUtility {

    protected EnginePackage internal;
    protected SystemPackage owner;

    // Root \\

    public final void create(
            EnginePackage internal,
            SystemPackage owner) {

        this.internal = internal;
        this.owner = owner;
    }

    // Accessible \\

    protected final InstanceFrame create(InstanceFrame instanceFrame) {

        instanceFrame.create(
                internal,
                owner);

        return instanceFrame;
    }
}
