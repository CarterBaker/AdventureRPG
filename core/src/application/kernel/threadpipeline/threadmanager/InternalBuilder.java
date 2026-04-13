package application.kernel.threadpipeline.threadmanager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BuilderPackage;

class InternalBuilder extends BuilderPackage {

    // Build \\

    ThreadHandle build(String threadName, int threadSize) {
        ExecutorService executor = Executors.newFixedThreadPool(
                threadSize,
                new NamedThreadFactory(threadName + "-"));
        ThreadHandle threadHandle = create(ThreadHandle.class);
        threadHandle.constructor(threadName, threadSize, executor);
        return threadHandle;
    }
}