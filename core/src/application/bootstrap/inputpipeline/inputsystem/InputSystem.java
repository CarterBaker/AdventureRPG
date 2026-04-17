package application.bootstrap.inputpipeline.inputsystem;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.input.Binding;
import engine.root.EngineContext;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;

public class InputSystem extends SystemPackage {

    /*
     * Thin bridge between the engine input backend and bootstrap systems.
     * Owns sensitivity-scaled mouse delta and binding query convenience.
     * All click, held, and release state delegates directly to EngineContext.input.
     * Mouse delta is computed lazily per call so it always reflects the active
     * window's input context — no stale snapshot from a cached update().
     *
     * Input is gated: if the window currently executing (contextWindow) is not
     * the focused active window, all queries return neutral values. This prevents
     * unfocused editor windows from reacting to input events.
     */

    // Internal
    private WindowManager windowManager;

    // Mouse — delta
    private Vector2 mouseDelta;

    private static final Vector2 ZERO_DELTA = new Vector2(0, 0);

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void create() {

        // Mouse — delta
        this.mouseDelta = new Vector2();
    }

    // Guard \\

    /**
     * Returns true only when the window currently executing this system's context
     * is also the window that has OS focus. Unfocused windows are silenced.
     */
    private boolean isActiveContext() {

        WindowInstance contextWindow = windowManager.getContextWindow();

        // contextWindow is only set when explicitly bracketed by beginContextWindow/
        // endContextWindow in the pipeline. If it's null we have no context information
        // to gate on — allow input through rather than silencing everything.
        if (contextWindow == null)
            return true;

        return contextWindow == windowManager.getActiveWindow();
    }

    // Platform \\

    public void captureCursor(boolean captured) {
        if (!isActiveContext())
            return;
        EngineContext.input.setCursorCatched(captured);
    }

    // Accessible — Bindings \\

    public boolean bindingHeld(Binding binding) {
        return isActiveContext() && binding.isDown(EngineContext.input);
    }

    public boolean bindingClicked(Binding binding) {
        return isActiveContext() && binding.isClicked(EngineContext.input);
    }

    public boolean bindingReleased(Binding binding) {
        return isActiveContext() && binding.isReleased(EngineContext.input);
    }

    // Accessible — Mouse \\

    public float getMouseX() {
        return isActiveContext() ? EngineContext.input.getMouseX() : 0f;
    }

    public float getMouseY() {
        return isActiveContext() ? EngineContext.input.getMouseY() : 0f;
    }

    public Vector2 getMouseDelta() {
        if (!isActiveContext())
            return ZERO_DELTA;

        mouseDelta.set(
                EngineContext.input.getDeltaX() * internal.settings.mouseSensitivity,
                EngineContext.input.getDeltaY() * internal.settings.mouseSensitivity);
        return mouseDelta;
    }
}