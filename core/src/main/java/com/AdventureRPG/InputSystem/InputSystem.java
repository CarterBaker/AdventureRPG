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

    private boolean blockInput = false;

    // Input \\

    // Rotation
    private final float sensitivity = 0.15f; // This will need to be added to settings

    // Movement
    private boolean W = false;
    private boolean A = false;
    private boolean S = false;
    private boolean D = false;

    private boolean SHIFT = false;
    private boolean SPACE = false;

    // Base \\

    public InputSystem(GameManager GameManager) {
        this.PlayerSystem = GameManager.PlayerSystem;
    }

    public void Start() {
        Gdx.input.setInputProcessor(this);
        Block(blockInput);
    }

    public void Update() {
        UpdateRotation();
        UpdateMovement();
    }

    // Input System \\

    public void Block(boolean block) {

        this.blockInput = block;
        Gdx.input.setCursorCatched(!block);
    }

    public boolean isLocked() {
        return blockInput;
    }

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

    private void UpdateRotation() {

        if (blockInput)
            return;

        float deltaX = -Gdx.input.getDeltaX() * sensitivity;
        float deltaY = -Gdx.input.getDeltaY() * sensitivity;

        PlayerSystem.camera.rotate(deltaX, deltaY);
    }

    private void UpdateMovement() {

        if (blockInput)
            return;

        Vector3Int movement = new Vector3Int(0, 0, 0);

        if (W)
            movement = movement.add(new Vector3Int(0, 0, 1));
        if (S)
            movement = movement.add(new Vector3Int(0, 0, -1));
        if (A)
            movement = movement.add(new Vector3Int(1, 0, 0));
        if (D)
            movement = movement.add(new Vector3Int(-1, 0, 0));

        if (SHIFT)
            movement = movement.add(new Vector3Int(0, -1, 0));
        if (SPACE)
            movement = movement.add(new Vector3Int(0, 1, 0));

        if (!movement.equals(new Vector3Int())) {
            PlayerSystem.Move(movement);
        }
    }
}
