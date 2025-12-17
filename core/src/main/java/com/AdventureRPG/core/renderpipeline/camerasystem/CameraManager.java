package com.AdventureRPG.core.renderpipeline.camerasystem;

import com.AdventureRPG.core.kernel.SystemFrame;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class CameraManager extends SystemFrame {

    // Camera storage
    private final Array<CameraInstance> cameraInstances = new Array<>(false, 4);
    private CameraInstance mainCamera;

    private int width;
    private int height;

    // Init
    @Override
    public void create() {
        createCamera(settings.FOV, width, height);
    }

    // Accessible \\

    public CameraInstance createCamera(float fov, float width, float height) {

        CameraInstance createdCamera = new CameraInstance(fov, width, height);

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

    public Array<CameraInstance> cameraInstances() {
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
