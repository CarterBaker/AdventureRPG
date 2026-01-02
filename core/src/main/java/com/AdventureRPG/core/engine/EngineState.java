package com.AdventureRPG.core.engine;

public enum EngineState {

    /*
     * EngineState represents the high-level lifecycle state of the game
     * within the engine. The `accessible` flag indicates whether gameplay
     * systems are permitted to run in this state.
     */

    BOOTSTRAP,
    CREATE,
    START,
    UPDATE,
    EXIT;
}