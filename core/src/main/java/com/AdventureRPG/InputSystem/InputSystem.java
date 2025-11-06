package com.AdventureRPG.InputSystem;

import com.AdventureRPG.Core.GameSystem;
import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.Util.Vector3Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

// TODO: Extending my own class means I needed to implement InputProcessor instead of extending official class
// This change needs scrutiny. Overrides for new implimentation are at the bottom under // Input Processor \\
public class InputSystem extends GameSystem implements InputProcessor {

    // Root
    public PlayerSystem playerSystem;

    // Input
    private boolean blockInput = false;

    // Temp
    public Vector3Int movement;
    private float sensitivity = 0.15f; // TODO: Move sensitivity to Settings

    // Base \\

    @Override
    public void init() {

        // Game Manager
        this.playerSystem = rootManager.playerSystem;

        // Temp
        movement = new Vector3Int();
    }

    @Override
    public void start() {

        Gdx.input.setInputProcessor(this);
        block(blockInput);
    }

    @Override
    public void update() {

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

        if (blockInput)
            return;

        float deltaX = Gdx.input.getDeltaX() * sensitivity;
        float deltaY = -Gdx.input.getDeltaY() * sensitivity;

        playerSystem.camera.rotate(deltaX, deltaY);
    }

    private void updateMovement() {

        if (blockInput)
            return;

        movement.set(0, 0, 0);

        if (W)
            movement.z += 1; // forward
        if (S)
            movement.z -= 1; // backward
        if (A)
            movement.x -= 1; // left
        if (D)
            movement.x += 1; // right

        if (SPACE)
            movement.y += 1; // up
        if (SHIFT)
            movement.y -= 1; // down

        if (movement.hasValues())
            playerSystem.position.move(movement);
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
}
