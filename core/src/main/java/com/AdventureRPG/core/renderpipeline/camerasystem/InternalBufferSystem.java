package com.AdventureRPG.core.renderpipeline.camerasystem;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.shaderpipeline.ubomanager.UBOHandle;
import com.AdventureRPG.core.shaderpipeline.ubomanager.UBOManager;

class InternalBufferSystem extends SystemPackage {

    private UBOManager uboManager;
    private CameraManager cameraManager;
    private UBOHandle cameraDataUBO;

    @Override
    protected void init() {
        this.uboManager = this.internal.get(UBOManager.class);
        this.cameraManager = this.internal.get(CameraManager.class);
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
        this.cameraDataUBO.update("u_projection", mainCam.getProjection());
        this.cameraDataUBO.update("u_view", mainCam.getView());
        this.cameraDataUBO.update("u_inverseProjection", mainCam.getInverseProjection());
        this.cameraDataUBO.update("u_inverseView", mainCam.getInverseView());
        this.cameraDataUBO.update("u_viewProjection", mainCam.getViewProjection());
        this.cameraDataUBO.update("u_cameraPosition", mainCam.getPosition());
        this.cameraDataUBO.update("u_cameraFOV", mainCam.getFOV());
        this.cameraDataUBO.update("u_viewport", mainCam.getViewport());
        this.cameraDataUBO.update("u_nearPlane", mainCam.getNearPlane());
        this.cameraDataUBO.update("u_farPlane", mainCam.getFarPlane());
    }
}
