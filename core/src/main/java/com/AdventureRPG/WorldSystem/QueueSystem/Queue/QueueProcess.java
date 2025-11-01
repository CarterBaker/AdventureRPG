package com.AdventureRPG.WorldSystem.QueueSystem.Queue;

import com.AdventureRPG.WorldSystem.Chunks.ChunkState;
import com.AdventureRPG.WorldSystem.QueueSystem.QueueSystem;

public enum QueueProcess {

        Unload(
                        6,
                        null,
                        null,
                        null),
        Load(
                        5,
                        null,
                        null,
                        ChunkState.NEEDS_GENERATION_DATA),
        Generate(
                        4,
                        null,
                        ChunkState.NEEDS_GENERATION_DATA,
                        ChunkState.NEEDS_ASSESSMENT_DATA),
        Assessment(
                        3,
                        ChunkState.NEEDS_GENERATION_DATA,
                        ChunkState.NEEDS_ASSESSMENT_DATA,
                        ChunkState.NEEDS_BUILD_DATA),
        Build(
                        2,
                        ChunkState.NEEDS_ASSESSMENT_DATA,
                        ChunkState.NEEDS_BUILD_DATA,
                        ChunkState.NEEDS_BATCH_DATA),
        Batch(
                        1,
                        ChunkState.NEEDS_BUILD_DATA,
                        ChunkState.NEEDS_BATCH_DATA,
                        ChunkState.FINALIZED);

        public final int priority;

        public final ChunkState previousState;
        public final ChunkState corrospondingState;
        public final ChunkState nextState;

        public QueueBundle bundle;

        QueueProcess(
                        int priority,
                        ChunkState previousState,
                        ChunkState corrospondingState,
                        ChunkState nextState) {

                this.priority = priority;

                this.previousState = previousState;
                this.corrospondingState = corrospondingState;
                this.nextState = nextState;
        }

        public boolean process(QueueSystem gridSystem) {

                return switch (this) {

                        case Unload -> gridSystem.unloadQueue();

                        default -> gridSystem.processQueue(this);
                };
        }
}
