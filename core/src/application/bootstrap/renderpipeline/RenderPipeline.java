package application.bootstrap.renderpipeline;

import application.bootstrap.renderpipeline.cameramanager.CameraManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.core.engine.PipelinePackage;

public class RenderPipeline extends PipelinePackage {

    /*
     * Registers all render pipeline managers in dependency order.
     * WindowManager first — CameraManager and RenderManager both depend on it.
     * CameraManager before RenderManager — RenderManager calls into CameraManager
     * each draw pass.
     */

    @Override
    protected void create() {
        create(CameraManager.class);
        create(RenderManager.class);
    }
}