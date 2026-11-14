package editor.bootstrap.recorderpipeline.recording;

import editor.bootstrap.recorderpipeline.util.RecordingSampleUtility;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class RecordingClockTrackStruct extends RecordingCurveTrackStruct {

    /*
     * The world's clock through a take, sample by sample, as the
     * milliseconds elapsed since its epoch. Wherever the clock ran on further
     * than the real time between two samples — a time command moved it — the
     * jump is kept with how far it went, so playback can sweep through the
     * skipped span instead of cutting to it. Given a curve, the clock follows
     * that instead, so the time of day and the seasons can be swept through
     * however the curve bends.
     */

    // Samples
    private final FloatArrayList times;
    private final LongArrayList elapsedMillis;

    // Jumps
    private final FloatArrayList jumpTimes;
    private final LongArrayList jumpMillis;

    // Constructor \\

    public RecordingClockTrackStruct() {

        // Samples
        this.times = new FloatArrayList();
        this.elapsedMillis = new LongArrayList();

        // Jumps
        this.jumpTimes = new FloatArrayList();
        this.jumpMillis = new LongArrayList();
    }

    // Management \\

    public void addSample(float time, long elapsed) {

        if (!times.isEmpty()) {

            float previousTime = times.getFloat(times.size() - 1);
            long previousElapsed = elapsedMillis.getLong(elapsedMillis.size() - 1);
            long jump = elapsed - previousElapsed - Math.round((time - previousTime) * EngineSetting.MILLIS_PER_SECOND);

            if (Math.abs(jump) >= EditorSetting.RECORDER_CLOCK_JUMP_MILLIS)
                addJump(time, jump);
        }

        times.add(time);
        elapsedMillis.add(elapsed);
    }

    public void addJump(float time, long millis) {
        jumpTimes.add(time);
        jumpMillis.add(millis);
    }

    public void shiftTimes(float seconds) {
        RecordingSampleUtility.shiftTimes(times, seconds);
        RecordingSampleUtility.shiftTimes(jumpTimes, seconds);
    }

    // The clock at the time: along its curve when it has one, else blended between samples with every jump within
    // the ramp before it eased in
    public long evaluate(float time, float rampSeconds) {

        int slot = findCurveSlot(RecorderCurveProperty.TIME);

        if (hasCurve(slot))
            return Math.round(getCurve(slot).evaluate(time));

        return Math.round(resolveRecorded(time) - resolveRampedJumps(time, rampSeconds));
    }

    // The clock as recorded at the time, a jump landing whole on the sample it was taken at
    private double resolveRecorded(float time) {

        int sample = RecordingSampleUtility.findSample(times, time);

        if (sample < 0)
            return elapsedMillis.getLong(0);

        if (sample >= times.size() - 1)
            return elapsedMillis.getLong(times.size() - 1);

        float blend = RecordingSampleUtility.resolveBlend(times, sample, time);
        long from = elapsedMillis.getLong(sample);
        long to = elapsedMillis.getLong(sample + 1);
        long jump = findJump(times.getFloat(sample + 1));

        return from + (to - jump - from) * (double) blend;
    }

    // The jump that landed on the sample at this time, none when the clock ran on as real time did
    private long findJump(float time) {

        for (int i = 0; i < jumpTimes.size(); i++)
            if (jumpTimes.getFloat(i) == time)
                return jumpMillis.getLong(i);

        return 0L;
    }

    // How much of every jump still easing in at the time is yet to be swept through
    private double resolveRampedJumps(float time, float rampSeconds) {

        if (rampSeconds <= 0f)
            return 0.0;

        double remaining = 0.0;

        for (int i = 0; i < jumpTimes.size(); i++) {

            float since = time - jumpTimes.getFloat(i);

            if (since < 0f || since >= rampSeconds)
                continue;

            remaining += jumpMillis.getLong(i) * (1.0 - RecordingSampleUtility.smoothStep(since / rampSeconds));
        }

        return remaining;
    }

    // Curves \\

    @Override
    public String getTrackLabel() {
        return EditorSetting.RECORDER_TRACK_LABEL_CLOCK;
    }

    @Override
    protected RecorderCurveProperty[] getCurveProperties() {
        return RecorderCurveProperty.CLOCK;
    }

    @Override
    public double readCurveSample(int slot, int sample) {
        return elapsedMillis.getLong(sample);
    }

    // Accessible \\

    @Override
    public FloatArrayList getTimes() {
        return times;
    }

    public LongArrayList getElapsedMillis() {
        return elapsedMillis;
    }

    public FloatArrayList getJumpTimes() {
        return jumpTimes;
    }

    public LongArrayList getJumpMillis() {
        return jumpMillis;
    }
}
