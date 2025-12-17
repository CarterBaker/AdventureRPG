package com.AdventureRPG.core.renderpipeline.camerasystem;

import com.AdventureRPG.core.kernel.SystemFrame;
import com.AdventureRPG.core.shaderpipeline.UBOManager.UBOData;
import com.AdventureRPG.core.shaderpipeline.UBOManager.UBOHandle;
import com.AdventureRPG.core.shaderpipeline.UBOManager.UBOManager;
import com.AdventureRPG.core.shaderpipeline.uniforms.UniformType;

class InternalBufferSystem extends SystemFrame {

    private UBOManager uboManager;
    private CameraManager cameraManager;
    private UBOHandle cameraDataUBO;

    @Override
    protected void create() {
        // Will be initialized in init() after dependencies are available
    }

    @Override
    protected void init() {
        this.uboManager = this.gameEngine.get(UBOManager.class);
        this.cameraManager = this.gameEngine.get(CameraManager.class);
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
