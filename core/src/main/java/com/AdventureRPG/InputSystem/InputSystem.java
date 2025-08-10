package com.AdventureRPG.InputSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.Util.Vector3Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class InputSystem extends InputAdapter {

    // Game Manager
    public final PlayerSystem PlayerSystem;

    // Input
    private boolean blockInput = false;

    // Rotation
    private final float sensitivity = 0.15f; // TODO: Move sensitivity to Settings()

    // Temp
    public final Vector3Int movement;

    // Base \\

    public InputSystem(GameManager gameManager) {

        // Game Manager
        this.PlayerSystem = gameManager.playerSystem;

        // Temp
        movement = new Vector3Int();
    }

    public void Awake() {

    }

    public void Start() {
        Gdx.input.setInputProcessor(this);
        Block(blockInput);
    }

    public void Update() {
        UpdateRotation();
        UpdateMovement();
    }

    public void Render() {

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

    public void Block(boolean block) {

        this.blockInput = block;
        Gdx.input.setCursorCatched(!block);
    }

    public boolean isLocked() {
        return blockInput;
    }

    private void UpdateRotation() {

        if (blockInput)
            return;

        float deltaX = Gdx.input.getDeltaX() * sensitivity;
        float deltaY = -Gdx.input.getDeltaY() * sensitivity;

        PlayerSystem.camera.Rotate(deltaX, deltaY);
    }

    private void UpdateMovement() {

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
            PlayerSystem.position.Move(movement);
    }
}
