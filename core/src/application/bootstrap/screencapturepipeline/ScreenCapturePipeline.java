package application.bootstrap.screencapturepipeline;

import application.bootstrap.screencapturepipeline.screencapturemanager.ScreenCaptureManager;
import engine.root.PipelinePackage;

public class ScreenCapturePipeline extends PipelinePackage {

    /*
     * Bootstraps the screen capture feature: a single ScreenCaptureManager
     * exposing one public hook to grab a screenshot and one to toggle video
     * recording, each backed by its own dedicated system.
     */

    @Override
    protected void create() {
        create(ScreenCaptureManager.class);
    }
}