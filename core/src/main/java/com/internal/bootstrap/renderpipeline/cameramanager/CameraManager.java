package com.internal.bootstrap.renderpipeline.cameramanager;

import com.internal.bootstrap.renderpipeline.camera.CameraData;
import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraData;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraInstance;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class CameraManager extends ManagerPackage {

    /*
     * Camera factory, registry, and GPU buffer owner. Creates perspective and
     * orthographic cameras and assigns them to their window. Owns
     * CameraBufferSystem — pushCamera() is the single call RenderManager makes
     * before each draw pass to upload the active window's camera data to the
     * shared GPU UBOs. All UBO knowledge lives here, not in the render pipeline.
     */

    // Internal
    private WindowManager windowManager;

    // Systems
    private CameraBufferSystem cameraBufferSystem;

    // Cameras
    private ObjectLinkedOpenHashSet<CameraInstance> cameraInstances;

    // Internal \\

    @Override
    protected void create() {

        // Systems
        this.cameraBufferSystem = create(CameraBufferSystem.class);

        // Cameras
        this.cameraInstances = new ObjectLinkedOpenHashSet<>();
    }

    @Override
    protected void get() {

        // Internal
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance mainWindow = windowManager.getMainWindow();

        OrthographicCameraData data = new OrthographicCameraData(
                mainWindow.getWidth(),
                mainWindow.getHeight());

        OrthographicCameraInstance orthoCamera = create(OrthographicCameraInstance.class);
        orthoCamera.constructor(data);

        mainWindow.setOrthoCamera(orthoCamera);
    }

    // Camera Factory \\

    public CameraInstance createCamera(
            float fov,
            float width,
            float height) {

        CameraData data = new CameraData(fov, width, height);
        CameraInstance instance = create(CameraInstance.class);
        instance.constructor(data);
        cameraInstances.add(instance);

        return instance;
    }

    // Buffer \\

    public void pushCamera(WindowInstance window) {
        cameraBufferSystem.pushForWindow(window);
    }

    // Accessible \\

    public ObjectLinkedOpenHashSet<CameraInstance> getCameraInstances() {
        return cameraInstances;
    }
}