package editor.bootstrap.recorderpipeline.recording;

import editor.bootstrap.recorderpipeline.util.RecordingSampleUtility;
import editor.runtime.EditorSetting;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class RecordingWeatherTrackStruct extends RecordingCurveTrackStruct {

    /*
     * The weather through a take, sample by sample: how far the weather had
     * drifted over the world, in blocks along each axis, and how far on the
     * wind's gusts had run. Together with the season the clock gives, these
     * are all the weather is, so playing them back puts every storm, cloud
     * and gust where it was. Drift is kept to the full double the world reads
     * it at, since it runs on for as long as the world does. Both drifts and
     * the gusts can each be carried on a curve.
     */

    // Samples
    private final FloatArrayList times;
    private final DoubleArrayList flowsX;
    private final DoubleArrayList flowsZ;
    private final FloatArrayList gustTimes;

    // Constructor \\

    public RecordingWeatherTrackStruct() {

        // Samples
        this.times = new FloatArrayList();
        this.flowsX = new DoubleArrayList();
        this.flowsZ = new DoubleArrayList();
        this.gustTimes = new FloatArrayList();
    }

    // Management \\

    public void addSample(float time, double flowX, double flowZ, float gustTime) {
        times.add(time);
        flowsX.add(flowX);
        flowsZ.add(flowZ);
        gustTimes.add(gustTime);
    }

    public void shiftTimes(float seconds) {
        RecordingSampleUtility.shiftTimes(times, seconds);
    }

    // The sample at or before the time, the first before it
    public int findSample(float time) {
        return Math.max(0, RecordingSampleUtility.findSample(times, time));
    }

    public float resolveBlend(int sample, float time) {
        return RecordingSampleUtility.resolveBlend(times, sample, time);
    }

    // Curves \\

    @Override
    public String getTrackLabel() {
        return EditorSetting.RECORDER_TRACK_LABEL_WEATHER;
    }

    @Override
    protected RecorderCurveProperty[] getCurveProperties() {
        return RecorderCurveProperty.WEATHER;
    }

    @Override
    public double readCurveSample(int slot, int sample) {
        return switch (getCurveProperty(slot)) {
            case DRIFT_X -> flowsX.getDouble(sample);
            case DRIFT_Z -> flowsZ.getDouble(sample);
            case GUST -> gustTimes.getFloat(sample);
            default -> throwException("The weather holds no " + getCurveProperty(slot).getLabel() + ".");
        };
    }

    // Accessible \\

    @Override
    public FloatArrayList getTimes() {
        return times;
    }

    public DoubleArrayList getFlowsX() {
        return flowsX;
    }

    public DoubleArrayList getFlowsZ() {
        return flowsZ;
    }

    public FloatArrayList getGustTimes() {
        return gustTimes;
    }
}
