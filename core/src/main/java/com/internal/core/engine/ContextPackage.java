package com.internal.core.engine;

public class ContextPackage extends ManagerPackage {

    /*
     * Base class for all render contexts. A context is owned by a window and
     * tells the render system what surface to draw to. Every window — main or
     * detached — holds a ContextPackage that defines its render target and the
     * systems responsible for populating it each frame.
     */
}