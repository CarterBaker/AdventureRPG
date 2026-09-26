package application.bootstrap.screencapturepipeline.screencapturemanager;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ManagerPackage;

public class ScreenCaptureManager extends ManagerPackage {

    /*
     * Owns screenshots and video recording for the window the caller names.
     * Requests only record intent; all readback and file dispatch happen in
     * flush(), called once per frame by the engine's draw.
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

    // Draw Authority \\

    public void flush() {
        screenshotSystem.flush();
        videoRecordingSystem.flush();
    }
}