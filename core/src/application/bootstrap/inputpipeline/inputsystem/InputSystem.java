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

    @Override
    protected void update() {
        float dx = EngineContext.input.getDeltaX() * sensitivity;
        float dy = EngineContext.input.getDeltaY() * sensitivity;
        mouseDelta.set(dx, dy);
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
        return mouseDelta;
    }
}