package com.AdventureRPG.core.engine;

public enum InternalState {

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