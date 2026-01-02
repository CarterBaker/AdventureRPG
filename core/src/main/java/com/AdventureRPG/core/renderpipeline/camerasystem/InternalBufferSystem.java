package com.AdventureRPG.core.renderpipeline.camerasystem;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.shaderpipeline.ubomanager.UBOHandle;
import com.AdventureRPG.core.shaderpipeline.ubomanager.UBOManager;

class InternalBufferSystem extends SystemPackage {

    private UBOManager uboManager;
    private CameraManager cameraManager;
    private UBOHandle cameraDataUBO;

    @Override
    protected void get() {
        this.uboManager = this.get(UBOManager.class);
        this.cameraManager = this.get(CameraManager.class);
    }

    @Override
    protected void awake() {
        this.cameraDataUBO = this.uboManager.getUBOHandleFromUBOName("CameraData");
    }

    @Override
    protected void update() {

        CameraInstance mainCam = this.cameraManager.mainCamera();

        if (mainCam == null)
            return;

        // Update all camera uniforms
        this.cameraDataUBO.updateUniform("u_projection", mainCam.getProjection());
        this.cameraDataUBO.updateUniform("u_view", mainCam.getView());
        this.cameraDataUBO.updateUniform("u_inverseProjection", mainCam.getInverseProjection());
        this.cameraDataUBO.updateUniform("u_inverseView", mainCam.getInverseView());
        this.cameraDataUBO.updateUniform("u_viewProjection", mainCam.getViewProjection());
        this.cameraDataUBO.updateUniform("u_cameraPosition", mainCam.getPosition());
        this.cameraDataUBO.updateUniform("u_cameraFOV", mainCam.getFOV());
        this.cameraDataUBO.updateUniform("u_viewport", mainCam.getViewport());
        this.cameraDataUBO.updateUniform("u_nearPlane", mainCam.getNearPlane());
        this.cameraDataUBO.updateUniform("u_farPlane", mainCam.getFarPlane());

        this.cameraDataUBO.push();
    }
}
