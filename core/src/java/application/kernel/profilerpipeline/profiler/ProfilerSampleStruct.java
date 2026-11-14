package application.kernel.profilerpipeline.profiler;

import engine.root.StructPackage;

public class ProfilerSampleStruct extends StructPackage {

    /*
     * One measured quantity, a scope's milliseconds or a counter's value.
     * Values accumulate through the frame and each committed frame lands in a
     * fixed history ring whose running sum gives the rolling average without a
     * scan. A capture keeps its own sum, frame count and peak, so a report
     * reads the whole capture window rather than the rolling one.
     */

    // Identity
    private final String name;

    // Frame
    private float frameValue;
    private int frameCalls;
    private boolean touched;
    private long lastActiveFrame;

    // History
    private final float[] history;
    private int cursor;
    private int count;
    private double runningSum;
    private float lastValue;

    // Capture
    private double captureSum;
    private long captureFrames;
    private long captureCalls;
    private float capturePeak;

    // Constructor \\

    public ProfilerSampleStruct(String name, int historyFrames) {

        // Identity
        this.name = name;

        // History
        this.history = new float[historyFrames];
    }

    // Frame \\

    public void accumulate(float value) {
        this.frameValue += value;
        this.frameCalls++;
        this.touched = true;
    }

    public void set(float value) {
        this.frameValue = value;
        this.frameCalls = 1;
        this.touched = true;
    }

    public void commit(long frameIndex, boolean capturing) {

        if (touched)
            this.lastActiveFrame = frameIndex;

        if (count == history.length)
            runningSum -= history[cursor];
        else
            count++;

        history[cursor] = frameValue;
        runningSum += frameValue;
        cursor = (cursor + 1) % history.length;
        this.lastValue = frameValue;

        if (capturing) {
            captureSum += frameValue;
            captureFrames++;
            captureCalls += frameCalls;
            capturePeak = Math.max(capturePeak, frameValue);
        }

        this.frameValue = 0f;
        this.frameCalls = 0;
        this.touched = false;
    }

    // Capture \\

    public void resetCapture() {
        this.captureSum = 0.0;
        this.captureFrames = 0L;
        this.captureCalls = 0L;
        this.capturePeak = 0f;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public float getFrameValue() {
        return frameValue;
    }

    public float getAverage() {
        return count == 0 ? 0f : (float) (runningSum / count);
    }

    public float getPeak() {

        float peak = 0f;

        for (int i = 0; i < count; i++)
            peak = Math.max(peak, history[i]);

        return peak;
    }

    public float getLast() {
        return lastValue;
    }

    public float getHistory(int framesAgo) {

        if (framesAgo >= count)
            return 0f;

        int index = cursor - 1 - framesAgo;

        return history[index < 0 ? index + history.length : index];
    }

    public int getHistoryCount() {
        return count;
    }

    public long getLastActiveFrame() {
        return lastActiveFrame;
    }

    public float getCaptureAverage() {
        return captureFrames == 0L ? 0f : (float) (captureSum / captureFrames);
    }

    public float getCapturePeak() {
        return capturePeak;
    }

    public long getCaptureCalls() {
        return captureCalls;
    }

    public long getCaptureFrames() {
        return captureFrames;
    }
}
