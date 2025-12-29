package com.AdventureRPG.core.physicspipeline.input;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector3Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;

public class InputSystem extends SystemPackage implements InputProcessor {

    // Input
    private boolean blockInput = false;

    // Temp
    private Vector2 rotation;
    private float sensitivity = 0.15f; // TODO: Move sensitivity to Settings
    private Vector3Int input;

    // Base \\

    @Override
    protected void create() {

        // Temp
        rotation = new Vector2();
        input = new Vector3Int();
    }

    @Override
    protected void start() {

        Gdx.input.setInputProcessor(this);
        block(blockInput);
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

        if (blockInput)
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

        if (blockInput)
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

    public void block(boolean block) {

        this.blockInput = block;
        Gdx.input.setCursorCatched(!block);
    }

    public boolean isLocked() {
        return blockInput;
    }

    private void updateRotation() {

        rotation.set(0, 0);

        if (blockInput)
            return;

        float deltaX = Gdx.input.getDeltaX() * sensitivity;
        float deltaY = -Gdx.input.getDeltaY() * sensitivity;

        rotation.set(deltaX, deltaY);
    }

    private void updateMovement() {

        input.set(0, 0, 0);

        if (blockInput)
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
