package com.AdventureRPG.PlayerSystem;

import com.AdventureRPG.GameManager;

public class InputHandler {

    // Game Manager
    public final GameManager GameManager;

    private boolean allowInput;

    public InputHandler(GameManager GameManager) {
        // Setup Game Systems
        this.GameManager = GameManager;
    }

    public void BlockInput(boolean allowInput) {
        this.allowInput = allowInput;
    }
}
