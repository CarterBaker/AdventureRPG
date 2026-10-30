package application.bootstrap.soundpipeline.sound;

import java.io.File;

import engine.root.HandlePackage;

public class SoundHandle extends HandlePackage {

    /*
     * Persistent reference to one loaded sound. Every accessor delegates to
     * the wrapped SoundData. Its length is the time its samples take at their
     * own rate; it plays for that long divided by its pitch.
     */

    private SoundData soundData;

    public void constructor(SoundData soundData) {
        this.soundData = soundData;
    }

    public SoundData getSoundData() {
        return soundData;
    }

    public String getSoundName() {
        return soundData.getSoundName();
    }

    public int getSoundID() {
        return soundData.getSoundID();
    }

    public File getFile() {
        return soundData.getFile();
    }

    public int getBuffer() {
        return soundData.getBuffer();
    }

    public int getChannels() {
        return soundData.getChannels();
    }

    public int getSampleRate() {
        return soundData.getSampleRate();
    }

    public int getFrameCount() {
        return soundData.getFrameCount();
    }

    public float getKeepSeconds() {
        return soundData.getKeepSeconds();
    }

    public float getLoopCrossfadeSeconds() {
        return soundData.getLoopCrossfadeSeconds();
    }

    public SoundCategory getCategory() {
        return soundData.getCategory();
    }

    public float getVolume() {
        return soundData.getVolume();
    }

    public float getPitch() {
        return soundData.getPitch();
    }

    public boolean isLoop() {
        return soundData.isLoop();
    }

    public boolean isSpatial() {
        return soundData.isSpatial();
    }

    public float getReferenceDistance() {
        return soundData.getReferenceDistance();
    }

    public float getMaxDistance() {
        return soundData.getMaxDistance();
    }

    public float getRolloff() {
        return soundData.getRolloff();
    }

    // Length \\

    // Seconds of samples at their own rate
    public float getLength() {
        return (float) soundData.getFrameCount() / soundData.getSampleRate();
    }

    // Seconds it plays for at its own pitch
    public float getPlayedLength() {
        return getLength() / soundData.getPitch();
    }
}
