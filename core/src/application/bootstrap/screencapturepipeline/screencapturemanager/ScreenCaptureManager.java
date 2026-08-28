package application.bootstrap.screencapturepipeline.screencapturemanager;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ManagerPackage;

public class ScreenCaptureManager extends ManagerPackage {

    /*
     * Owns screenshot and video recording as two independent child systems,
     * exposing exactly the two hooks any input system needs to drive capture
     * — captureScreenshot() for a single still, and toggleRecording() to
     * start or stop a video session — both keyed to the window the caller
     * supplies rather than any window this manager resolves on its own.
     */

    // Internal
    private ScreenshotSystem screenshotSystem;
    private VideoRecordingSystem videoRecordingSystem;

    // Internal \\

    @Override
    protected void create() {
        this.screenshotSystem = create(ScreenshotSystem.class);
        this.videoRecordingSystem = create(VideoRecordingSystem.class);
    }

    // Accessible \\

    public void captureScreenshot(WindowInstance window) {
        screenshotSystem.capture(window);
    }

    public void toggleRecording(WindowInstance window) {
        videoRecordingSystem.toggleRecording(window);
    }
}