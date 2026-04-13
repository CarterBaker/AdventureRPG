package application.bootstrap.renderpipeline.cameramanager;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ManagerPackage;
import engine.util.camera.CameraInstance;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class CameraManager extends ManagerPackage {

    /*
     * Camera factory, registry, and GPU buffer owner. Creates perspective and
     * orthographic cameras and assigns them to their window. Owns
     * CameraBufferSystem — pushCamera() is the single call RenderManager makes
     * before each draw pass to upload the active window's camera data to the
     * shared GPU UBOs. All UBO knowledge lives here, not in the render pipeline.
     */

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

    // Buffer \\

    public void pushCamera(WindowInstance window) {
        cameraBufferSystem.pushForWindow(window);
    }

    // Accessible \\

    public ObjectLinkedOpenHashSet<CameraInstance> getCameraInstances() {
        return cameraInstances;
    }
}
