package com.internal.bootstrap.renderpipeline.cameramanager;

import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraInstance;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;

class CameraBufferSystem extends SystemPackage {

    /*
     * Pushes a window's perspective and orthographic camera data to the shared
     * GPU UBOs. Called by CameraManager.pushCamera() once per draw pass before
     * RenderSystem flushes. Takes the window directly — no WindowManager
     * dependency. All UBO knowledge is contained here within the camera pipeline.
     */

    // Internal
    private UBOManager uboManager;

    // UBOs
    private UBOHandle cameraDataUBO;
    private UBOHandle orthoDataUBO;

    // Internal \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.cameraDataUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.CAMERA_DATA_UBO);
        this.orthoDataUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.ORTHO_DATA_UBO);
    }

    // Push \\

    void pushForWindow(WindowInstance window) {
        pushPerspective(window);
        pushOrtho(window);
    }

    // Perspective \\

    private void pushPerspective(WindowInstance window) {

        CameraInstance camera = window.getActiveCamera();

        if (camera == null)
            return;

        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_PROJECTION, camera.getProjection());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_VIEW, camera.getView());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_INVERSE_PROJECTION, camera.getInverseProjection());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_INVERSE_VIEW, camera.getInverseView());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_VIEW_PROJECTION, camera.getViewProjection());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_POSITION, camera.getPosition());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_FOV, camera.getFOV());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_VIEWPORT, camera.getViewport());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_NEAR_PLANE, camera.getNearPlane());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_FAR_PLANE, camera.getFarPlane());

        uboManager.push(cameraDataUBO);
    }

    // Ortho \\

    private void pushOrtho(WindowInstance window) {

        OrthographicCameraInstance ortho = window.getOrthoCamera();

        if (ortho == null)
            return;

        orthoDataUBO.updateUniform(EngineSetting.UNIFORM_ORTHO_PROJECTION, ortho.getProjection());
        orthoDataUBO.updateUniform(EngineSetting.UNIFORM_ORTHO_SCREEN_SIZE, ortho.getScreenSize());

        uboManager.push(orthoDataUBO);
    }
}