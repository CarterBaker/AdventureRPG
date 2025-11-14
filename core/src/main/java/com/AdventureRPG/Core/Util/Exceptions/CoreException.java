package com.AdventureRPG.Core.Util.Exceptions;

import com.AdventureRPG.Core.Bootstrap.InternalProcess;
import com.AdventureRPG.Core.Bootstrap.InternalState;
import com.AdventureRPG.Core.Bootstrap.SystemFrame;

public class CoreException {

    private CoreException() {
    } // prevents instantiation

    // Designed to keep the game from creating multiples of the same syste.
    public static class DuplicateSystemFrameDetected extends RuntimeException {
        public DuplicateSystemFrameDetected(SystemFrame system) {
            super("Core Exception: Duplicate system detected (" + system.getClass().getName() + ")");
        }
    }

    // Designed to keep the game from creating multiples of the same syste.
    public static class OutOfOrderException extends RuntimeException {
        public OutOfOrderException(InternalProcess internalProcess) {
            super("Core Exception: Error in process: " + internalProcess + ", illegal method called");

        }
    }

    // Designed to be called if accidentally trying to set game state back to START
    public static class GameStateException extends RuntimeException {
        public GameStateException(InternalState internalState) {
            super("Core Exception: transitioning to " + internalState + " is not permitted in the current context");
        }
    }

    // Called when a class was not found in a varag.
    public static class DependencyNotFoundException extends RuntimeException {
        public DependencyNotFoundException(Class<?> type) {
            super("Core Exception: Required dependency missing (" + type.getName() + ")");
        }
    }
}
