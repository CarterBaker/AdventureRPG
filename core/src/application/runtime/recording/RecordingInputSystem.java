package application.runtime.recording;

import application.bootstrap.screencapturepipeline.screencapturemanager.ScreenCaptureManager;
import application.runtime.input.InputSystem;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;

public class RecordingInputSystem extends SystemPackage {

    /*
     * Runtime-thin input bridge for on-demand screen capture. Reuses the
     * context's own InputSystem for raw input rather than owning a second
     * RawInputHandle, and forwards clicks straight to ScreenCaptureManager's
     * two public hooks — one for a single screenshot, one to toggle video
     * recording — with no capture logic of its own.
     */

    // Internal
    private InputSystem inputSystem;
    private ScreenCaptureManager screenCaptureManager;

    // Internal \\

    @Override
    protected void get() {
        this.inputSystem = get(InputSystem.class);
        this.screenCaptureManager = get(ScreenCaptureManager.class);
    }

    @Override
    protected void update() {

        if (inputSystem.getRawInputHandle().isBindingClicked(KeyBindings.SCREENSHOT))
            screenCaptureManager.captureScreenshot(context.getWindow());

        if (inputSystem.getRawInputHandle().isBindingClicked(KeyBindings.RECORD_VIDEO))
            screenCaptureManager.toggleRecording(context.getWindow());
    }
}