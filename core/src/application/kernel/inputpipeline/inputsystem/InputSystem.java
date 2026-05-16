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
     * Input is hover-gated: if the context window is not the one the mouse
     * is currently over, the handle is cleared to neutral. No concept of
     * active or focused window — all windows are equal.
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

    // Guard \\

    private boolean isHovered() {
        WindowInstance ctx = windowManager.getContextWindow();
        if (ctx == null)
            return true;
        return ctx == windowManager.getHoveredWindow();
    }

    // Frame write \\

    public void writeRawInput(RawInputHandle handle) {
        if (!isHovered()) {
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

    public boolean bindingHeld(Binding binding) {
        return isHovered() && binding.isDown(EngineContext.input);
    }

    public boolean bindingClicked(Binding binding) {
        return isHovered() && binding.isClicked(EngineContext.input);
    }

    public boolean bindingReleased(Binding binding) {
        return isHovered() && binding.isReleased(EngineContext.input);
    }

    // Convenience — direct queries \\

    public float getMouseX() {
        return isHovered() ? EngineContext.input.getMouseX() : 0f;
    }

    public float getMouseY() {
        return isHovered() ? EngineContext.input.getMouseY() : 0f;
    }

    public Vector2 getMouseDelta() {
        if (!isHovered())
            return ZERO_DELTA;
        mouseDelta.set(
                EngineContext.input.getDeltaX() * internal.settings.mouseSensitivity,
                EngineContext.input.getDeltaY() * internal.settings.mouseSensitivity);
        return mouseDelta;
    }

    // Platform \\

    public void captureCursor(boolean captured) {
        EngineContext.input.setCursorCatched(captured);
    }
}