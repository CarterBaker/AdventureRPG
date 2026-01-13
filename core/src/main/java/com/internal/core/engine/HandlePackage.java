package com.internal.core.engine;

public class HandlePackage extends InstancePackage {

    /*
     * HandlePackages are intended to be held indefinitely during
     * game creation and may be duplicated during runtime when
     * required.
     *
     * Primarily used as persistent data containers for long-lived
     * references, including (but not limited to) GPU-side resources
     * and engine-managed handles.
     */
}
