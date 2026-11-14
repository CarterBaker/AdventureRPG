package application.kernel.inputpipeline.inputmanager;

import application.bootstrap.shaderpipeline.sprite.SpriteData;
import application.bootstrap.shaderpipeline.sprite.SpriteHandle;
import application.bootstrap.shaderpipeline.sprite.SpriteInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineContext;
import engine.root.SystemPackage;

public class CursorSystem extends SystemPackage {

    /*
     * Owns cursor capture and cursor shape. Capture is driven only by
     * InputManager, which decides when it is granted or released. Shape is
     * either the OS default or a sprite; Lwjgl3Input reads back and caches the
     * sprite's cursor per GPU handle.
     */

    // Internal
    private WindowManager windowManager;

    // Internal \\

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
    }

    // Capture — package-private, driven by InputManager \\

    void capture(WindowInstance window) {
        windowManager.captureLockWindow(window);
        internal.windowPlatform.getInputForWindow(window.getGLWindow()).setCursorCatched(true);
    }

    void releaseCapture() {
        WindowInstance previouslyCaptured = windowManager.getCapturedWindow();
        windowManager.releaseCaptureLock();
        if (previouslyCaptured != null)
            internal.windowPlatform.getInputForWindow(previouslyCaptured.getGLWindow()).setCursorCatched(false);
    }

    void releaseCaptureIfOwner(WindowInstance window) {
        if (windowManager.getCapturedWindow() == window)
            releaseCapture();
    }

    void captureCursor(boolean captured, WindowInstance window) {
        if (captured)
            capture(window);
        else
            releaseCaptureIfOwner(window);
    }

    // Sprite cursor — public \\

    public void setCursorSprite(SpriteHandle handle) {
        applySpriteData(handle.getSpriteData());
    }

    public void setCursorSprite(SpriteInstance instance) {
        applySpriteData(instance.getSpriteData());
    }

    public void clearCursor() {
        EngineContext.input.clearCursor();
    }

    // Implementation \\

    private void applySpriteData(SpriteData data) {
        EngineContext.input.setCursorFromSprite(data.getGpuHandle(), data.getWidth(), data.getHeight());
    }
}