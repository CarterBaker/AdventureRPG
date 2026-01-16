package com.internal.core.kernel.InternalThreadManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.ThreadHandle;

class InternalBuildSystem extends SystemPackage {

    // Build \\

    ThreadHandle buildThreadHandle(String threadName, int threadSize) {

        ExecutorService executor = Executors.newFixedThreadPool(
                threadSize,
                new NamedThreadFactory(threadName + "-"));

        ThreadHandle threadHandle = create(ThreadHandle.class);
        threadHandle.constructor(threadName, threadSize, executor);

        return threadHandle;
    }

    // Thread Factory \\

    private static class NamedThreadFactory implements java.util.concurrent.ThreadFactory {

        private final String baseName;
        private int count = 1;

        NamedThreadFactory(String baseName) {
            this.baseName = baseName;
        }

        @Override
        public Thread newThread(Runnable r) {

            Thread t = new Thread(r, baseName + count++);
            t.setPriority(Thread.NORM_PRIORITY);

            return t;
        }
    }
}
