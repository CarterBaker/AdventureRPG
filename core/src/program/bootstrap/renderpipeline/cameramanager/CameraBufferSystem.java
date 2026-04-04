package program.bootstrap.renderpipeline.cameramanager;

import program.bootstrap.shaderpipeline.ubo.UBOHandle;
import program.bootstrap.shaderpipeline.ubomanager.UBOManager;
import program.core.engine.SystemPackage;
import program.core.kernel.window.WindowInstance;
import program.core.settings.EngineSetting;
import program.core.util.camera.CameraInstance;
import program.core.util.camera.OrthographicCameraInstance;

class CameraBufferSystem extends SystemPackage {

    /*
     * Pushes a window's perspective and orthographic camera data to the shared
     * GPU UBOs. Called by CameraManager.pushCamera() once per draw pass before
     * RenderSystem flushes. Takes the window directly — no WindowManager
     * dependency. All UBO knowledge is contained here within the camera pipeline.
     */

    // Internal
    private UBOManager uboManager;

    // UBOs
    private UBOHandle cameraDataUBO;
    private UBOHandle orthoDataUBO;

    // Internal \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.cameraDataUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.CAMERA_DATA_UBO);
        this.orthoDataUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.ORTHO_DATA_UBO);
    }

    // Push \\

    private int debugFrameCount = 0;

    void pushForWindow(WindowInstance window) {
        if (debugFrameCount == 60)
            debug(window);
        debugFrameCount++;
        pushPerspective(window);
        pushOrtho(window);
    }

    void debug(WindowInstance window) {
        CameraInstance camera = window.getActiveCamera();
        OrthographicCameraInstance ortho = window.getOrthoCamera();

        System.out.println("=== CameraBufferSystem Debug ===");
        System.out.println("Window ID: " + window.getWindowID());

        if (camera == null) {
            System.out.println("PERSPECTIVE CAMERA: null");
        } else {
            System.out.println("PERSPECTIVE CAMERA:");
            System.out.println("  Position:        " + camera.getPosition());
            System.out.println("  FOV:             " + camera.getFOV());
            System.out.println("  Near:            " + camera.getNearPlane());
            System.out.println("  Far:             " + camera.getFarPlane());
            System.out.println("  Viewport:        " + camera.getViewport());
            System.out.println("  Projection:      " + camera.getProjection());
            System.out.println("  View:            " + camera.getView());
            System.out.println("  ViewProjection:  " + camera.getViewProjection());
        }

        if (ortho == null) {
            System.out.println("ORTHO CAMERA: null");
        } else {
            System.out.println("ORTHO CAMERA:");
            System.out.println("  Projection:   " + ortho.getProjection());
            System.out.println("  Screen Size:  " + ortho.getScreenSize());
        }

        System.out.println("================================");
    }

    // Perspective \\

    private void pushPerspective(WindowInstance window) {

        CameraInstance camera = window.getActiveCamera();

        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_PROJECTION, camera.getProjection());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_VIEW, camera.getView());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_INVERSE_PROJECTION, camera.getInverseProjection());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_INVERSE_VIEW, camera.getInverseView());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_VIEW_PROJECTION, camera.getViewProjection());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_POSITION, camera.getPosition());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_FOV, camera.getFOV());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_VIEWPORT, camera.getViewport());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_NEAR_PLANE, camera.getNearPlane());
        cameraDataUBO.updateUniform(EngineSetting.UNIFORM_CAM_FAR_PLANE, camera.getFarPlane());

        uboManager.push(cameraDataUBO);
    }

    // Ortho \\

    private void pushOrtho(WindowInstance window) {

        OrthographicCameraInstance ortho = window.getOrthoCamera();

        if (ortho == null)
            return;

        orthoDataUBO.updateUniform(EngineSetting.UNIFORM_ORTHO_PROJECTION, ortho.getProjection());
        orthoDataUBO.updateUniform(EngineSetting.UNIFORM_ORTHO_SCREEN_SIZE, ortho.getScreenSize());

        uboManager.push(orthoDataUBO);
    }
}