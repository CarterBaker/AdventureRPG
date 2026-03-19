package com.internal.core.engine;

import com.internal.bootstrap.renderpipeline.window.WindowInstance;

public abstract class ContextPackage extends ManagerPackage {

    /*
     * Base class for all render contexts. A context is owned by a window and
     * tells the render system what surface to draw to. Every window — main or
     * detached — holds a ContextPackage that defines its render target and the
     * systems responsible for populating it each frame.
     *
     * Systems created within a context automatically receive a reference back
     * to it via SystemPackage.context. They call context.getWindow() without
     * knowing which window they are targeting. The editor reuses RuntimeContext
     * unchanged — it just calls setWindow() with its preview panel window before
     * the context starts, and every system inside behaves identically.
     */

    // Window
    private WindowInstance window;

    // Accessible \\

    public WindowInstance getWindow() {
        return window;
    }

    public void setWindow(WindowInstance window) {
        this.window = window;
    }

    public boolean hasWindow() {
        return window != null;
    }
}