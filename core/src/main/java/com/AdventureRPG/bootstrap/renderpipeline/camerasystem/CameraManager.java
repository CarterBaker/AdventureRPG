package com.AdventureRPG.bootstrap.renderpipeline.camerasystem;

import com.AdventureRPG.bootstrap.inputpipeline.input.InputSystem;
import com.AdventureRPG.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class CameraManager extends ManagerPackage {

    // Internal
    private InternalBufferSystem internalBufferSystem;
    private InputSystem inputSystem;

    private final ObjectLinkedOpenHashSet<CameraInstance> cameraInstances = new ObjectLinkedOpenHashSet<>();
    private CameraInstance mainCamera;

    private int width;
    private int height;

    // Internal \\

    @Override
    public void create() {

        // Internal
        this.internalBufferSystem = create(InternalBufferSystem.class);
        createCamera(settings.FOV, width, height);
    }

    @Override
    public void get() {

        // Internal
        this.inputSystem = get(InputSystem.class);
    }

    @Override
    protected void update() {

        if (mainCamera == null)
            return;

        mainCamera.setRotation(inputSystem.getRotation());
    }

    // Accessible \\

    public CameraInstance createCamera(float fov, float width, float height) {

        // Internal
        CameraInstance createdCamera = create(CameraInstance.class);
        createdCamera.constructor(fov, width, height);

        cameraInstances.add(createdCamera);

        if (mainCamera == null)
            mainCamera = createdCamera;

        return createdCamera;
    }

    public CameraInstance getMainCamera() {
        return mainCamera;
    }

    public void setMainCamera(CameraInstance cam) {

        if (cam == null)
            return;

        mainCamera = cam;
    }

    public ObjectLinkedOpenHashSet<CameraInstance> getCameraInstances() {
        return cameraInstances;
    }

    public void resize(int width, int height) {

        this.width = width;
        this.height = height;

        mainCamera.updateViewport(width, height);
    }
}
