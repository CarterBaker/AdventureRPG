package program.bootstrap.renderpipeline.cameramanager;

import program.core.engine.InstancePackage;
import program.core.engine.ManagerPackage;
import program.core.kernel.window.WindowInstance;
import program.core.kernel.windowmanager.WindowManager;
import program.core.util.camera.CameraData;
import program.core.util.camera.CameraInstance;
import program.core.util.camera.OrthographicCameraData;
import program.core.util.camera.OrthographicCameraInstance;
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

    // Buffer \\

    public void pushCamera(WindowInstance window) {
        cameraBufferSystem.pushForWindow(window);
    }

    // Accessible \\

    public ObjectLinkedOpenHashSet<CameraInstance> getCameraInstances() {
        return cameraInstances;
    }
}
