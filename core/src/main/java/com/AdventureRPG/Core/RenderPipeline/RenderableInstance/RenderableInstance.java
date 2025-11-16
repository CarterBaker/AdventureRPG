package com.AdventureRPG.Core.RenderPipeline.RenderableInstance;

import com.AdventureRPG.Core.Bootstrap.InstanceFrame;

public abstract class RenderableInstance extends InstanceFrame {

    public final int handle;

    public RenderableInstance(int handle) {

        this.handle = handle;
    }
}
