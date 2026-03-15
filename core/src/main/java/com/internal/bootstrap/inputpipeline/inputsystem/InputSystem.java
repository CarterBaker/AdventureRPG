package com.internal.bootstrap.inputpipeline.inputsystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.vectors.Vector2;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class InputSystem extends SystemPackage implements InputProcessor {

    /*
     * Handles all raw input from LibGDX and exposes clean state to the engine.
     * Movement and camera rotation are blocked when the input is locked.
     * UI keys and raw mouse state are always tracked regardless of lock state.
     */

    // Internal
    private boolean locked;
    private float sensitivity;

    // Vectors
    private Vector2 rotation;
    private Vector3Int input;

    // Movement
    private boolean W;
    private boolean A;
    private boolean S;
    private boolean D;
    private boolean CTRL;
    private boolean SHIFT;
    private boolean SPACE;

    // Mouse — game, blocked when locked
    private boolean leftClick;
    private boolean rightClick;

    // Mouse — raw, always tracked, used by UI raycasting
    private boolean rawLeftClick;
    private float mouseX;
    private float mouseY;

    // UI Keys — always tracked regardless of lock
    private boolean inventoryJustPressed;
    private boolean inventoryDown;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.locked = false;
        this.sensitivity = internal.settings.mouseSensitivity;

        // Vectors
        this.rotation = new Vector2();
        this.input = new Vector3Int();
    }

    @Override
    protected void start() {
        Gdx.input.setInputProcessor(this);
        lockInput(locked);
    }

    @Override
    protected void update() {
        updateRotation();
        updateMovement();
    }

    // Input \\

    private void updateRotation() {

        rotation.set(0, 0);

        if (locked)
            return;

        float deltaX = Gdx.input.getDeltaX() * sensitivity;
        float deltaY = Gdx.input.getDeltaY() * sensitivity;

        rotation.set(deltaX, deltaY);
    }

    private void updateMovement() {

        input.set(0, 0, 0);

        if (locked)
            return;

        if (W)
            input.z += 1;
        if (S)
            input.z -= 1;
        if (A)
            input.x -= 1;
        if (D)
            input.x += 1;
        if (SPACE)
            input.y = 1;
    }

    // Input Processor \\

    @Override
    public boolean keyDown(int keycode) {

        if (keycode == Input.Keys.I && !inventoryDown) {
            inventoryDown = true;
            inventoryJustPressed = true;
        }

        if (locked)
            return false;

        switch (keycode) {
            case Input.Keys.W -> W = true;
            case Input.Keys.A -> A = true;
            case Input.Keys.S -> S = true;
            case Input.Keys.D -> D = true;
            case Input.Keys.CONTROL_LEFT -> CTRL = true;
            case Input.Keys.SHIFT_LEFT -> SHIFT = true;
            case Input.Keys.SPACE -> SPACE = true;
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {

        if (keycode == Input.Keys.I)
            inventoryDown = false;

        if (locked)
            return false;

        switch (keycode) {
            case Input.Keys.W -> W = false;
            case Input.Keys.A -> A = false;
            case Input.Keys.S -> S = false;
            case Input.Keys.D -> D = false;
            case Input.Keys.CONTROL_LEFT -> CTRL = false;
            case Input.Keys.SHIFT_LEFT -> SHIFT = false;
            case Input.Keys.SPACE -> SPACE = false;
        }

        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        if (button == Input.Buttons.LEFT)
            rawLeftClick = true;

        if (locked)
            return false;

        if (button == Input.Buttons.LEFT)
            leftClick = true;

        if (button == Input.Buttons.RIGHT)
            rightClick = true;

        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {

        if (button == Input.Buttons.LEFT)
            rawLeftClick = false;

        if (locked)
            return false;

        if (button == Input.Buttons.LEFT)
            leftClick = false;

        if (button == Input.Buttons.RIGHT)
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

    // Input Locking \\

    public void lockInput(boolean input) {
        this.locked = input;
        Gdx.input.setCursorCatched(!input);
    }

    // Consume \\

    public boolean consumeInventoryJustPressed() {
        boolean val = inventoryJustPressed;
        inventoryJustPressed = false;
        return val;
    }

    // Accessible \\

    public boolean isLocked() {
        return locked;
    }

    public Vector2 getRotation() {
        return rotation;
    }

    public Vector3Int getInput() {
        return input;
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

    public boolean isWalkHeld() {
        return CTRL;
    }

    public boolean isSprintHeld() {
        return SHIFT;
    }

    public float getMouseX() {
        return mouseX;
    }

    public float getMouseY() {
        return mouseY;
    }
}