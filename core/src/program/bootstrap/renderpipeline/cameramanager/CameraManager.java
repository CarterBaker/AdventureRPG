package program.bootstrap.renderpipeline.cameramanager;

import program.bootstrap.renderpipeline.camera.CameraInstance;
import program.bootstrap.renderpipeline.camera.OrthographicCameraInstance;
import program.bootstrap.renderpipeline.window.WindowInstance;
import program.bootstrap.renderpipeline.windowmanager.WindowManager;
import program.core.engine.ManagerPackage;
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

    // Camera Factory \\

    public CameraInstance createCamera(
            float fov,
            float width,
            float height) {

        CameraInstance instance = GLSLUtility.createCamera(this, fov, width, height);
        cameraInstances.add(instance);

        return instance;
    }

    public OrthographicCameraInstance createOrthographicCamera(
            float width,
            float height) {

        return GLSLUtility.createOrthographicCamera(this, width, height);
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
