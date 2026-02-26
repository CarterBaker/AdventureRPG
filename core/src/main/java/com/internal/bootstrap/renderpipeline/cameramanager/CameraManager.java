package com.internal.bootstrap.renderpipeline.cameramanager;

import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraInstance;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class CameraManager extends ManagerPackage {

    // Internal
    private InternalBufferSystem internalBufferSystem;
    private InputSystem inputSystem;

    // Perspective
    private final ObjectLinkedOpenHashSet<CameraInstance> cameraInstances = new ObjectLinkedOpenHashSet<>();
    private CameraInstance mainCamera;

    // Orthographic
    private OrthographicCameraInstance orthoCamera;

    // Viewport
    private int width;
    private int height;

    // Base \\

    @Override
    public void create() {
        this.internalBufferSystem = create(InternalBufferSystem.class);
        createCamera(settings.FOV, width, height);
    }

    @Override
    public void get() {
        this.inputSystem = get(InputSystem.class);
    }

    @Override
    protected void awake() {
        float w = internal.getWindowInstance().getWidth();
        float h = internal.getWindowInstance().getHeight();
        createOrthoCamera(w, h);
    }

    @Override
    protected void update() {
        if (mainCamera == null)
            return;
        mainCamera.setRotation(inputSystem.getRotation());
    }

    // Camera Management \\

    public CameraInstance createCamera(float fov, float width, float height) {
        CameraInstance createdCamera = create(CameraInstance.class);
        createdCamera.constructor(fov, width, height);
        cameraInstances.add(createdCamera);
        if (mainCamera == null)
            mainCamera = createdCamera;
        return createdCamera;
    }

    private void createOrthoCamera(float width, float height) {
        orthoCamera = create(OrthographicCameraInstance.class);
        orthoCamera.constructor(width, height);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        if (mainCamera != null)
            mainCamera.updateViewport(width, height);
        if (orthoCamera != null)
            orthoCamera.updateViewport(width, height);
    }

    // Accessible \\

    public CameraInstance getMainCamera() {
        return mainCamera;
    }

    public void setMainCamera(CameraInstance cam) {
        if (cam == null)
            return;
        mainCamera = cam;
    }

    public OrthographicCameraInstance getOrthoCamera() {
        return orthoCamera;
    }

    public ObjectLinkedOpenHashSet<CameraInstance> getCameraInstances() {
        return cameraInstances;
    }
}