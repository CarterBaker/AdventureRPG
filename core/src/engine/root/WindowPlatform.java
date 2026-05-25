package engine.root;

import application.kernel.windowpipeline.window.WindowInstance;

public interface WindowPlatform {
    /*
     * Backend contract for all GLFW window operations. Implemented once per
     * platform and injected at launch. The engine never calls GLFW directly —
     * all window management routes through here.
     */
    void openWindow(WindowInstance window);

    void destroyWindow(WindowInstance window);

    boolean shouldClose(WindowInstance window);

    boolean isWindowFocused(WindowInstance window);

    void makeContextCurrent(WindowInstance window);

    void syncInputForWindow(WindowInstance window);

    void swapBuffers(WindowInstance window);

    void restoreMainContext();

    void setCursorShape(long windowHandle, int shape);

    void syncWindowSize(WindowInstance window);

    void exit();

    /*
     * Returns the cursor position relative to the given OS window's client
     * area, queried directly from the platform without affecting the current
     * input context or hoveredWindow state. Used by TabDragManager to obtain
     * window-local cursor coordinates for BSP drop-target resolution when the
     * input-synced window (the drag source) differs from the window being
     * tested as a drop target.
     *
     * Callers must pass a window with a valid native handle. Results for
     * logical windows (no native handle) are undefined.
     */
    float getCursorX(WindowInstance window);

    float getCursorY(WindowInstance window);

    /*
     * Returns the OS-level screen position of the given window's top-left
     * corner. Called by openWindow and by any window-moved callback so
     * WindowInstance.screenX/Y stays current. Used by TabDragManager to test
     * whether the global cursor position falls within a specific OS window's
     * screen bounds during drag resolution.
     */
    float getScreenX(WindowInstance window);

    float getScreenY(WindowInstance window);
}