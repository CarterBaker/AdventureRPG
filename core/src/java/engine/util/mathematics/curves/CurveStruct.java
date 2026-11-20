package engine.util.mathematics.curves;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CurveStruct extends StructPackage {

    /*
     * One value's curve of keys, as an animation clip's channel or a
     * recording's property holds it: keys in time order, each a moment and
     * a value, with a handle reaching back and one reaching on, kept as
     * offsets of time and value from the key. Between two keys the curve
     * runs a cubic Bezier through both keys and their facing handles, each
     * handle held within its own span so the curve always runs forward in
     * time; a constant key holds its value until the next. Every change
     * resolves the handles again — auto keys along the curve's own slope,
     * flat at a peak or a trough so the curve never overshoots, the end keys
     * as its ends say, linear keys straight at their neighbours — so the
     * curve always reads true. Its revision moves on every change, so what
     * was built from it is known to still hold. A curve with no key reads its
     * rest value. Evaluating never allocates.
     */

    // Curve
    private final CurveEnds ends;
    private final double restValue;

    // Keys
    private final FloatArrayList times;
    private final DoubleArrayList values;
    private final IntArrayList modes;

    // Handles
    private final FloatArrayList inTimes;
    private final DoubleArrayList inValues;
    private final FloatArrayList outTimes;
    private final DoubleArrayList outValues;

    // Revision
    private int revision;

    // Constructor \\

    public CurveStruct(CurveEnds ends) {
        this(ends, 0.0);
    }

    public CurveStruct(CurveEnds ends, double restValue) {

        // Curve
        this.ends = ends;
        this.restValue = restValue;

        // Keys
        this.times = new FloatArrayList();
        this.values = new DoubleArrayList();
        this.modes = new IntArrayList();

        // Handles
        this.inTimes = new FloatArrayList();
        this.inValues = new DoubleArrayList();
        this.outTimes = new FloatArrayList();
        this.outValues = new DoubleArrayList();
    }

    // Keys \\

    // A key at the moment, or the key already there given the value; its index once the handles are resolved
    public int addKey(float time, double value, CurveKeyMode mode) {

        int index = 0;

        while (index < times.size() && times.getFloat(index) < time)
            index++;

        if (index < times.size() && times.getFloat(index) - time < EngineSetting.CURVE_KEY_MIN_GAP_SECONDS) {
            values.set(index, value);
            resolveHandles();
            return index;
        }

        insertKey(index, time, value, mode, 0f, 0.0, 0f, 0.0);
        resolveHandles();
        return index;
    }

    // A key laid after every other just as it is given, as a fit or a file builds a curve; resolveHandles() follows
    public void appendKey(
            float time,
            double value,
            CurveKeyMode mode,
            float inTime,
            double inValue,
            float outTime,
            double outValue) {

        insertKey(times.size(), time, value, mode, inTime, inValue, outTime, outValue);
    }

    private void insertKey(
            int index,
            float time,
            double value,
            CurveKeyMode mode,
            float inTime,
            double inValue,
            float outTime,
            double outValue) {

        revision++;
        times.add(index, time);
        values.add(index, value);
        modes.add(index, mode.ordinal());
        inTimes.add(index, inTime);
        inValues.add(index, inValue);
        outTimes.add(index, outTime);
        outValues.add(index, outValue);
    }

    // Every key let go, as a fit laying the curve again begins
    public void clear() {
        revision++;
        times.clear();
        values.clear();
        modes.clear();
        inTimes.clear();
        inValues.clear();
        outTimes.clear();
        outValues.clear();
    }

    // Every key of another curve laid in place of this one's, modes and handles kept, resolved by this one's ends
    public void copyKeys(CurveStruct source) {

        clear();

        for (int i = 0; i < source.size(); i++)
            appendKey(
                    source.getTime(i),
                    source.getValue(i),
                    source.getMode(i),
                    source.getInTime(i),
                    source.getInValue(i),
                    source.getOutTime(i),
                    source.getOutValue(i));

        resolveHandles();
    }

    public void removeKey(int index) {

        times.removeFloat(index);
        values.removeDouble(index);
        modes.removeInt(index);
        inTimes.removeFloat(index);
        inValues.removeDouble(index);
        outTimes.removeFloat(index);
        outValues.removeDouble(index);
        resolveHandles();
    }

    // The key moved to the moment and value, kept between its neighbours so the keys stay in order
    public void moveKey(int index, float time, double value) {

        float earliest = index > 0
                ? times.getFloat(index - 1) + EngineSetting.CURVE_KEY_MIN_GAP_SECONDS
                : Float.NEGATIVE_INFINITY;
        float latest = index < times.size() - 1
                ? times.getFloat(index + 1) - EngineSetting.CURVE_KEY_MIN_GAP_SECONDS
                : Float.POSITIVE_INFINITY;

        times.set(index, Math.clamp(time, earliest, Math.max(earliest, latest)));
        values.set(index, value);
        resolveHandles();
    }

    public void setValue(int index, double value) {
        values.set(index, value);
        resolveHandles();
    }

    public void setMode(int index, CurveKeyMode mode) {
        modes.set(index, mode.ordinal());
        resolveHandles();
    }

    // The key's handles joined on one line again, as aligned: the incoming one swung round opposite the outgoing,
    // keeping its own length in time, or the outgoing opposite the incoming on the last key
    public void joinHandles(int index) {

        float inTime = inTimes.getFloat(index);
        float outTime = outTimes.getFloat(index);

        modes.set(index, CurveKeyMode.ALIGNED.ordinal());

        if (outTime != 0f && inTime != 0f)
            inValues.set(index, outValues.getDouble(index) / outTime * inTime);

        resolveHandles();
    }

    // One handle dragged to an offset from its key; a key whose handles the curve set becomes aligned, and an
    // aligned key's other handle swings round to stay on one line with it, keeping its own length in time
    public void setHandle(int index, boolean outgoing, float time, double value) {

        if (getMode(index).isResolved())
            modes.set(index, CurveKeyMode.ALIGNED.ordinal());

        float handleTime = outgoing ? Math.max(0f, time) : Math.min(0f, time);

        if (outgoing) {
            outTimes.set(index, handleTime);
            outValues.set(index, value);
        } else {
            inTimes.set(index, handleTime);
            inValues.set(index, value);
        }

        if (getMode(index) == CurveKeyMode.ALIGNED && handleTime != 0f) {

            float otherTime = outgoing ? inTimes.getFloat(index) : outTimes.getFloat(index);

            if (otherTime == 0f)
                otherTime = -handleTime;

            double otherValue = value / handleTime * otherTime;

            if (outgoing) {
                inTimes.set(index, otherTime);
                inValues.set(index, otherValue);
            } else {
                outTimes.set(index, otherTime);
                outValues.set(index, otherValue);
            }
        }

        resolveHandles();
    }

    // Handles \\

    // Every handle the curve sets laid again, and every handle held within the span it reaches across
    public void resolveHandles() {

        revision++;

        int count = times.size();

        for (int i = 0; i < count; i++) {

            float before = i > 0 ? times.getFloat(i) - times.getFloat(i - 1) : 0f;
            float after = i < count - 1 ? times.getFloat(i + 1) - times.getFloat(i) : 0f;

            switch (getMode(i)) {
                case AUTO -> {
                    double slope = resolveAutoSlope(i);
                    setHandles(i, -before / 3f, -slope * before / 3f, after / 3f, slope * after / 3f);
                }
                case LINEAR -> setHandles(
                        i,
                        -before / 3f,
                        i > 0 ? (values.getDouble(i - 1) - values.getDouble(i)) / 3.0 : 0.0,
                        after / 3f,
                        i < count - 1 ? (values.getDouble(i + 1) - values.getDouble(i)) / 3.0 : 0.0);
                case CONSTANT -> setHandles(i, 0f, 0.0, 0f, 0.0);
                case ALIGNED, FREE -> holdHandles(i, before, after);
            }
        }
    }

    private void setHandles(int index, float inTime, double inValue, float outTime, double outValue) {
        inTimes.set(index, inTime);
        inValues.set(index, inValue);
        outTimes.set(index, outTime);
        outValues.set(index, outValue);
    }

    // A handle reaching past its neighbouring key is drawn back along its own line to reach no further
    private void holdHandles(int index, float before, float after) {

        float inTime = inTimes.getFloat(index);
        float outTime = outTimes.getFloat(index);

        if (index > 0 && -inTime > before) {
            inValues.set(index, inValues.getDouble(index) * before / -inTime);
            inTimes.set(index, -before);
        }

        if (index < times.size() - 1 && outTime > after) {
            outValues.set(index, outValues.getDouble(index) * after / outTime);
            outTimes.set(index, after);
        }
    }

    // The slope an auto key takes: between its neighbours one that never overshoots them, and at an end as the
    // curve's ends say
    private double resolveAutoSlope(int index) {

        int last = times.size() - 1;

        if (last < 1)
            return 0.0;

        if (index > 0 && index < last)
            return CurveUtility.resolveSmoothSlope(
                    resolveSecant(index - 1), resolveSecant(index), resolveSpan(index - 1), resolveSpan(index));

        return switch (ends) {
            case OPEN -> resolveSecant(index == 0 ? 0 : last - 1);
            case CLAMPED -> 0.0;
            case CYCLIC -> CurveUtility.resolveSmoothSlope(
                    resolveSecant(last - 1), resolveSecant(0), resolveSpan(last - 1), resolveSpan(0));
        };
    }

    private double resolveSecant(int index) {
        return (values.getDouble(index + 1) - values.getDouble(index)) / resolveSpan(index);
    }

    private double resolveSpan(int index) {
        return times.getFloat(index + 1) - times.getFloat(index);
    }

    // Evaluate \\

    // The curve's value at a moment, the first key's before it and the last key's after it
    public double evaluate(float time) {

        int count = times.size();

        if (count == 0)
            return restValue;

        if (count == 1 || time <= times.getFloat(0))
            return values.getDouble(0);

        if (time >= times.getFloat(count - 1))
            return values.getDouble(count - 1);

        int key = Math.min(CurveUtility.findAtOrBefore(times, time), count - 2);

        if (getMode(key) == CurveKeyMode.CONSTANT)
            return values.getDouble(key);

        float startTime = times.getFloat(key);
        float endTime = times.getFloat(key + 1);
        double startValue = values.getDouble(key);
        double endValue = values.getDouble(key + 1);
        double u = CurveUtility.solveBezier(
                time,
                startTime,
                startTime + outTimes.getFloat(key),
                endTime + inTimes.getFloat(key + 1),
                endTime);

        return CurveUtility.bezier(startValue, startValue + outValues.getDouble(key),
                endValue + inValues.getDouble(key + 1), endValue, u);
    }

    // Accessible \\

    public CurveEnds getEnds() {
        return ends;
    }

    public double getRestValue() {
        return restValue;
    }

    public int getRevision() {
        return revision;
    }

    public int size() {
        return times.size();
    }

    public boolean isEmpty() {
        return times.isEmpty();
    }

    public float getTime(int index) {
        return times.getFloat(index);
    }

    public float getEndTime() {
        return times.getFloat(times.size() - 1);
    }

    public double getValue(int index) {
        return values.getDouble(index);
    }

    public CurveKeyMode getMode(int index) {
        return CurveKeyMode.VALUES[modes.getInt(index)];
    }

    public float getInTime(int index) {
        return inTimes.getFloat(index);
    }

    public double getInValue(int index) {
        return inValues.getDouble(index);
    }

    public float getOutTime(int index) {
        return outTimes.getFloat(index);
    }

    public double getOutValue(int index) {
        return outValues.getDouble(index);
    }

    // The columns themselves, as a file writes and reads them; resolveHandles() follows a read
    public FloatArrayList getTimes() {
        return times;
    }

    public DoubleArrayList getValues() {
        return values;
    }

    public IntArrayList getModes() {
        return modes;
    }

    public FloatArrayList getInTimes() {
        return inTimes;
    }

    public DoubleArrayList getInValues() {
        return inValues;
    }

    public FloatArrayList getOutTimes() {
        return outTimes;
    }

    public DoubleArrayList getOutValues() {
        return outValues;
    }

    // True when every key lies after the one before it, as a curve read from a file must
    public boolean isOrdered() {

        for (int i = 1; i < times.size(); i++)
            if (times.getFloat(i) <= times.getFloat(i - 1))
                return false;

        return true;
    }

    // The key nearest the moment within the tolerance, -1 for none
    public int findKey(float time, float tolerance) {

        int found = -1;
        float nearest = tolerance;

        for (int i = 0; i < times.size(); i++) {

            float distance = Math.abs(times.getFloat(i) - time);

            if (distance <= nearest) {
                found = i;
                nearest = distance;
            }
        }

        return found;
    }
}
