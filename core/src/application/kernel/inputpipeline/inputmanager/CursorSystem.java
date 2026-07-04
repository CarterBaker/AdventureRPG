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
     * Owns all cursor state: capture routing and cursor shape.
     *
     * Capture methods are package-private — driven exclusively by InputManager,
     * which remains the single authority over when capture is granted or released.
     * CursorSystem only executes the platform calls on its behalf.
     *
     * Shape is either the OS default (clearCursor) or a custom sprite texture.
     * Both public overloads funnel through applySpriteData() which pulls identity
     * and GPU info from the wrapped SpriteData — SpriteHandle and SpriteInstance
     * are never distinguished below the entry points.
     *
     * Pixel readback and GLFW cursor handle caching are owned by Lwjgl3Input.
     * setCursorFromSprite triggers a glGetTexImage on first use per GPU handle;
     * subsequent calls with the same handle hit the cache and cost only a
     * glfwSetCursor call.
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