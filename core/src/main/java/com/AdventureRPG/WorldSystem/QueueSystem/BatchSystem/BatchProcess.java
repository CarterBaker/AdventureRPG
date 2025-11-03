package com.AdventureRPG.WorldSystem.QueueSystem.BatchSystem;

import com.AdventureRPG.WorldSystem.MegaChunk.MegaChunkState;
import com.AdventureRPG.WorldSystem.QueueSystem.QueueBundle;

public enum BatchProcess {

        Add(
                        1,
                        null,
                        null,
                        null),

        Batch(
                        2,
                        null,
                        null,
                        null);

        public final int priority;

        public final MegaChunkState previousState;
        public final MegaChunkState corrospondingState;
        public final MegaChunkState nextState;

        public QueueBundle bundle;

        BatchProcess(
                        int priority,
                        MegaChunkState previousState,
                        MegaChunkState corrospondingState,
                        MegaChunkState nextState) {

                this.priority = priority;

                this.previousState = previousState;
                this.corrospondingState = corrospondingState;
                this.nextState = nextState;
        }

        public boolean process(BatchSystem batchSystem) {

                return switch (this) {

                        default ->
                                batchSystem.processQueue(this);
                };
        }
}
