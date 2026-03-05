package com.internal.core.kernel.threadmanager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.internal.core.engine.BuilderPackage;

class InternalBuildSystem extends BuilderPackage {

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