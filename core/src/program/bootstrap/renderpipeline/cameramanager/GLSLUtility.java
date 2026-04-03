package program.bootstrap.renderpipeline.cameramanager;

import program.bootstrap.renderpipeline.camera.CameraData;
import program.bootstrap.renderpipeline.camera.CameraInstance;
import program.bootstrap.renderpipeline.camera.OrthographicCameraData;
import program.bootstrap.renderpipeline.camera.OrthographicCameraInstance;
import program.core.engine.InstancePackage;
import program.core.engine.UtilityPackage;

/*
 * Camera construction bridge used by both WindowInstance and CameraManager.
 * Keeps camera creation free from manager lifecycle timing, while preserving
 * the same camera data initialization path in one place.
 */
public class GLSLUtility extends UtilityPackage {

    // Factory \\

    public static CameraInstance createCamera(
            InstancePackage owner,
            float fov,
            float width,
            float height) {

        CameraData data = new CameraData(fov, width, height);
        CameraInstance instance = owner.create(CameraInstance.class);
        instance.constructor(data);
        return instance;
    }

    public static OrthographicCameraInstance createOrthographicCamera(
            InstancePackage owner,
            float width,
            float height) {

        OrthographicCameraData data = new OrthographicCameraData(width, height);
        OrthographicCameraInstance instance = owner.create(OrthographicCameraInstance.class);
        instance.constructor(data);
        return instance;
    }
}
