package com.AdventureRPG.WorldPipeline.batchsystem;

import com.AdventureRPG.WorldPipeline.queuesystem.QueueBundle;

public enum BatchProcess {

        Creation(4),

        Assessment(3),

        Batch(2),

        Render(1);

        public final int priority;

        public QueueBundle bundle;

        BatchProcess(int priority) {

                this.priority = priority;
        }

        public boolean process(BatchSystem batchSystem) {

                return switch (this) {

                        default ->
                                batchSystem.processQueue(this);
                };
        }
}
