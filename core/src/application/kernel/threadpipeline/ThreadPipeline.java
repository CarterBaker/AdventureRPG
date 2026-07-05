package application.kernel.threadpipeline;

import application.kernel.threadpipeline.threadmanager.ThreadManager;
import engine.root.PipelinePackage;

public class ThreadPipeline extends PipelinePackage {
    /*
     * Registers the internal thread manager which owns all thread handles
     * and provides async execution infrastructure to the rest of the engine.
     */
    @Override
    protected void create() {
        create(ThreadManager.class);
    }
}