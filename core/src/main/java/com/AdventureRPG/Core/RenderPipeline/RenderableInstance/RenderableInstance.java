package com.AdventureRPG.Core.RenderPipeline.RenderableInstance;

import com.AdventureRPG.Core.Bootstrap.InstanceFrame;

public abstract class RenderableInstance extends InstanceFrame {

    protected final int handle;

    public RenderableInstance(int handle) {

        this.handle = handle;
    }
}
