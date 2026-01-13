package com.internal.bootstrap.inputpipeline.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.vectors.Vector2;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class InputSystem extends SystemPackage implements InputProcessor {

    // Internal
    private boolean locked = false;
    private float sensitivity = 0.15f; // TODO: Move sensitivity to Settings

    private Vector2 rotation;
    private Vector3Int input;

    // Base \\

    @Override
    protected void create() {

        // Internal
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

    // Movement
    private boolean W = false;
    private boolean A = false;
    private boolean S = false;
    private boolean D = false;

    private boolean SHIFT = false;
    private boolean SPACE = false;

    // Input System \\

    @Override
    public boolean keyDown(int keycode) {

        if (locked)
            return false;

        switch (keycode) {
            case Input.Keys.W -> W = true;
            case Input.Keys.A -> A = true;
            case Input.Keys.S -> S = true;
            case Input.Keys.D -> D = true;

            case Input.Keys.SHIFT_LEFT -> SHIFT = true;
            case Input.Keys.SPACE -> SPACE = true;
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {

        if (locked)
            return false;

        switch (keycode) {
            case Input.Keys.W -> W = false;
            case Input.Keys.A -> A = false;
            case Input.Keys.S -> S = false;
            case Input.Keys.D -> D = false;

            case Input.Keys.SHIFT_LEFT -> SHIFT = false;
            case Input.Keys.SPACE -> SPACE = false;
        }

        return true;
    }

    // Input \\

    public void lockInput(boolean input) {

        this.locked = input;
        Gdx.input.setCursorCatched(!input);
    }

    public boolean isLocked() {
        return locked;
    }

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
            input.z += 1; // forward
        if (S)
            input.z -= 1; // backward
        if (A)
            input.x -= 1; // left
        if (D)
            input.x += 1; // right

        if (SPACE)
            input.y += 1; // up
        if (SHIFT)
            input.y -= 1; // down
    }

    // Input Processor \\

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
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

    // Accessible \\

    public Vector2 getRotation() {
        return rotation;
    }

    public Vector3Int getInput() {
        return input;
    }
}
