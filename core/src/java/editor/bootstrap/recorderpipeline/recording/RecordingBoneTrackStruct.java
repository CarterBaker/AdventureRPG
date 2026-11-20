package editor.bootstrap.recorderpipeline.recording;

import application.bootstrap.animationpipeline.animation.AnimationChannel;
import engine.root.EngineSetting;
import engine.util.mathematics.curves.CurveEnds;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class RecordingBoneTrackStruct extends RecordingCurveTrackStruct {

    /*
     * One bone of an animation clip open on the timeline: its name, the
     * joined part whose tracks key it (null for the clip's own), the nearest
     * bone above it in the rig that the same layer holds (null for none, so
     * the Layers tab nests the layer's bones as the rig does), and a curve
     * for every axis of every channel the clip keys for a bone, in the
     * clip's own slot order, each running at its ends as the clip loops or
     * holds and reading its channel's default while it holds no key. It
     * records nothing, so it is made of keys alone and is never laid again
     * from samples.
     */

    // Identity
    private final String boneName;
    private final String partName;
    private final String parentName;

    // Samples — a bone records none
    private final FloatArrayList times;

    // Constructor \\

    public RecordingBoneTrackStruct(String boneName, String partName, String parentName, CurveEnds ends) {

        // Identity
        this.boneName = boneName;
        this.partName = partName;
        this.parentName = parentName;

        // Samples
        this.times = new FloatArrayList();

        for (int slot = 0; slot < AnimationChannel.SLOT_COUNT; slot++)
            setCurve(slot, new RecordingCurveStruct(
                    RecorderCurveProperty.BONE[slot],
                    EngineSetting.INDEX_NOT_FOUND,
                    ends,
                    AnimationChannel.fromSlot(slot).getDefaultValue()));
    }

    // Curves \\

    @Override
    public String getTrackLabel() {
        return boneName;
    }

    @Override
    protected RecorderCurveProperty[] getCurveProperties() {
        return RecorderCurveProperty.BONE;
    }

    @Override
    public FloatArrayList getTimes() {
        return times;
    }

    @Override
    public double readCurveSample(int slot, int sample) {
        return AnimationChannel.fromSlot(slot).getDefaultValue();
    }

    @Override
    public boolean isKeyedOnly() {
        return true;
    }

    // True while any of its curves holds a key, so the clip keys the bone
    public boolean isKeyed() {

        for (int slot = 0; slot < getCurveCount(); slot++)
            if (!getCurve(slot).isEmpty())
                return true;

        return false;
    }

    // Accessible \\

    public String getBoneName() {
        return boneName;
    }

    public String getPartName() {
        return partName;
    }

    public String getParentName() {
        return parentName;
    }
}
