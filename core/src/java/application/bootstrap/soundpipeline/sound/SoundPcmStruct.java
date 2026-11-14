package application.bootstrap.soundpipeline.sound;

import engine.root.StructPackage;

public class SoundPcmStruct extends StructPackage {

    /*
     * One sound's decoded samples: signed 16-bit, every channel of a frame
     * side by side, at the rate and over the channels they were decoded to.
     */

    // Samples
    private final short[] samples;
    private final int channels;
    private final int sampleRate;

    // Constructor \\

    public SoundPcmStruct(short[] samples, int channels, int sampleRate) {

        // Samples
        this.samples = samples;
        this.channels = channels;
        this.sampleRate = sampleRate;
    }

    // Accessible \\

    public short[] getSamples() {
        return samples;
    }

    public int getChannels() {
        return channels;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getFrameCount() {
        return samples.length / channels;
    }
}
