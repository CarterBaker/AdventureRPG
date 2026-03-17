package com.internal.bootstrap.renderpipeline.cameramanager;

import com.internal.bootstrap.renderpipeline.camera.CameraData;
import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraData;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraInstance;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class CameraManager extends ManagerPackage {

    /*
     * Pure camera factory and registry. Creates and tracks all camera instances
     * and owns the orthographic camera. Drives UBO pushes via InternalBufferSystem
     * for whichever camera is registered as main. Never drives position or rotation
     * —
     * whoever owns a camera is responsible for updating it each frame.
     * Main camera is null until an owner explicitly registers one via
     * setMainCamera().
     */

    // Internal
    private InternalBufferSystem internalBufferSystem;

    // Perspective
    private ObjectLinkedOpenHashSet<CameraInstance> cameraInstances;
    private CameraInstance mainCamera;

    // Orthographic
    private OrthographicCameraInstance orthoCamera;

    // Viewport
    private int width;
    private int height;

    // Internal \\

    @Override
    protected void create() {

        // Palette
        this.cameraInstances = new ObjectLinkedOpenHashSet<>();

        // Systems
        this.internalBufferSystem = create(InternalBufferSystem.class);
    }

    @Override
    protected void awake() {

        float w = internal.getWindowInstance().getWidth();
        float h = internal.getWindowInstance().getHeight();

        createOrthoCamera(w, h);
    }

    // Camera Management \\

    public CameraInstance createCamera(float fov, float width, float height) {

        CameraData data = new CameraData(fov, width, height);
        CameraInstance instance = create(CameraInstance.class);
        instance.constructor(data);
        cameraInstances.add(instance);

        return instance;
    }

    private void createOrthoCamera(float width, float height) {

        OrthographicCameraData data = new OrthographicCameraData(width, height);
        orthoCamera = create(OrthographicCameraInstance.class);
        orthoCamera.constructor(data);
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

    public void setMainCamera(CameraInstance camera) {

        if (camera == null)
            return;

        this.mainCamera = camera;
    }

    public OrthographicCameraInstance getOrthoCamera() {
        return orthoCamera;
    }

    public ObjectLinkedOpenHashSet<CameraInstance> getCameraInstances() {
        return cameraInstances;
    }
}