package application.kernel.threadpipeline.threadmanager;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class NamedThreadFactory implements ThreadFactory {

    private final String baseName;
    private final AtomicInteger count = new AtomicInteger(1);

    NamedThreadFactory(String baseName) {
        this.baseName = baseName;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, baseName + count.getAndIncrement());
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
}