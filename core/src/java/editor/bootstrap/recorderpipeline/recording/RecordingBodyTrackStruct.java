package editor.bootstrap.recorderpipeline.recording;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.entitypipeline.entity.EntityActionHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import editor.bootstrap.recorderpipeline.util.RecordingSampleUtility;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingBodyTrackStruct extends RecordingTrackStruct {

    /*
     * One character in a take — the player or a resident. It knows the
     * template, gender and name it was, the look it wore from each moment that
     * look changed, and, sample by sample, how it stood and moved: its
     * movement state, speeds, facing and ground offset, and the action its
     * hands were in with that action's clip, phase and sequence. Clips are
     * kept once by name and sampled by index. While a take records it, it
     * remembers the last look it saw, which is never saved. Its place, facing,
     * look, speeds and ground offset can each be carried on a curve; its
     * movement state and actions are nodes instead.
     */

    // Identity
    private final String templateName;
    private final String genderName;
    private final String displayName;

    // Looks
    private final FloatArrayList lookTimes;
    private final ObjectArrayList<ArpgObjectStruct> looks;

    // Clips
    private final ObjectArrayList<String> clipNames;

    // Samples
    private final IntArrayList states;
    private final FloatArrayList horizontalSpeeds;
    private final FloatArrayList verticalSpeeds;
    private final FloatArrayList bodyYaws;
    private final FloatArrayList bodyYawRates;
    private final FloatArrayList lookPitches;
    private final FloatArrayList lookYaws;
    private final FloatArrayList groundOffsets;
    private final IntArrayList actions;
    private final IntArrayList clips;
    private final FloatArrayList phases;
    private final IntArrayList sequences;

    // Capture
    private int lookSignature;

    // Constructor \\

    public RecordingBodyTrackStruct(String templateName, String genderName, String displayName) {

        // Identity
        this.templateName = templateName;
        this.genderName = genderName;
        this.displayName = displayName;

        // Looks
        this.lookTimes = new FloatArrayList();
        this.looks = new ObjectArrayList<>();

        // Clips
        this.clipNames = new ObjectArrayList<>();

        // Samples
        this.states = new IntArrayList();
        this.horizontalSpeeds = new FloatArrayList();
        this.verticalSpeeds = new FloatArrayList();
        this.bodyYaws = new FloatArrayList();
        this.bodyYawRates = new FloatArrayList();
        this.lookPitches = new FloatArrayList();
        this.lookYaws = new FloatArrayList();
        this.groundOffsets = new FloatArrayList();
        this.actions = new IntArrayList();
        this.clips = new IntArrayList();
        this.phases = new FloatArrayList();
        this.sequences = new IntArrayList();

        // Capture
        this.lookSignature = EngineSetting.INDEX_NOT_FOUND;
    }

    // Management \\

    public void addSample(float time, EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        EntityActionHandle action = entity.getEntityActionHandle();

        addPlace(time, entity.getWorldPositionStruct().getChunkCoordinate(),
                entity.getWorldPositionStruct().getPosition());

        states.add(state.getMovementState().ordinal());
        horizontalSpeeds.add(state.getHorizontalSpeed());
        verticalSpeeds.add(state.getVerticalSpeed());
        bodyYaws.add(state.getBodyYaw());
        bodyYawRates.add(state.getBodyYawRate());
        lookPitches.add(state.getLookPitch());
        lookYaws.add(state.getLookYaw());
        groundOffsets.add(state.getGroundOffset());
        actions.add(action.getAction().ordinal());
        clips.add(indexClip(action.getClip()));
        phases.add(action.getPhase());
        sequences.add(action.getSequence());
    }

    public void addLook(float time, ArpgObjectStruct lookArpg) {
        lookTimes.add(time);
        looks.add(lookArpg);
    }

    @Override
    public void shiftTimes(float seconds) {
        super.shiftTimes(seconds);
        RecordingSampleUtility.shiftTimes(lookTimes, seconds);
    }

    // The clip's place in this track's clip list, added on first sight, -1 for none
    private int indexClip(AnimationClipHandle clip) {

        if (clip == null)
            return EngineSetting.INDEX_NOT_FOUND;

        String clipName = clip.getClipName();

        for (int i = 0; i < clipNames.size(); i++)
            if (clipNames.get(i).equals(clipName))
                return i;

        clipNames.add(clipName);
        return clipNames.size() - 1;
    }

    // The look in effect at the time — the last taken at or before it, the first before any was
    public int findLook(float time) {

        int look = 0;

        while (look + 1 < lookTimes.size() && lookTimes.getFloat(look + 1) <= time)
            look++;

        return looks.isEmpty() ? EngineSetting.INDEX_NOT_FOUND : look;
    }

    // Curves \\

    @Override
    public String getTrackLabel() {
        return displayName;
    }

    @Override
    protected RecorderCurveProperty[] getCurveProperties() {
        return RecorderCurveProperty.BODY;
    }

    @Override
    public double readCurveSample(int slot, int sample) {
        return switch (getCurveProperty(slot)) {
            case FACING -> bodyYaws.getFloat(sample);
            case LOOK_YAW -> lookYaws.getFloat(sample);
            case LOOK_PITCH -> lookPitches.getFloat(sample);
            case GROUND_SPEED -> horizontalSpeeds.getFloat(sample);
            case RISE_SPEED -> verticalSpeeds.getFloat(sample);
            case TURN_RATE -> bodyYawRates.getFloat(sample);
            case GROUND_OFFSET -> groundOffsets.getFloat(sample);
            default -> readPlaceSample(getCurveProperty(slot), sample);
        };
    }

    // Capture \\

    public int getLookSignature() {
        return lookSignature;
    }

    public void setLookSignature(int lookSignature) {
        this.lookSignature = lookSignature;
    }

    // Accessible \\

    public String getTemplateName() {
        return templateName;
    }

    public String getGenderName() {
        return genderName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public FloatArrayList getLookTimes() {
        return lookTimes;
    }

    public ObjectArrayList<ArpgObjectStruct> getLooks() {
        return looks;
    }

    public ObjectArrayList<String> getClipNames() {
        return clipNames;
    }

    public IntArrayList getStates() {
        return states;
    }

    public FloatArrayList getHorizontalSpeeds() {
        return horizontalSpeeds;
    }

    public FloatArrayList getVerticalSpeeds() {
        return verticalSpeeds;
    }

    public FloatArrayList getBodyYaws() {
        return bodyYaws;
    }

    public FloatArrayList getBodyYawRates() {
        return bodyYawRates;
    }

    public FloatArrayList getLookPitches() {
        return lookPitches;
    }

    public FloatArrayList getLookYaws() {
        return lookYaws;
    }

    public FloatArrayList getGroundOffsets() {
        return groundOffsets;
    }

    public IntArrayList getActions() {
        return actions;
    }

    public IntArrayList getClips() {
        return clips;
    }

    public FloatArrayList getPhases() {
        return phases;
    }

    public IntArrayList getSequences() {
        return sequences;
    }
}
