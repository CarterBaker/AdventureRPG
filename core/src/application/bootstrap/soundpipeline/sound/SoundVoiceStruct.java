package application.bootstrap.soundpipeline.sound;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class SoundVoiceStruct extends StructPackage {

    /*
     * One voice of the sound pool: the audio source it plays through, and
     * while it plays, the handle it answers to, the sound, the window whose
     * systems asked for it, how loud, at what pitch and whether it loops, where
     * in the world it stands when it is placed, when it began and in what order
     * among every voice begun. A voice is reused for every sound that takes its
     * slot, so nothing is allocated per sound played.
     */

    // Source
    private final int source;
    private final int slot;
    private int generation;

    // Playing
    private boolean active;
    private boolean paused;
    private int handle;
    private SoundHandle soundHandle;
    private int ownerWindowID;
    private float volume;
    private float pitch;
    private boolean looping;
    private long startMillis;
    private long serial;

    // Placement
    private boolean spatial;
    private WorldHandle worldHandle;
    private long chunkCoordinate;
    private final Vector3 position;

    // Constructor \\

    public SoundVoiceStruct(int source, int slot) {

        // Source
        this.source = source;
        this.slot = slot;

        // Playing
        this.handle = EngineSetting.INDEX_NOT_FOUND;
        this.ownerWindowID = EngineSetting.INDEX_NOT_FOUND;

        // Placement
        this.position = new Vector3();
    }

    // Management \\

    // The voice takes up a sound under a new handle, the generation of its slot moved on
    public int begin(
            SoundHandle soundHandle,
            int ownerWindowID,
            float volume,
            float pitch,
            boolean looping,
            long startMillis,
            long serial) {

        this.generation = (generation + 1) & EngineSetting.SOUND_VOICE_GENERATION_MASK;

        if (generation == 0)
            this.generation = 1;

        this.handle = (generation << EngineSetting.SOUND_VOICE_SLOT_BITS) | slot;
        this.active = true;
        this.paused = false;
        this.soundHandle = soundHandle;
        this.ownerWindowID = ownerWindowID;
        this.volume = volume;
        this.pitch = pitch;
        this.looping = looping;
        this.startMillis = startMillis;
        this.serial = serial;
        this.spatial = false;
        this.worldHandle = null;
        return handle;
    }

    public void place(WorldHandle worldHandle, long chunkCoordinate, float x, float y, float z) {
        this.spatial = true;
        this.worldHandle = worldHandle;
        this.chunkCoordinate = chunkCoordinate;
        position.set(x, y, z);
    }

    public void end() {
        this.active = false;
        this.paused = false;
        this.soundHandle = null;
        this.worldHandle = null;
    }

    // True while the handle is this voice's and it still plays or waits paused
    public boolean answers(int handle) {
        return active && this.handle == handle;
    }

    // Accessible \\

    public int getSource() {
        return source;
    }

    public int getSlot() {
        return slot;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public int getHandle() {
        return handle;
    }

    public SoundHandle getSoundHandle() {
        return soundHandle;
    }

    public int getOwnerWindowID() {
        return ownerWindowID;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public long getSerial() {
        return serial;
    }

    public boolean isSpatial() {
        return spatial;
    }

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public long getChunkCoordinate() {
        return chunkCoordinate;
    }

    public Vector3 getPosition() {
        return position;
    }
}
