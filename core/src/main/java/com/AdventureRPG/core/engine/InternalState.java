package com.AdventureRPG.core.engine;

public enum InternalState {

    /*
     * InternalState represents the high-level lifecycle state of the game
     * within the engine. The `accessible` flag indicates whether gameplay
     * systems are permitted to run in this state.
     */

    CONSTRUCTOR(false),
    FIRST_FRAME(false),
    MENU_EXCLUSIVE(true),
    GAME_EXCLUSIVE(true),
    EXIT(false);

    // Internal
    public final boolean accessible;

    // Internal \\

    InternalState(boolean accessible) {

        // Internal
        this.accessible = accessible;
    }
}