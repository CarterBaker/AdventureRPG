package application.bootstrap.screencapturepipeline.screencapturemanager;

import java.io.File;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

class ScreenCaptureIOUtility extends EngineUtility {

    /*
     * Stateless path resolution helper shared by ScreenshotSystem and
     * VideoRecordingSystem. Capture output always resolves against the base
     * game directory the engine was launched from rather than the working
     * directory assets stream from, so a shipped build never depends on
     * anything present on the end user's machine.
     */

    static File resolveCaptureDirectory(File baseGameDirectory, String subdirectoryName) {

        File directory = new File(new File(baseGameDirectory, EngineSetting.CAPTURE_ROOT_DIRECTORY),
                subdirectoryName);

        if (!directory.exists() && !directory.mkdirs())
            throwException("Failed to create capture output directory: " + directory.getAbsolutePath());

        return directory;
    }
}