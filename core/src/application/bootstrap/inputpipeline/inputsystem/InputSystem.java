package application.bootstrap.inputpipeline.inputsystem;

import engine.root.EngineContext;
import engine.root.SystemPackage;
import engine.util.input.Binding;
import engine.util.input.Buttons;
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

    // Accessible — Keys \\

    public boolean keyHeld(int key) {
        return EngineContext.input.isKeyDown(key);
    }

    public boolean keyClicked(int key) {
        return EngineContext.input.isKeyClicked(key);
    }

    public boolean keyReleased(int key) {
        return EngineContext.input.isKeyReleased(key);
    }

    // Accessible — Bindings \\

    public boolean bindingHeld(Binding binding) {

        int[] keys = binding.getCodes();

        for (int key : keys)
            if (!EngineContext.input.isKeyDown(key))
                return false;

        return true;
    }

    public boolean bindingClicked(Binding binding) {

        int[] keys = binding.getCodes();

        for (int i = 0; i < keys.length - 1; i++)
            if (!EngineContext.input.isKeyDown(keys[i]))
                return false;

        return EngineContext.input.isKeyClicked(keys[keys.length - 1]);
    }

    public boolean bindingReleased(Binding binding) {

        int[] keys = binding.getCodes();

        for (int i = 0; i < keys.length - 1; i++)
            if (!EngineContext.input.isKeyDown(keys[i]))
                return false;

        return EngineContext.input.isKeyReleased(keys[keys.length - 1]);
    }

    // Accessible — Mouse \\

    public boolean isLeftClicked() {
        return EngineContext.input.isMouseClicked(Buttons.LEFT);
    }

    public boolean isLeftDown() {
        return EngineContext.input.isMouseDown(Buttons.LEFT);
    }

    public boolean isLeftReleased() {
        return EngineContext.input.isMouseReleased(Buttons.LEFT);
    }

    public boolean isRightClicked() {
        return EngineContext.input.isMouseClicked(Buttons.RIGHT);
    }

    public boolean isRightDown() {
        return EngineContext.input.isMouseDown(Buttons.RIGHT);
    }

    public boolean isRightReleased() {
        return EngineContext.input.isMouseReleased(Buttons.RIGHT);
    }

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