package application.bootstrap.musicpipeline.noteset;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class NoteRegionStruct extends StructPackage {

    /*
     * One region of a note set: the sample that plays for a range of pitches
     * and velocities, the pitch it was recorded at, which turn of its round
     * robin it takes, how far it is tuned in cents, how loud it plays in
     * decibels (kept as the gain it scales by) and how it loops. The sample is
     * named here and its sound ID resolved the first time the note set plays,
     * when its samples load; everything else is fixed once built.
     */

    // Sample
    private final String soundName;
    private int soundID;

    // Range
    private final int root;
    private final int low;
    private final int high;
    private final int velocityLow;
    private final int velocityHigh;

    // Round Robin
    private final int roundRobin;
    private final int roundRobinCount;

    // Playback
    private final float tune;
    private final float volume;
    private final float gain;
    private final NoteLoopMode loopMode;

    // Constructor \\

    public NoteRegionStruct(
            String soundName,
            int root,
            int low,
            int high,
            int velocityLow,
            int velocityHigh,
            int roundRobin,
            int roundRobinCount,
            float tune,
            float volume,
            float gain,
            NoteLoopMode loopMode) {

        // Sample
        this.soundName = soundName;
        this.soundID = EngineSetting.INDEX_NOT_FOUND;

        // Range
        this.root = root;
        this.low = low;
        this.high = high;
        this.velocityLow = velocityLow;
        this.velocityHigh = velocityHigh;

        // Round Robin
        this.roundRobin = roundRobin;
        this.roundRobinCount = roundRobinCount;

        // Playback
        this.tune = tune;
        this.volume = volume;
        this.gain = gain;
        this.loopMode = loopMode;
    }

    // Sample \\

    // The sample's sound ID, resolved once when the note set's samples are asked for
    public void resolveSound(int soundID) {
        this.soundID = soundID;
    }

    // Match \\

    // Whether the region sounds for a velocity on the given turn of its round robin
    public boolean matches(int velocity, int roundRobinTurn) {
        return velocity >= velocityLow
                && velocity <= velocityHigh
                && roundRobinTurn % roundRobinCount == roundRobin;
    }

    // Accessible \\

    public String getSoundName() {
        return soundName;
    }

    public int getSoundID() {
        return soundID;
    }

    public int getRoot() {
        return root;
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }

    public int getVelocityLow() {
        return velocityLow;
    }

    public int getVelocityHigh() {
        return velocityHigh;
    }

    public int getRoundRobin() {
        return roundRobin;
    }

    public int getRoundRobinCount() {
        return roundRobinCount;
    }

    public float getTune() {
        return tune;
    }

    public float getVolume() {
        return volume;
    }

    public float getGain() {
        return gain;
    }

    public NoteLoopMode getLoopMode() {
        return loopMode;
    }
}
