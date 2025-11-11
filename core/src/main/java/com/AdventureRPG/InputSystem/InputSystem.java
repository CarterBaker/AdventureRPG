package com.AdventureRPG.InputSystem;

import com.AdventureRPG.Core.Root.SystemFrame;
import com.AdventureRPG.Core.Util.Vector3Int;
import com.AdventureRPG.PlayerSystem.PlayerCamera;
import com.AdventureRPG.PlayerSystem.PlayerManager;
import com.AdventureRPG.PlayerSystem.PositionManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

// TODO: Extending my own class means I needed to implement InputProcessor instead of extending official class
// This change needs scrutiny. Overrides for new implimentation are at the bottom under // Input Processor \\
public class InputSystem extends SystemFrame implements InputProcessor {

    // Root
    private PlayerManager playerManager;
    private PlayerCamera playerCamera;
    private PositionManager positionManager;

    // Input
    private boolean blockInput = false;

    // Temp
    private Vector3Int movement;
    private float sensitivity = 0.15f; // TODO: Move sensitivity to Settings

    // Base \\

    @Override
    protected void create() {

        // Temp
        movement = new Vector3Int();
    }

    @Override
    protected void init() {

        // Game Manager
        this.playerManager = rootManager.get(PlayerManager.class);
        this.playerCamera = playerManager.get(PlayerCamera.class);
        this.positionManager = playerManager.get(PositionManager.class);
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

        if (blockInput)
            return;

        float deltaX = Gdx.input.getDeltaX() * sensitivity;
        float deltaY = -Gdx.input.getDeltaY() * sensitivity;

        playerCamera.rotate(deltaX, deltaY);
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
            positionManager.move(movement);
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
