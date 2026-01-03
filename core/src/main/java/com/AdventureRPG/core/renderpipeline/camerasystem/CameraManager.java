package com.AdventureRPG.core.renderpipeline.camerasystem;

import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.util.mathematics.vectors.Vector2;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class CameraManager extends ManagerPackage {

    // Internal
    private InternalBufferSystem internalBufferSystem;

    // Camera storage
    private final ObjectLinkedOpenHashSet<CameraInstance> cameraInstances = new ObjectLinkedOpenHashSet<>();
    private CameraInstance mainCamera;

    private int width;
    private int height;

    // Get
    @Override
    public void create() {

        // Internal
        this.internalBufferSystem = create(InternalBufferSystem.class);
        createCamera(settings.FOV, width, height);
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

    public void setMainCamera(CameraInstance cam) {

        if (cam == null)
            return;

        mainCamera = cam;
    }

    public CameraInstance mainCamera() {
        return mainCamera;
    }

    public ObjectLinkedOpenHashSet<CameraInstance> cameraInstances() {
        return cameraInstances;
    }

    public void resize(int width, int height) {

        this.width = width;
        this.height = height;

        mainCamera.updateViewport(width, height);
    }

    public void rotateCamera(Vector2 input) {
        rotateCamera(mainCamera, input);
    }

    // TODO: Not sure I want to be doing this
    public void moveCamera(Vector3 input) {
        moveCamera(mainCamera, input);
    }

    public void rotateCamera(CameraInstance cameraInstance, Vector2 input) {
        cameraInstance.setRotation(input);
    }

    public void moveCamera(CameraInstance cameraInstance, Vector3 input) {
        cameraInstance.setPosition(input);
    }
}
