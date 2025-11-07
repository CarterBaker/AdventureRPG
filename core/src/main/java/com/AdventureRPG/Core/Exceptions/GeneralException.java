package com.AdventureRPG.Core.Exceptions;

import com.AdventureRPG.Core.InternalState;
import com.AdventureRPG.Core.SystemFrame;

public class GeneralException {

    private GeneralException() {
    } // prevents instantiation

    // Designed to keep the game from creating multiples of the same syste.
    public static class DuplicateSystemFrameDetected extends RuntimeException {
        public DuplicateSystemFrameDetected(SystemFrame system) {
            super("General Exception: Duplicate system detected (" + system.getClass().getName() + ")");
        }
    }

    // Designed to be called if accidentally trying to set game state back to START
    public static class GameStateException extends RuntimeException {
        public GameStateException(InternalState internalState) {
            super("Illegal state transition: transitioning to " + internalState
                    + " is not permitted in the current context");
        }
    }

    // Called when a class was not found in a varag.
    public static class DependencyNotFoundException extends RuntimeException {
        public DependencyNotFoundException(Class<?> type) {
            super("General Exception: Required dependency missing (" + type.getName() + ")");
        }
    }
}
