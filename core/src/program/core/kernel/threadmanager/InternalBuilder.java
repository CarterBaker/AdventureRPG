package program.core.kernel.threadmanager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import program.core.engine.BuilderPackage;
import program.core.kernel.thread.ThreadHandle;

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