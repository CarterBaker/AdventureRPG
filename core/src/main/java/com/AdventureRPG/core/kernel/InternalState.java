package com.AdventureRPG.core.kernel;

public enum InternalState {

    CONSTRUCTOR(false),
    FIRST_FRAME(false),
    MENU_EXCLUSIVE(true),
    GAME_EXCLUSIVE(true),
    EXIT(false);

    public final boolean accessible;

    InternalState(boolean accessible) {
        this.accessible = accessible;
    }
}