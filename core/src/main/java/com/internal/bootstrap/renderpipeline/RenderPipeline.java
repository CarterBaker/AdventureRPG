package com.internal.bootstrap.renderpipeline;

import com.internal.bootstrap.renderpipeline.cameramanager.CameraManager;
import com.internal.bootstrap.renderpipeline.rendermanager.RenderManager;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.core.engine.PipelinePackage;

public class RenderPipeline extends PipelinePackage {

    /*
     * Registers all render pipeline managers in dependency order.
     * WindowManager first — CameraManager and RenderManager both depend on it.
     * CameraManager before RenderManager — RenderManager calls into CameraManager
     * each draw pass.
     */

    @Override
    protected void create() {
        create(WindowManager.class);
        create(CameraManager.class);
        create(RenderManager.class);
    }
}