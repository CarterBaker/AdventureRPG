package application.bootstrap.musicpipeline.song;

import engine.root.StructPackage;

public class SongLaneStruct extends StructPackage {

    /*
     * Immutable lane of a song, one row of its arrangement as a DAW's track
     * is: the name it is shown under, how loud everything on it plays, 0 to
     * 1, where it sits from left (-1) to right (1), whether it is muted and
     * whether it is soloed — while any lane of a song is soloed, only soloed
     * lanes play.
     */

    // Identity
    private final String name;

    // Mix
    private final float volume;
    private final float pan;
    private final boolean muted;
    private final boolean solo;

    // Constructor \\

    public SongLaneStruct(String name, float volume, float pan, boolean muted, boolean solo) {

        // Identity
        this.name = name;

        // Mix
        this.volume = volume;
        this.pan = pan;
        this.muted = muted;
        this.solo = solo;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public float getVolume() {
        return volume;
    }

    public float getPan() {
        return pan;
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean isSolo() {
        return solo;
    }
}
