package application.bootstrap.inputpipeline.inputsystem;

import engine.input.Binding;
import engine.input.InputListener;
import engine.root.EngineContext;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class InputSystem extends SystemPackage implements InputListener {

    /*
     * Captures all raw input from platform each frame and exposes clean query
     * methods. No game knowledge — no named keys, no lock state, no handle
     * writing. Any system that needs input reads from this and interprets the
     * state in its own context.
     */

    // Internal
    private float sensitivity;

    // Keys
    private IntOpenHashSet heldKeys;
    private IntOpenHashSet justPressedKeys;
    private IntOpenHashSet justPressedSwap;

    // Mouse — delta
    private Vector2 mouseDelta;

    // Mouse — position
    private float mouseX;
    private float mouseY;

    // Mouse — buttons
    private boolean leftDown;
    private boolean rightDown;

    // Internal \\

    @Override
    protected void create() {

        this.sensitivity = internal.settings.mouseSensitivity;
        this.heldKeys = new IntOpenHashSet();
        this.justPressedKeys = new IntOpenHashSet();
        this.justPressedSwap = new IntOpenHashSet();
        this.mouseDelta = new Vector2();
    }

    @Override
    protected void start() {
        EngineContext.input.addListener(this);
    }

    @Override
    protected void dispose() {
        EngineContext.input.removeListener(this);
    }

    @Override
    protected void update() {

        IntOpenHashSet temp = justPressedKeys;
        justPressedKeys = justPressedSwap;
        justPressedSwap = temp;
        justPressedSwap.clear();

        float dx = EngineContext.input.getDeltaX() * sensitivity;
        float dy = EngineContext.input.getDeltaY() * sensitivity;
        mouseDelta.set(dx, dy);
    }

    // InputListener \\

    @Override
    public void onKeyDown(int key) {
        heldKeys.add(key);
        justPressedSwap.add(key);
    }

    @Override
    public void onKeyUp(int key) {
        heldKeys.remove(key);
    }

    @Override
    public void onMouseDown(int button, int x, int y) {
        if (button == engine.input.Buttons.LEFT)
            leftDown = true;
        if (button == engine.input.Buttons.RIGHT)
            rightDown = true;
    }

    @Override
    public void onMouseUp(int button, int x, int y) {
        if (button == engine.input.Buttons.LEFT)
            leftDown = false;
        if (button == engine.input.Buttons.RIGHT)
            rightDown = false;
    }

    @Override
    public void onMouseMoved(int x, int y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    // Platform \\

    public void captureCursor(boolean captured) {
        EngineContext.input.setCursorCatched(captured);
    }

    // Accessible \\

    public boolean keyHeld(int key) {
        return heldKeys.contains(key);
    }

    public boolean keyJustPressed(int key) {
        return justPressedKeys.contains(key);
    }

    public boolean bindingHeld(Binding binding) {
        for (int key : binding.getKeys())
            if (!heldKeys.contains(key))
                return false;
        return true;
    }

    public boolean bindingJustPressed(Binding binding) {
        int[] keys = binding.getKeys();
        if (!justPressedKeys.contains(keys[keys.length - 1]))
            return false;
        for (int i = 0; i < keys.length - 1; i++)
            if (!heldKeys.contains(keys[i]))
                return false;
        return true;
    }

    public Vector2 getMouseDelta() {
        return mouseDelta;
    }

    public float getMouseX() {
        return mouseX;
    }

    public float getMouseY() {
        return mouseY;
    }

    public boolean isLeftDown() {
        return leftDown;
    }

    public boolean isRightDown() {
        return rightDown;
    }
}