package com.AdventureRPG.PlayerSystem;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;

import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.GameManager;

public class InputHandler extends InputAdapter {

    // Game Manager
    public final GameManager GameManager;

    private boolean allowInput = true;

    // Input

    // Movement
    private boolean W = false;
    private boolean A = false;
    private boolean S = false;
    private boolean D = false;

    public InputHandler(GameManager GameManager) {
        this.GameManager = GameManager;
        Gdx.input.setInputProcessor(this);
    }

    public void Update() {
        UpdateMovement();
    }

    public void BlockInput(boolean allowInput) {
        this.allowInput = allowInput;
    }

    @Override
    public boolean keyDown(int keycode) {

        if (!allowInput)
            return false;

        switch (keycode) {
            case Input.Keys.W -> W = true;
            case Input.Keys.A -> A = true;
            case Input.Keys.S -> S = true;
            case Input.Keys.D -> D = true;
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {

        if (!allowInput)
            return false;

        switch (keycode) {
            case Input.Keys.W -> W = false;
            case Input.Keys.A -> A = false;
            case Input.Keys.S -> S = false;
            case Input.Keys.D -> D = false;
        }

        return true;
    }

    public void UpdateMovement() {
        
        if (!allowInput)
            return;

        Vector3Int movement = new Vector3Int(0, 0, 0);

        if (W)
            movement = movement.add(new Vector3Int(0, 0, -1));
        if (S)
            movement = movement.add(new Vector3Int(0, 0, 1));
        if (A)
            movement = movement.add(new Vector3Int(-1, 0, 0));
        if (D)
            movement = movement.add(new Vector3Int(1, 0, 0));

        if (!movement.equals(new Vector3Int())) {
            GameManager.Move(movement);
        }
    }

}
