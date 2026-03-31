package com.internal.bootstrap.inputpipeline.inputsystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class InputSystem extends SystemPackage implements InputProcessor {

    /*
     * Captures all raw input from LibGDX each frame and exposes clean query
     * methods. No game knowledge — no named keys, no lock state, no handle
     * writing. Any system that needs input reads from this and interprets the
     * state in its own context. captureCursor() is the only platform call
     * exposed — runtime systems call it rather than touching Gdx directly.
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
    private boolean leftClick;
    private boolean rightClick;
    private boolean rawLeftClick;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.sensitivity = internal.settings.mouseSensitivity;

        // Keys
        this.heldKeys = new IntOpenHashSet();
        this.justPressedKeys = new IntOpenHashSet();
        this.justPressedSwap = new IntOpenHashSet();

        // Mouse
        this.mouseDelta = new Vector2();
    }

    @Override
    protected void start() {
        Gdx.input.setInputProcessor(this);
    }

    @Override
    protected void update() {

        // Swap and clear — justPressedSwap was populated by keyDown events
        // this frame, becomes the readable set for this tick
        IntOpenHashSet temp = justPressedKeys;
        justPressedKeys = justPressedSwap;
        justPressedSwap = temp;
        justPressedSwap.clear();

        float deltaX = Gdx.input.getDeltaX() * sensitivity;
        float deltaY = Gdx.input.getDeltaY() * sensitivity;
        mouseDelta.set(deltaX, deltaY);
    }

    // Input Processor \\

    @Override
    public boolean keyDown(int keycode) {
        heldKeys.add(keycode);
        justPressedSwap.add(keycode);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        heldKeys.remove(keycode);
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        if (button == com.badlogic.gdx.Input.Buttons.LEFT) {
            leftClick = true;
            rawLeftClick = true;
        }

        if (button == com.badlogic.gdx.Input.Buttons.RIGHT)
            rightClick = true;

        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {

        if (button == com.badlogic.gdx.Input.Buttons.LEFT) {
            leftClick = false;
            rawLeftClick = false;
        }

        if (button == com.badlogic.gdx.Input.Buttons.RIGHT)
            rightClick = false;

        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        this.mouseX = screenX;
        this.mouseY = screenY;
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    // Platform \\

    public void captureCursor(boolean captured) {
        Gdx.input.setCursorCatched(captured);
    }

    // Accessible \\

    public boolean keyHeld(int keycode) {
        return heldKeys.contains(keycode);
    }

    public boolean keyJustPressed(int keycode) {
        return justPressedKeys.contains(keycode);
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

    public boolean isLeftClick() {
        return leftClick;
    }

    public boolean isRightClick() {
        return rightClick;
    }

    public boolean isRawLeftClick() {
        return rawLeftClick;
    }
}