package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;
import engine.util.mathematics.curves.CurveEnds;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingMorphTrackStruct extends RecordingCurveTrackStruct {

    /*
     * The morph curves of an animation clip open on the timeline, the
     * blendshape weights a facial clip keys: one slot for every morph the
     * clip names, in the order it lists them, each a weight curve named
     * after its morph and running at its ends as the clip loops or holds.
     * It records nothing, so it is made of keys alone and is never laid
     * again from samples.
     */

    // Morphs
    private final ObjectArrayList<String> morphNames;
    private final RecorderCurveProperty[] properties;

    // Samples — morphs record none
    private final FloatArrayList times;

    // Constructor \\

    public RecordingMorphTrackStruct(ObjectArrayList<String> morphNames, CurveEnds ends) {

        // Morphs
        this.morphNames = morphNames;
        this.properties = new RecorderCurveProperty[morphNames.size()];

        // Samples
        this.times = new FloatArrayList();

        for (int slot = 0; slot < properties.length; slot++) {
            properties[slot] = RecorderCurveProperty.MORPH_WEIGHT;
            setCurve(slot, new RecordingCurveStruct(RecorderCurveProperty.MORPH_WEIGHT, slot, ends, 0.0));
        }
    }

    // Curves \\

    @Override
    public String getTrackLabel() {
        return EditorSetting.RECORDER_TRACK_LABEL_MORPHS;
    }

    @Override
    protected RecorderCurveProperty[] getCurveProperties() {
        return properties;
    }

    // Each slot is its own morph, so the part names which
    @Override
    public int getCurvePart(int slot) {
        return slot;
    }

    @Override
    public String getCurveLabel(int slot) {
        return morphNames.get(slot);
    }

    @Override
    public FloatArrayList getTimes() {
        return times;
    }

    @Override
    public double readCurveSample(int slot, int sample) {
        return 0.0;
    }

    @Override
    public boolean isKeyedOnly() {
        return true;
    }

    // Accessible \\

    public String getMorphName(int slot) {
        return morphNames.get(slot);
    }
}
