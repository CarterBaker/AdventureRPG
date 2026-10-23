package editor.bootstrap.recorderpipeline.recordermanager;

import application.bootstrap.worldpipeline.world.WorldHandle;
import editor.bootstrap.recorderpipeline.recording.RecorderCurveKind;
import editor.bootstrap.recorderpipeline.recording.RecorderKeyMode;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.util.RecordingCurveUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class RecordingCurveBranch extends BranchPackage {

    /*
     * Lays curves on a take's tracks from what they recorded. A track's
     * samples are read every so often, an angle unwound so it never jumps a
     * whole turn, and a curve of auto keys is laid from the first to the
     * last; the sample it strays furthest from becomes a key, and again,
     * until the curve keeps within its kind's tolerance everywhere or holds
     * as many keys as a curve of its length may. Only the spans beside a new key are
     * measured again, so a long take is fitted quickly. It also frames a
     * curve for a graph: the least and greatest value it reaches across a
     * stretch of the take, keys and handles included.
     */

    // Scratch
    private FloatArrayList fitTimes;
    private DoubleArrayList fitValues;
    private IntArrayList keySamples;
    private DoubleArrayList spanErrors;
    private IntArrayList spanWorst;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.fitTimes = new FloatArrayList();
        this.fitValues = new DoubleArrayList();
        this.keySamples = new IntArrayList();
        this.spanErrors = new DoubleArrayList();
        this.spanWorst = new IntArrayList();
    }

    // Fit \\

    // The slot's curve, laid from its samples the first time it is asked for; false when the track holds no samples
    boolean ensureCurve(RecordingCurveTrackStruct track, int slot, WorldHandle worldHandle) {

        if (track.hasCurve(slot))
            return true;

        if (track.isEmpty())
            return false;

        track.setCurve(slot, fitCurve(track, slot, worldHandle));
        return true;
    }

    // A curve laid again from what the slot recorded, whatever was done to it since
    void refitCurve(RecordingCurveTrackStruct track, int slot, WorldHandle worldHandle) {
        if (!track.isEmpty())
            track.setCurve(slot, fitCurve(track, slot, worldHandle));
    }

    private RecordingCurveStruct fitCurve(RecordingCurveTrackStruct track, int slot, WorldHandle worldHandle) {

        RecorderCurveKind kind = track.getCurveProperty(slot).getKind();
        RecordingCurveStruct curve = new RecordingCurveStruct(track.getCurveProperty(slot), track.getCurvePart(slot));

        readSamples(track, slot, worldHandle, kind.isWrapping());

        curve.appendKey(fitTimes.getFloat(0), fitValues.getDouble(0), RecorderKeyMode.AUTO, 0f, 0.0, 0f, 0.0);

        if (fitTimes.size() == 1) {
            curve.resolveHandles();
            return curve;
        }

        int last = fitTimes.size() - 1;

        curve.appendKey(fitTimes.getFloat(last), fitValues.getDouble(last), RecorderKeyMode.AUTO, 0f, 0.0, 0f, 0.0);
        curve.resolveHandles();

        keySamples.clear();
        keySamples.add(0);
        keySamples.add(last);
        spanErrors.clear();
        spanWorst.clear();
        spanErrors.add(0.0);
        spanWorst.add(EngineSetting.INDEX_NOT_FOUND);
        measureSpan(curve, 0);

        int keyLimit = Math.max(EditorSetting.RECORDER_CURVE_MAX_KEYS, (int) Math.ceil(
                (fitTimes.getFloat(last) - fitTimes.getFloat(0)) * EditorSetting.RECORDER_CURVE_MAX_KEYS_PER_SECOND));

        while (curve.size() < keyLimit) {

            int span = findWorstSpan();

            if (span == EngineSetting.INDEX_NOT_FOUND || spanErrors.getDouble(span) <= kind.getTolerance())
                break;

            int sample = spanWorst.getInt(span);

            curve.addKey(fitTimes.getFloat(sample), fitValues.getDouble(sample), RecorderKeyMode.AUTO);
            keySamples.add(span + 1, sample);
            spanErrors.add(span + 1, 0.0);
            spanWorst.add(span + 1, EngineSetting.INDEX_NOT_FOUND);

            for (int i = Math.max(0, span - 2); i <= Math.min(spanErrors.size() - 1, span + 3); i++)
                measureSpan(curve, i);
        }

        return curve;
    }

    // The slot's samples no closer together than the fit reads them, the first and last always among them
    private void readSamples(RecordingCurveTrackStruct track, int slot, WorldHandle worldHandle, boolean wrapping) {

        FloatArrayList times = track.getTimes();
        float lastTime = Float.NEGATIVE_INFINITY;

        fitTimes.clear();
        fitValues.clear();

        for (int sample = 0; sample < times.size(); sample++) {

            float time = times.getFloat(sample);

            if (sample != times.size() - 1 && time - lastTime < EditorSetting.RECORDER_CURVE_FIT_INTERVAL)
                continue;

            double value = RecordingCurveUtility.readSample(track, slot, sample, worldHandle);

            if (wrapping && !fitValues.isEmpty()) {
                double previous = fitValues.getDouble(fitValues.size() - 1);
                value = previous + RecordingCurveUtility.wrapDegrees(value - previous);
            }

            fitTimes.add(time);
            fitValues.add(value);
            lastTime = time;
        }
    }

    private int findWorstSpan() {

        int worst = EngineSetting.INDEX_NOT_FOUND;
        double worstError = 0.0;

        for (int i = 0; i < spanErrors.size(); i++)
            if (spanWorst.getInt(i) != EngineSetting.INDEX_NOT_FOUND && spanErrors.getDouble(i) > worstError) {
                worst = i;
                worstError = spanErrors.getDouble(i);
            }

        return worst;
    }

    // How far the curve strays from the samples between two of its keys, and at which sample furthest
    private void measureSpan(RecordingCurveStruct curve, int span) {

        double worstError = 0.0;
        int worst = EngineSetting.INDEX_NOT_FOUND;

        for (int sample = keySamples.getInt(span) + 1; sample < keySamples.getInt(span + 1); sample++) {

            double error = Math.abs(curve.evaluate(fitTimes.getFloat(sample)) - fitValues.getDouble(sample));

            if (error > worstError) {
                worstError = error;
                worst = sample;
            }
        }

        spanErrors.set(span, worstError);
        spanWorst.set(span, worst);
    }

    // Frame \\

    // The least value the curve reaches across the stretch of the take, its keys and their handles there included
    double resolveMinimum(RecordingCurveStruct curve, float from, float to) {
        return resolveExtreme(curve, from, to, false);
    }

    double resolveMaximum(RecordingCurveStruct curve, float from, float to) {
        return resolveExtreme(curve, from, to, true);
    }

    private double resolveExtreme(RecordingCurveStruct curve, float from, float to, boolean greatest) {

        double extreme = curve.evaluate(from);

        for (int i = 1; i <= EditorSetting.RECORDER_GRAPH_FRAME_SAMPLES; i++)
            extreme = pick(extreme, curve.evaluate(from + (to - from) * i / EditorSetting.RECORDER_GRAPH_FRAME_SAMPLES),
                    greatest);

        for (int i = 0; i < curve.size(); i++) {

            if (curve.getTime(i) < from || curve.getTime(i) > to)
                continue;

            extreme = pick(extreme, curve.getValue(i), greatest);
            extreme = pick(extreme, curve.getValue(i) + curve.getInValue(i), greatest);
            extreme = pick(extreme, curve.getValue(i) + curve.getOutValue(i), greatest);
        }

        return extreme;
    }

    private double pick(double extreme, double value, boolean greatest) {
        return greatest ? Math.max(extreme, value) : Math.min(extreme, value);
    }
}
