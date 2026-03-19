package com.internal.core.engine;

import com.internal.bootstrap.renderpipeline.window.WindowInstance;

public interface WindowPlatform {

    /*
     * Platform abstraction for opening OS windows. Implemented in the lwjgl3
     * subproject where backend imports are available. Injected at launch via
     * Main and MainEditor constructors and held on EnginePackage for global
     * access via internal.windowPlatform. WindowManager is the only caller.
     */

    void openWindow(WindowInstance window);
}