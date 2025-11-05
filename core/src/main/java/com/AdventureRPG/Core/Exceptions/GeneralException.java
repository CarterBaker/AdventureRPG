package com.AdventureRPG.Core.Exceptions;

import com.AdventureRPG.Core.GameState;

public class GeneralException {

    private GeneralException() {
    } // prevents instantiation

    // Designed to be called if accidentally trying to set game state back to START
    public static class GameStateException extends RuntimeException {
        public GameStateException(GameState gameState) {
            super("General Exception: Game State " + gameState.toString() + " was called illegally!");
        }
    }
}
