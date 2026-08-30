package application.kernel.frameratepipeline.frameratemanager;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.ManagerPackage;

public class FrameRateManager extends ManagerPackage {

    /*
     * Owns frame pacing and FPS measurement for the whole engine loop.
     * EnginePackage brackets its UPDATE cycle with beginFrame() and
     * capFrameRate() — any time left under the target interval is mostly
     * slept off and precisely spun out for the last couple of milliseconds,
     * giving every downstream system a stable, predictable cadence
     * regardless of vsync state or display refresh rate. Measured FPS is a
     * plain one-second rolling count, cheap enough to update
     * unconditionally every frame with no allocation.
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

        if (remaining > EngineSetting.FRAME_PACING_SLEEP_THRESHOLD_NANOS)
            sleepNanos(remaining - EngineSetting.FRAME_PACING_SLEEP_THRESHOLD_NANOS);

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