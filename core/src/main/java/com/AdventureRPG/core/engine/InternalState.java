package com.AdventureRPG.core.engine;

public enum InternalState {

    /*
     * InternalState represents the high-level lifecycle state of the game
     * within the engine. The `accessible` flag indicates whether gameplay
     * systems are permitted to run in this state.
     */

    CONSTRUCTOR(),
    FIRST_FRAME(),
    MENU_EXCLUSIVE(),
    GAME_EXCLUSIVE(),
    EXIT();
}