package application.bootstrap.soundpipeline.soundcue;

import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class SoundCueHandle extends HandlePackage {

    /*
     * Persistent reference to one loaded sound cue. Every setting delegates
     * to the wrapped SoundCueData. The sounds the cue names are resolved to
     * their IDs the first time it plays — those the game has, the rest left
     * out, so a cue whose sounds are not in the project yet plays nothing —
     * and the variant played last is kept so the next play chooses another.
     * These are registry state SoundCueManager keeps here, not settings.
     */

    private SoundCueData soundCueData;
    private int[] soundIDs;
    private int lastVariant = EngineSetting.INDEX_NOT_FOUND;

    public void constructor(SoundCueData soundCueData) {
        this.soundCueData = soundCueData;
    }

    public SoundCueData getSoundCueData() {
        return soundCueData;
    }

    public String getCueName() {
        return soundCueData.getCueName();
    }

    public int getCueID() {
        return soundCueData.getCueID();
    }

    public String[] getSoundNames() {
        return soundCueData.getSoundNames();
    }

    public float getVolume() {
        return soundCueData.getVolume();
    }

    public float getVolumeVariance() {
        return soundCueData.getVolumeVariance();
    }

    public float getPitch() {
        return soundCueData.getPitch();
    }

    public float getPitchVariance() {
        return soundCueData.getPitchVariance();
    }

    public float getFadeSeconds() {
        return soundCueData.getFadeSeconds();
    }

    // Resolution \\

    public boolean isResolved() {
        return soundIDs != null;
    }

    public void resolve(int[] soundIDs) {
        this.soundIDs = soundIDs;
    }

    public int[] getSoundIDs() {
        return soundIDs;
    }

    public int getLastVariant() {
        return lastVariant;
    }

    public void setLastVariant(int lastVariant) {
        this.lastVariant = lastVariant;
    }
}
