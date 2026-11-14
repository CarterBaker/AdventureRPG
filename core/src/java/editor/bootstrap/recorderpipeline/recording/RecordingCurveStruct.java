package editor.bootstrap.recorderpipeline.recording;

import editor.bootstrap.recorderpipeline.util.RecordingSampleUtility;
import editor.runtime.EditorSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class RecordingCurveStruct extends StructPackage {

    /*
     * One property's curve in a take: keys in time order, each a moment of
     * the take and a value, with a handle reaching back and one reaching on,
     * kept as offsets of time and value from the key. Between two keys the
     * curve runs a cubic Bezier through both keys and their facing handles,
     * each handle held within its own span so the curve always runs forward
     * in time; a constant key holds its value until the next. Every change
     * resolves the handles again — auto keys along the curve's own slope,
     * flat at a peak or a trough so the curve never overshoots, linear keys
     * straight at their neighbours — so the curve always reads true.
     */

    // Curve
    private final RecorderCurveProperty property;
    private final int part;

    // Keys
    private final FloatArrayList times;
    private final DoubleArrayList values;
    private final IntArrayList modes;

    // Handles
    private final FloatArrayList inTimes;
    private final DoubleArrayList inValues;
    private final FloatArrayList outTimes;
    private final DoubleArrayList outValues;

    // Constructor \\

    public RecordingCurveStruct(RecorderCurveProperty property, int part) {

        // Curve
        this.property = property;
        this.part = part;

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
    public int addKey(float time, double value, RecorderKeyMode mode) {

        int index = 0;

        while (index < times.size() && times.getFloat(index) < time)
            index++;

        if (index < times.size() && times.getFloat(index) - time < EditorSetting.RECORDER_KEY_MIN_GAP_SECONDS) {
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
            RecorderKeyMode mode,
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
            RecorderKeyMode mode,
            float inTime,
            double inValue,
            float outTime,
            double outValue) {

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
        times.clear();
        values.clear();
        modes.clear();
        inTimes.clear();
        inValues.clear();
        outTimes.clear();
        outValues.clear();
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
                ? times.getFloat(index - 1) + EditorSetting.RECORDER_KEY_MIN_GAP_SECONDS
                : Float.NEGATIVE_INFINITY;
        float latest = index < times.size() - 1
                ? times.getFloat(index + 1) - EditorSetting.RECORDER_KEY_MIN_GAP_SECONDS
                : Float.POSITIVE_INFINITY;

        times.set(index, Math.clamp(time, earliest, Math.max(earliest, latest)));
        values.set(index, value);
        resolveHandles();
    }

    public void setValue(int index, double value) {
        values.set(index, value);
        resolveHandles();
    }

    public void setMode(int index, RecorderKeyMode mode) {
        modes.set(index, mode.ordinal());
        resolveHandles();
    }

    // One handle dragged to an offset from its key; a key whose handles the curve set becomes aligned, and an
    // aligned key's other handle swings round to stay on one line with it, keeping its own length in time
    public void setHandle(int index, boolean outgoing, float time, double value) {

        if (getMode(index).isResolved())
            modes.set(index, RecorderKeyMode.ALIGNED.ordinal());

        float handleTime = outgoing ? Math.max(0f, time) : Math.min(0f, time);

        if (outgoing) {
            outTimes.set(index, handleTime);
            outValues.set(index, value);
        } else {
            inTimes.set(index, handleTime);
            inValues.set(index, value);
        }

        if (getMode(index) == RecorderKeyMode.ALIGNED && handleTime != 0f) {

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

    // The slope through a key that keeps the curve from overshooting either neighbour: flat where the curve turns,
    // else a mean of the slopes either side weighted by their spans
    private double resolveAutoSlope(int index) {

        int count = times.size();

        if (count < 2)
            return 0.0;

        if (index == 0)
            return resolveSecant(0);

        if (index == count - 1)
            return resolveSecant(count - 2);

        double before = resolveSecant(index - 1);
        double after = resolveSecant(index);

        if (before == 0.0 || after == 0.0 || before > 0.0 != after > 0.0)
            return 0.0;

        double spanBefore = times.getFloat(index) - times.getFloat(index - 1);
        double spanAfter = times.getFloat(index + 1) - times.getFloat(index);
        double weightBefore = 2.0 * spanAfter + spanBefore;
        double weightAfter = spanAfter + 2.0 * spanBefore;

        return (weightBefore + weightAfter) / (weightBefore / before + weightAfter / after);
    }

    private double resolveSecant(int index) {
        return (values.getDouble(index + 1) - values.getDouble(index))
                / (times.getFloat(index + 1) - times.getFloat(index));
    }

    // Evaluate \\

    // The curve's value at a moment of the take, the first key's before it and the last key's after it
    public double evaluate(float time) {

        int count = times.size();

        if (count == 0)
            return 0.0;

        if (count == 1 || time <= times.getFloat(0))
            return values.getDouble(0);

        if (time >= times.getFloat(count - 1))
            return values.getDouble(count - 1);

        int key = Math.min(RecordingSampleUtility.findSample(times, time), count - 2);

        if (getMode(key) == RecorderKeyMode.CONSTANT)
            return values.getDouble(key);

        float startTime = times.getFloat(key);
        float endTime = times.getFloat(key + 1);
        double startValue = values.getDouble(key);
        double endValue = values.getDouble(key + 1);
        double u = solveBezier(
                time,
                startTime,
                startTime + outTimes.getFloat(key),
                endTime + inTimes.getFloat(key + 1),
                endTime);

        return bezier(startValue, startValue + outValues.getDouble(key),
                endValue + inValues.getDouble(key + 1), endValue, u);
    }

    // The share of the way along a segment at which it reaches the moment, its time running forward throughout
    private double solveBezier(float time, double startTime, double startHandle, double endHandle, double endTime) {

        double low = 0.0;
        double high = 1.0;
        double u = (time - startTime) / (endTime - startTime);

        for (int i = 0; i < EditorSetting.RECORDER_CURVE_SOLVE_STEPS; i++) {

            double miss = bezier(startTime, startHandle, endHandle, endTime, u) - time;

            if (Math.abs(miss) < EditorSetting.RECORDER_CURVE_SOLVE_EPSILON)
                return u;

            if (miss > 0.0)
                high = u;
            else
                low = u;

            double slope = bezierSlope(startTime, startHandle, endHandle, endTime, u);
            double next = slope != 0.0 ? u - miss / slope : low;

            u = next > low && next < high ? next : (low + high) / 2.0;
        }

        return u;
    }

    private double bezier(double start, double startHandle, double endHandle, double end, double u) {

        double v = 1.0 - u;

        return v * v * v * start + 3.0 * v * v * u * startHandle + 3.0 * v * u * u * endHandle + u * u * u * end;
    }

    private double bezierSlope(double start, double startHandle, double endHandle, double end, double u) {

        double v = 1.0 - u;

        return 3.0 * v * v * (startHandle - start) + 6.0 * v * u * (endHandle - startHandle)
                + 3.0 * u * u * (end - endHandle);
    }

    // Accessible \\

    public RecorderCurveProperty getProperty() {
        return property;
    }

    public int getPart() {
        return part;
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

    public double getValue(int index) {
        return values.getDouble(index);
    }

    public RecorderKeyMode getMode(int index) {
        return RecorderKeyMode.VALUES[modes.getInt(index)];
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

    // The columns themselves, as a recording file writes and reads them; resolveHandles() follows a read
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
