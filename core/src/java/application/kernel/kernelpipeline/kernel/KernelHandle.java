package application.kernel.kernelpipeline.kernel;

import java.util.concurrent.atomic.AtomicLong;

import engine.root.HandlePackage;

public class KernelHandle extends HandlePackage {

    /*
     * Wraps a resolved kernel's KernelData and counts the calls made to it and
     * the time they took, from any thread, until KernelManager drains both
     * into the profiler once a frame.
     */

    // Internal
    private KernelData data;

    // Load
    private final AtomicLong callCount = new AtomicLong(0L);
    private final AtomicLong busyNanos = new AtomicLong(0L);

    // Constructor \\

    public void constructor(KernelData data) {
        this.data = data;
    }

    // Load \\

    public void recordCall(long callNanos) {
        callCount.incrementAndGet();
        busyNanos.addAndGet(callNanos);
    }

    public long drainCallCount() {
        return callCount.getAndSet(0L);
    }

    public long drainBusyNanos() {
        return busyNanos.getAndSet(0L);
    }

    // Accessible \\

    public KernelData getKernelData() {
        return data;
    }

    public String getKernelName() {
        return data.getKernelName();
    }

    public int getKernelID() {
        return data.getKernelID();
    }

    public String getSymbolName() {
        return data.getSymbolName();
    }

    public long getFunctionAddress() {
        return data.getFunctionAddress();
    }

    public String getCallsCounterName() {
        return data.getCallsCounterName();
    }

    public String getMillisCounterName() {
        return data.getMillisCounterName();
    }
}
