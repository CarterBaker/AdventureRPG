package application.bootstrap.soundpipeline.soundcue;

import engine.root.DataPackage;

public class SoundCueData extends DataPackage {

    /*
     * Immutable settings of one sound cue — what the game plays for one kind
     * of moment: the sounds it chooses among by name, how loud and at what
     * pitch, how far each play strays from both at random so a repeated
     * moment never sounds the same twice, and how quickly a cue held as a
     * loop fades in and out. SoundCueHandle wraps this class and delegates
     * every accessor to it.
     */

    // Identity
    private final String cueName;
    private final int cueID;

    // Sounds
    private final String[] soundNames;

    // Playback
    private final float volume;
    private final float volumeVariance;
    private final float pitch;
    private final float pitchVariance;
    private final float fadeSeconds;

    // Constructor \\

    public SoundCueData(
            String cueName,
            int cueID,
            String[] soundNames,
            float volume,
            float volumeVariance,
            float pitch,
            float pitchVariance,
            float fadeSeconds) {

        // Identity
        this.cueName = cueName;
        this.cueID = cueID;

        // Sounds
        this.soundNames = soundNames;

        // Playback
        this.volume = volume;
        this.volumeVariance = volumeVariance;
        this.pitch = pitch;
        this.pitchVariance = pitchVariance;
        this.fadeSeconds = fadeSeconds;
    }

    // Accessible \\

    public String getCueName() {
        return cueName;
    }

    public int getCueID() {
        return cueID;
    }

    public String[] getSoundNames() {
        return soundNames;
    }

    public float getVolume() {
        return volume;
    }

    public float getVolumeVariance() {
        return volumeVariance;
    }

    public float getPitch() {
        return pitch;
    }

    public float getPitchVariance() {
        return pitchVariance;
    }

    public float getFadeSeconds() {
        return fadeSeconds;
    }
}
