package com.internal.bootstrap.inputpipeline.inputsystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.vectors.Vector2;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class InputSystem extends SystemPackage implements InputProcessor {

    // Internal
    private boolean locked = false;
    private float sensitivity = 0.15f;

    private Vector2 rotation;
    private Vector3Int input;

    // Movement
    private boolean W = false;
    private boolean A = false;
    private boolean S = false;
    private boolean D = false;
    private boolean CTRL = false;
    private boolean SHIFT = false;
    private boolean SPACE = false;

    // Mouse — game, blocked when locked
    private boolean leftClick = false;
    private boolean rightClick = false;

    // Mouse — raw, always tracked, used by UI raycasting
    private boolean rawLeftClick = false;
    private float mouseX = 0f;
    private float mouseY = 0f;

    // UI keys — always tracked regardless of lock
    private boolean inventoryJustPressed = false;
    private boolean inventoryDown = false;

    // =========================================================
    // DEBUG — remove when real item system is wired
    private boolean debugItem1JustPressed = false;
    private boolean debugItem1Down = false;
    private boolean debugItem2JustPressed = false;
    private boolean debugItem2Down = false;
    // =========================================================

    // Base \\

    @Override
    protected void create() {
        rotation = new Vector2();
        input = new Vector3Int();
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

        // UI keys — always tracked regardless of lock
        if (keycode == Input.Keys.I && !inventoryDown) {
            inventoryDown = true;
            inventoryJustPressed = true;
        }

        // DEBUG
        if (keycode == Input.Keys.NUM_1 && !debugItem1Down) {
            debugItem1Down = true;
            debugItem1JustPressed = true;
        }
        if (keycode == Input.Keys.NUM_2 && !debugItem2Down) {
            debugItem2Down = true;
            debugItem2JustPressed = true;
        }
        // END DEBUG

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
        // DEBUG
        if (keycode == Input.Keys.NUM_1)
            debugItem1Down = false;
        if (keycode == Input.Keys.NUM_2)
            debugItem2Down = false;
        // END DEBUG

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

    // Consume — reads and clears in one call so update order does not matter \\

    public boolean consumeInventoryJustPressed() {
        boolean val = inventoryJustPressed;
        inventoryJustPressed = false;
        return val;
    }

    // DEBUG — remove with 1/2 key handling in PlayerManager
    public boolean consumeDebugItem1JustPressed() {
        boolean val = debugItem1JustPressed;
        debugItem1JustPressed = false;
        return val;
    }

    public boolean consumeDebugItem2JustPressed() {
        boolean val = debugItem2JustPressed;
        debugItem2JustPressed = false;
        return val;
    }
    // END DEBUG

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