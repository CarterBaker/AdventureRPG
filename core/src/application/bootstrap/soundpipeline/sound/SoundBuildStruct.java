package application.bootstrap.soundpipeline.sound;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import engine.root.EngineUtility;
import engine.root.StructPackage;

public class SoundBuildStruct extends StructPackage {

    /*
     * One sound on its way into the palette: its name and ID, the file it is
     * decoded from and everything its companion file sets, read on the main
     * thread, then its samples once they are decoded, on the main thread for
     * a sound loaded at once or on a worker for one asked for while the game
     * runs. The main thread uploads the samples and lets this go.
     */

    // Identity
    private final String soundName;
    private final int soundID;
    private final File file;

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

    // Decoding
    private final boolean mono;
    private final float keepSeconds;
    private final float loopCrossfadeSeconds;

    // Loop Points — seconds into the kept samples, both 0 for none
    private final float loopStartSeconds;
    private final float loopEndSeconds;

    // Samples — written once by whichever thread decodes them
    private volatile SoundPcmStruct pcm;
    private Future<?> decodeFuture;

    // Constructor \\

    public SoundBuildStruct(
            String soundName,
            int soundID,
            File file,
            SoundCategory category,
            float volume,
            float pitch,
            boolean loop,
            boolean spatial,
            float referenceDistance,
            float maxDistance,
            float rolloff,
            boolean mono,
            float keepSeconds,
            float loopCrossfadeSeconds,
            float loopStartSeconds,
            float loopEndSeconds) {

        // Identity
        this.soundName = soundName;
        this.soundID = soundID;
        this.file = file;

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

        // Decoding
        this.mono = mono;
        this.keepSeconds = keepSeconds;
        this.loopCrossfadeSeconds = loopCrossfadeSeconds;

        // Loop Points
        this.loopStartSeconds = loopStartSeconds;
        this.loopEndSeconds = loopEndSeconds;
    }

    // Samples \\

    public void setPcm(SoundPcmStruct pcm) {
        this.pcm = pcm;
    }

    public SoundPcmStruct getPcm() {
        return pcm;
    }

    public boolean isDecoded() {
        return pcm != null;
    }

    public void setDecodeFuture(Future<?> decodeFuture) {
        this.decodeFuture = decodeFuture;
    }

    // Main thread — waits for a worker's decode to land, for a sound needed this frame
    public void awaitDecoded() {

        if (pcm != null || decodeFuture == null)
            return;

        try {
            decodeFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            EngineUtility.throwException("Sound \"" + soundName + "\" failed to decode: " + file.getAbsolutePath(), e);
        }
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

    public boolean isMono() {
        return mono;
    }

    public float getKeepSeconds() {
        return keepSeconds;
    }

    public float getLoopCrossfadeSeconds() {
        return loopCrossfadeSeconds;
    }

    public float getLoopStartSeconds() {
        return loopStartSeconds;
    }

    public float getLoopEndSeconds() {
        return loopEndSeconds;
    }
}
