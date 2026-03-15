package com.internal.bootstrap.renderpipeline.cameramanager;

import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.renderpipeline.camera.CameraData;
import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraData;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraInstance;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class CameraManager extends ManagerPackage {

    /*
     * Owns all camera instances and drives rotation updates each frame.
     * The main perspective camera and orthographic camera are created on awake
     * once the window size is known. Resize propagates to both cameras.
     */

    // Internal
    private InternalBufferSystem internalBufferSystem;
    private InputSystem inputSystem;

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
    protected void get() {

        // Internal
        this.inputSystem = get(InputSystem.class);
    }

    @Override
    protected void awake() {

        float w = internal.getWindowInstance().getWidth();
        float h = internal.getWindowInstance().getHeight();

        createCamera(internal.settings.FOV, w, h);
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

        CameraData data = new CameraData(fov, width, height);
        CameraInstance instance = create(CameraInstance.class);
        instance.constructor(data);
        cameraInstances.add(instance);

        if (mainCamera == null)
            mainCamera = instance;

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