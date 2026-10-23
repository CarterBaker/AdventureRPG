package editor.bootstrap.recorderpipeline.recording;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public abstract class RecordingCurveTrackStruct extends StructPackage {

    /*
     * Anything in a take whose values a curve can carry: the camera, every
     * character, item and vehicle, the weather and the clock. It names the
     * properties it holds, one slot each, reads what each held at a sample
     * as it was recorded, and keeps a curve for every slot that has been
     * given one, so playback reads that slot from its curve instead of its
     * samples. Positions are read across chunks by RecordingCurveUtility,
     * which knows the world they lie in.
     */

    // Curves
    private RecordingCurveStruct[] curves;

    // Curves \\

    // The words the Keys tab names this by
    public abstract String getTrackLabel();

    protected abstract RecorderCurveProperty[] getCurveProperties();

    public abstract FloatArrayList getTimes();

    // What the slot held at the sample as it was recorded
    public abstract double readCurveSample(int slot, int sample);

    public int getCurveCount() {
        return getCurveProperties().length;
    }

    public RecorderCurveProperty getCurveProperty(int slot) {
        return getCurveProperties()[slot];
    }

    // Which mast, sail or door the slot is for, -1 for a property held once
    public int getCurvePart(int slot) {
        return EngineSetting.INDEX_NOT_FOUND;
    }

    public String getCurveLabel(int slot) {
        return getCurveProperty(slot).getLabel();
    }

    // The slot holding the property for the part, -1 when this holds none
    public int findCurveSlot(RecorderCurveProperty property, int part) {

        for (int i = 0; i < getCurveCount(); i++)
            if (getCurveProperty(i) == property && getCurvePart(i) == part)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    public int findCurveSlot(RecorderCurveProperty property) {
        return findCurveSlot(property, EngineSetting.INDEX_NOT_FOUND);
    }

    public boolean hasCurve(int slot) {
        return curves != null && slot >= 0 && slot < curves.length && curves[slot] != null;
    }

    public boolean hasCurves() {

        if (curves == null)
            return false;

        for (int i = 0; i < curves.length; i++)
            if (curves[i] != null)
                return true;

        return false;
    }

    public RecordingCurveStruct getCurve(int slot) {
        return hasCurve(slot) ? curves[slot] : null;
    }

    // The slot given the curve, or played from its samples again for null
    public void setCurve(int slot, RecordingCurveStruct curve) {

        if (curves == null)
            curves = new RecordingCurveStruct[getCurveCount()];

        curves[slot] = curve;
    }

    public int size() {
        return getTimes().size();
    }

    public boolean isEmpty() {
        return getTimes().isEmpty();
    }
}
