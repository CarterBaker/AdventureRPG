package com.internal.bootstrap.renderpipeline.cameramanager;

import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraInstance;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.SystemPackage;

class InternalBufferSystem extends SystemPackage {

    private UBOManager uboManager;
    private CameraManager cameraManager;

    private UBOHandle cameraDataUBO;
    private UBOHandle orthoDataUBO;

    // Base \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.cameraManager = get(CameraManager.class);
    }

    @Override
    protected void awake() {
        this.cameraDataUBO = uboManager.getUBOHandleFromUBOName("CameraData");
        this.orthoDataUBO = uboManager.getUBOHandleFromUBOName("OrthoData");
    }

    @Override
    protected void update() {
        updatePerspective();
        updateOrtho();
    }

    // Perspective \\

    private void updatePerspective() {

        CameraInstance mainCam = cameraManager.getMainCamera();

        if (mainCam == null)
            return;

        cameraDataUBO.updateUniform("u_projection", mainCam.getProjection());
        cameraDataUBO.updateUniform("u_view", mainCam.getView());
        cameraDataUBO.updateUniform("u_inverseProjection", mainCam.getInverseProjection());
        cameraDataUBO.updateUniform("u_inverseView", mainCam.getInverseView());
        cameraDataUBO.updateUniform("u_viewProjection", mainCam.getViewProjection());
        cameraDataUBO.updateUniform("u_cameraPosition", mainCam.getPosition());
        cameraDataUBO.updateUniform("u_cameraFOV", mainCam.getFOV());
        cameraDataUBO.updateUniform("u_viewport", mainCam.getViewport());
        cameraDataUBO.updateUniform("u_nearPlane", mainCam.getNearPlane());
        cameraDataUBO.updateUniform("u_farPlane", mainCam.getFarPlane());
        cameraDataUBO.push();
    }

    // Ortho \\

    private void updateOrtho() {

        OrthographicCameraInstance orthoCam = cameraManager.getOrthoCamera();

        if (orthoCam == null)
            return;

        orthoDataUBO.updateUniform("u_orthoProjection", orthoCam.getProjection());
        orthoDataUBO.updateUniform("u_screenSize", orthoCam.getScreenSize());
        orthoDataUBO.push();
    }
}