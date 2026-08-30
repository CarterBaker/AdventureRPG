package application.kernel.frameratepipeline.frameratemanager;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.ManagerPackage;

public class FrameRateManager extends ManagerPackage {

    /*
     * Owns frame pacing and FPS measurement for the whole engine loop.
     * EnginePackage brackets its UPDATE cycle with beginFrame() and
     * capFrameRate() — time left under the target interval is slept off in
     * bounded chunks, re-measured after each one so the OS scheduler's
     * coarser sleep granularity can never overshoot the deadline unnoticed,
     * then the last stretch is spun out precisely. Measured FPS is a plain
     * one-second rolling count, cheap enough to update unconditionally every
     * frame with no allocation.
     */

    // Pacing
    private long frameStartNanos;
    private long targetIntervalNanos;

    // Measurement
    private long measurementWindowStartNanos;
    private int framesThisWindow;
    private int measuredFPS;

    // Internal \\

    @Override
    protected void create() {
        this.targetIntervalNanos = EngineSetting.NANOS_PER_SECOND / EngineSetting.TARGET_FRAME_RATE;
        this.frameStartNanos = System.nanoTime();
        this.measurementWindowStartNanos = frameStartNanos;
        EngineUtility.assignFrameRateManager(this);
    }

    // Pacing \\

    public void beginFrame() {
        this.frameStartNanos = System.nanoTime();
    }

    public void capFrameRate() {

        long deadline = frameStartNanos + targetIntervalNanos;
        long remaining = deadline - System.nanoTime();

        while (remaining > EngineSetting.FRAME_PACING_SLEEP_THRESHOLD_NANOS) {
            sleepNanos(Math.min(remaining - EngineSetting.FRAME_PACING_SLEEP_THRESHOLD_NANOS,
                    EngineSetting.FRAME_PACING_SLEEP_CHUNK_NANOS));
            remaining = deadline - System.nanoTime();
        }

        while (System.nanoTime() < deadline)
            Thread.onSpinWait();

        recordFrame();
    }

    private void sleepNanos(long nanos) {
        try {
            Thread.sleep(nanos / EngineSetting.NANOS_PER_MILLI, (int) (nanos % EngineSetting.NANOS_PER_MILLI));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Measurement \\

    private void recordFrame() {

        framesThisWindow++;

        long now = System.nanoTime();
        long windowElapsed = now - measurementWindowStartNanos;

        if (windowElapsed < EngineSetting.NANOS_PER_SECOND)
            return;

        this.measuredFPS = framesThisWindow;
        this.framesThisWindow = 0;
        this.measurementWindowStartNanos = now;
    }

    // Accessible \\

    public int getMeasuredFPS() {
        return measuredFPS;
    }

    public long getTargetIntervalNanos() {
        return targetIntervalNanos;
    }
}