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
     * Lays curves on a take's tracks from what they recorded, with as few
     * keys as keep each curve within its kind's tolerance. A track's samples
     * are read every so often, an angle unwound so it never jumps a whole
     * turn. A curve begins as a key at the first sample and one at the last;
     * each pass lays the keys chosen so far with aligned handles whose slopes
     * follow the samples best — one least-squares solve across the whole
     * curve, so every key bends smoothly through — then makes a key of the
     * sample furthest out in every span that strays past the tolerance and
     * further than either span beside it, until no span strays or the curve
     * holds as many keys as one of its length may. It also frames a curve for
     * a graph: the least and greatest value it reaches across a stretch of
     * the take, keys and handles included.
     */

    // Scratch
    private FloatArrayList fitTimes;
    private DoubleArrayList fitValues;
    private IntArrayList keySamples;
    private DoubleArrayList spanErrors;
    private IntArrayList spanWorst;
    private DoubleArrayList diagonals;
    private DoubleArrayList uppers;
    private DoubleArrayList targets;
    private DoubleArrayList slopes;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.fitTimes = new FloatArrayList();
        this.fitValues = new DoubleArrayList();
        this.keySamples = new IntArrayList();
        this.spanErrors = new DoubleArrayList();
        this.spanWorst = new IntArrayList();
        this.diagonals = new DoubleArrayList();
        this.uppers = new DoubleArrayList();
        this.targets = new DoubleArrayList();
        this.slopes = new DoubleArrayList();
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

        int last = fitTimes.size() - 1;
        int keyLimit = Math.max(EditorSetting.RECORDER_CURVE_MAX_KEYS, (int) Math.ceil(
                (fitTimes.getFloat(last) - fitTimes.getFloat(0)) * EditorSetting.RECORDER_CURVE_MAX_KEYS_PER_SECOND));

        keySamples.clear();
        keySamples.add(0);

        if (last > 0)
            keySamples.add(last);

        do {
            layKeys(curve);
            measureSpans(curve);
        } while (splitSpans(kind.getTolerance(), keyLimit));

        return curve;
    }

    // A key at every sample chosen, each aligned along the slope that keeps the curve nearest the samples about it
    private void layKeys(RecordingCurveStruct curve) {

        int count = keySamples.size();

        solveSlopes();
        curve.clear();

        for (int i = 0; i < count; i++) {

            float time = fitTimes.getFloat(keySamples.getInt(i));
            float before = i > 0 ? time - fitTimes.getFloat(keySamples.getInt(i - 1)) : 0f;
            float after = i < count - 1 ? fitTimes.getFloat(keySamples.getInt(i + 1)) - time : 0f;
            double slope = slopes.getDouble(i);

            curve.appendKey(time, fitValues.getDouble(keySamples.getInt(i)), RecorderKeyMode.ALIGNED,
                    -before / 3f, -slope * before / 3.0, after / 3f, slope * after / 3.0);
        }

        curve.resolveHandles();
    }

    // The slope through every key that keeps the curve nearest every sample between the keys, solved for all keys at
    // once; a slight pull toward the slope through each key's neighbours holds a key with no samples beside it steady
    private void solveSlopes() {

        int count = keySamples.size();

        diagonals.clear();
        uppers.clear();
        targets.clear();
        slopes.clear();

        for (int i = 0; i < count; i++) {
            diagonals.add(0.0);
            uppers.add(0.0);
            targets.add(0.0);
            slopes.add(0.0);
        }

        if (count < 2)
            return;

        for (int span = 0; span < count - 1; span++)
            accumulateSpan(span);

        for (int i = 0; i < count; i++) {

            int from = keySamples.getInt(Math.max(0, i - 1));
            int to = keySamples.getInt(Math.min(count - 1, i + 1));
            double reach = fitTimes.getFloat(to) - fitTimes.getFloat(from);
            double steadiness = EditorSetting.RECORDER_CURVE_FIT_STEADINESS * reach * reach;

            diagonals.set(i, diagonals.getDouble(i) + steadiness);
            targets.set(i, targets.getDouble(i)
                    + steadiness * (fitValues.getDouble(to) - fitValues.getDouble(from)) / reach);
        }

        for (int i = 1; i < count; i++) {

            double factor = uppers.getDouble(i - 1) / diagonals.getDouble(i - 1);

            diagonals.set(i, diagonals.getDouble(i) - factor * uppers.getDouble(i - 1));
            targets.set(i, targets.getDouble(i) - factor * targets.getDouble(i - 1));
        }

        slopes.set(count - 1, targets.getDouble(count - 1) / diagonals.getDouble(count - 1));

        for (int i = count - 2; i >= 0; i--)
            slopes.set(i, (targets.getDouble(i) - uppers.getDouble(i) * slopes.getDouble(i + 1))
                    / diagonals.getDouble(i));
    }

    // How every sample between two keys pulls on the slopes at both, the curve between them a cubic through both keys
    private void accumulateSpan(int span) {

        int from = keySamples.getInt(span);
        int to = keySamples.getInt(span + 1);
        float startTime = fitTimes.getFloat(from);
        double length = fitTimes.getFloat(to) - startTime;
        double startValue = fitValues.getDouble(from);
        double endValue = fitValues.getDouble(to);

        for (int sample = from + 1; sample < to; sample++) {

            double u = (fitTimes.getFloat(sample) - startTime) / length;
            double u2 = u * u;
            double u3 = u2 * u;
            double startPull = length * (u3 - 2.0 * u2 + u);
            double endPull = length * (u3 - u2);
            double rest = fitValues.getDouble(sample)
                    - (2.0 * u3 - 3.0 * u2 + 1.0) * startValue
                    - (3.0 * u2 - 2.0 * u3) * endValue;

            diagonals.set(span, diagonals.getDouble(span) + startPull * startPull);
            uppers.set(span, uppers.getDouble(span) + startPull * endPull);
            diagonals.set(span + 1, diagonals.getDouble(span + 1) + endPull * endPull);
            targets.set(span, targets.getDouble(span) + startPull * rest);
            targets.set(span + 1, targets.getDouble(span + 1) + endPull * rest);
        }
    }

    private void measureSpans(RecordingCurveStruct curve) {

        spanErrors.clear();
        spanWorst.clear();

        for (int span = 0; span < keySamples.size() - 1; span++) {
            spanErrors.add(0.0);
            spanWorst.add(EngineSetting.INDEX_NOT_FOUND);
            measureSpan(curve, span);
        }
    }

    // A key made of the sample furthest out in every span past the tolerance that strays further than either span
    // beside it, from the last span back so the places of those before stay true; false when none was made
    private boolean splitSpans(double tolerance, int keyLimit) {

        boolean split = false;
        int spans = spanErrors.size();

        for (int span = spans - 1; span >= 0 && keySamples.size() < keyLimit; span--) {

            double error = spanErrors.getDouble(span);

            if (spanWorst.getInt(span) == EngineSetting.INDEX_NOT_FOUND || error <= tolerance
                    || span > 0 && spanErrors.getDouble(span - 1) > error
                    || span < spans - 1 && spanErrors.getDouble(span + 1) > error)
                continue;

            keySamples.add(span + 1, spanWorst.getInt(span));
            split = true;
        }

        return split;
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
