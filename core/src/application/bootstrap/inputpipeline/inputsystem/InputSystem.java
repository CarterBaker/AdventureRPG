package application.bootstrap.inputpipeline.inputsystem;

import engine.root.EngineContext;
import engine.root.SystemPackage;
import engine.util.input.Binding;
import engine.util.mathematics.vectors.Vector2;

public class InputSystem extends SystemPackage {

    /*
     * Thin bridge between the engine input backend and bootstrap systems.
     * Owns sensitivity-scaled mouse delta and binding query convenience.
     * All click, held, and release state delegates directly to EngineContext.input.
     * Mouse delta is computed lazily per call so it always reflects the active
     * window's input context — no stale snapshot from a cached update().
     */

    // Internal
    private float sensitivity;

    // Mouse — delta
    private Vector2 mouseDelta;

    // Internal \\

    @Override
    protected void create() {
        this.sensitivity = internal.settings.mouseSensitivity;
        this.mouseDelta = new Vector2();
    }

    // Platform \\

    public void captureCursor(boolean captured) {
        EngineContext.input.setCursorCatched(captured);
    }

    // Accessible — Bindings \\

    public boolean bindingHeld(Binding binding) {
        return binding.isDown(EngineContext.input);
    }

    public boolean bindingClicked(Binding binding) {
        return binding.isClicked(EngineContext.input);
    }

    public boolean bindingReleased(Binding binding) {
        return binding.isReleased(EngineContext.input);
    }

    // Accessible — Mouse \\

    public float getMouseX() {
        return EngineContext.input.getMouseX();
    }

    public float getMouseY() {
        return EngineContext.input.getMouseY();
    }

    public Vector2 getMouseDelta() {
        mouseDelta.set(
                EngineContext.input.getDeltaX() * sensitivity,
                EngineContext.input.getDeltaY() * sensitivity);
        return mouseDelta;
    }
}