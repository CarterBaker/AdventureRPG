package application.bootstrap.soundpipeline.sound;

import java.io.File;

import engine.root.DataPackage;

public class SoundData extends DataPackage {

    /*
     * Immutable settings of one loaded sound: its name and ID, the file it was
     * decoded from, the audio buffer its samples were uploaded to, and how it
     * plays — its category, volume, pitch, whether it loops, and whether it is
     * placed in the world, with the distance it is heard at full volume, the
     * distance past which it grows no quieter and how quickly it fades between.
     * The samples themselves live only in the buffer; how many there are, at
     * what rate and over how many channels is kept here, with how much of the
     * file was kept and across how long its tail was faded into its head, so
     * a tool decoding it again hears the same samples. SoundHandle wraps
     * this class and delegates every accessor to it.
     */

    // Identity
    private final String soundName;
    private final int soundID;
    private final File file;

    // Buffer
    private final int buffer;
    private final int channels;
    private final int sampleRate;
    private final int frameCount;
    private final float keepSeconds;
    private final float loopCrossfadeSeconds;

    // Playback
    private final SoundCategory category;
    private final float volume;
    private final float pitch;
    private final boolean loop;

    // Placement
    private final boolean spatial;
    private final float referenceDistance;
    private final float maxDistance;
    private final float rolloff;

    // Constructor \\

    public SoundData(
            String soundName,
            int soundID,
            File file,
            int buffer,
            int channels,
            int sampleRate,
            int frameCount,
            float keepSeconds,
            float loopCrossfadeSeconds,
            SoundCategory category,
            float volume,
            float pitch,
            boolean loop,
            boolean spatial,
            float referenceDistance,
            float maxDistance,
            float rolloff) {

        // Identity
        this.soundName = soundName;
        this.soundID = soundID;
        this.file = file;

        // Buffer
        this.buffer = buffer;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.frameCount = frameCount;
        this.keepSeconds = keepSeconds;
        this.loopCrossfadeSeconds = loopCrossfadeSeconds;

        // Playback
        this.category = category;
        this.volume = volume;
        this.pitch = pitch;
        this.loop = loop;

        // Placement
        this.spatial = spatial;
        this.referenceDistance = referenceDistance;
        this.maxDistance = maxDistance;
        this.rolloff = rolloff;
    }

    // Accessible \\

    public String getSoundName() {
        return soundName;
    }

    public int getSoundID() {
        return soundID;
    }

    public File getFile() {
        return file;
    }

    public int getBuffer() {
        return buffer;
    }

    public int getChannels() {
        return channels;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public float getKeepSeconds() {
        return keepSeconds;
    }

    public float getLoopCrossfadeSeconds() {
        return loopCrossfadeSeconds;
    }

    public SoundCategory getCategory() {
        return category;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isLoop() {
        return loop;
    }

    public boolean isSpatial() {
        return spatial;
    }

    public float getReferenceDistance() {
        return referenceDistance;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public float getRolloff() {
        return rolloff;
    }
}
