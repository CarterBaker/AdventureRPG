package application.kernel.inputpipeline.inputsystem;

import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.input.Binding;
import engine.input.Input;
import engine.root.EngineContext;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;

public class InputSystem extends SystemPackage {

    /*
     * Thin bridge between the engine input backend and bootstrap systems.
     * Owns sensitivity-scaled mouse delta and binding query convenience.
     * Writes a full hardware snapshot into a RawInputHandle each frame —
     * reuses pre-allocated arrays to avoid per-frame heap allocation.
     *
     * Input routes to the focused window — the window the user last clicked.
     * Cursor capture pins routing to the captured window while in play mode.
     *
     * Focus is detected here each frame: any mouse click on a window that is
     * not already focused shifts focus to it and syncs capture state. Capture
     * follows focus — granted when the focused window has no input-locked menus,
     * released when it does.
     */

    // Internal
    private WindowManager windowManager;

    // Mouse delta
    private Vector2 mouseDelta;
    private static final Vector2 ZERO_DELTA = new Vector2(0, 0);

    // Pre-allocated snapshot buffers — reused every frame
    private final boolean[] kc = new boolean[512];
    private final boolean[] kh = new boolean[512];
    private final boolean[] kr = new boolean[512];
    private final boolean[] bc = new boolean[8];
    private final boolean[] bh = new boolean[8];
    private final boolean[] br = new boolean[8];

    // Internal \\

    @Override
    protected void create() {
        this.mouseDelta = new Vector2();
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void update() {
        syncFocus();
    }

    // Focus \\

    private void syncFocus() {
        if (!EngineContext.input.isMouseClicked(0) && !EngineContext.input.isMouseClicked(1))
            return;
        WindowInstance hovered = windowManager.getHoveredWindow();
        if (hovered == null || hovered == windowManager.getFocusedWindow())
            return;
        windowManager.setFocusedWindow(hovered);
        onWindowFocused(hovered);
    }

    private void onWindowFocused(WindowInstance window) {
        if (windowManager.getCapturedWindow() != null) {
            windowManager.releaseCaptureLock();
            EngineContext.input.setCursorCatched(false);
        }
        if (!window.getMenuListHandle().isInputLocked()) {
            windowManager.captureLockWindow(window);
            EngineContext.input.setCursorCatched(true);
        }
    }

    // Guard \\

    private boolean isHovered(WindowInstance window) {
        WindowInstance captured = windowManager.getCapturedWindow();
        if (captured != null)
            return window == captured;
        return window == windowManager.getFocusedWindow();
    }

    // Frame write \\

    public void writeRawInput(RawInputHandle handle, WindowInstance window) {
        if (!isHovered(window)) {
            handle.clear();
            return;
        }

        Input raw = EngineContext.input;

        for (int i = 0; i < 512; i++) {
            kc[i] = raw.isKeyClicked(i);
            kh[i] = raw.isKeyDown(i);
            kr[i] = raw.isKeyReleased(i);
        }

        for (int i = 0; i < 8; i++) {
            bc[i] = raw.isMouseClicked(i);
            bh[i] = raw.isMouseDown(i);
            br[i] = raw.isMouseReleased(i);
        }

        handle.write(
                kc, kh, kr,
                bc, bh, br,
                raw.getMouseX(),
                raw.getMouseY(),
                raw.getDeltaX() * internal.settings.mouseSensitivity,
                raw.getDeltaY() * internal.settings.mouseSensitivity);
    }

    // Convenience — bindings \\

    public boolean bindingHeld(Binding binding, WindowInstance window) {
        return isHovered(window) && binding.isDown(EngineContext.input);
    }

    public boolean bindingClicked(Binding binding, WindowInstance window) {
        return isHovered(window) && binding.isClicked(EngineContext.input);
    }

    public boolean bindingReleased(Binding binding, WindowInstance window) {
        return isHovered(window) && binding.isReleased(EngineContext.input);
    }

    // Convenience — direct queries \\

    public float getMouseX(WindowInstance window) {
        return isHovered(window) ? EngineContext.input.getMouseX() : 0f;
    }

    public float getMouseY(WindowInstance window) {
        return isHovered(window) ? EngineContext.input.getMouseY() : 0f;
    }

    public Vector2 getMouseDelta(WindowInstance window) {
        if (!isHovered(window))
            return ZERO_DELTA;
        mouseDelta.set(
                EngineContext.input.getDeltaX() * internal.settings.mouseSensitivity,
                EngineContext.input.getDeltaY() * internal.settings.mouseSensitivity);
        return mouseDelta;
    }

    // Platform \\

    public void captureCursor(boolean captured, WindowInstance window) {
        if (captured) {
            windowManager.captureLockWindow(window);
            EngineContext.input.setCursorCatched(true);
        } else if (windowManager.getCapturedWindow() == window) {
            windowManager.releaseCaptureLock();
            EngineContext.input.setCursorCatched(false);
        }
    }
}