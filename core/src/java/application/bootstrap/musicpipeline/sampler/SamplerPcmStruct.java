package application.bootstrap.musicpipeline.sampler;

import application.bootstrap.soundpipeline.sound.SoundHandle;
import application.bootstrap.soundpipeline.sound.SoundPcmStruct;
import engine.root.StructPackage;

public class SamplerPcmStruct extends StructPackage {

    /*
     * One sample decoded for offline mixing, as SamplerMixUtility reads it:
     * its frames at the rate of the mix, the stretch its sustain loop repeats
     * in frames of that rate — none when its end is not past its start — how
     * fast its companion pitches it, and whether the sound itself loops whole,
     * as a sound clip's sound may.
     */

    // Samples
    private final SoundPcmStruct pcm;

    // Loop
    private final int loopStartFrame;
    private final int loopEndFrame;
    private final boolean looping;

    // Playback
    private final float pitch;

    // Constructor \\

    public SamplerPcmStruct(SoundHandle soundHandle, SoundPcmStruct pcm) {

        // Samples
        this.pcm = pcm;

        // Loop
        this.loopStartFrame = (int) (soundHandle.getLoopStartSeconds() * pcm.getSampleRate());
        this.loopEndFrame = (int) (soundHandle.getLoopEndSeconds() * pcm.getSampleRate());
        this.looping = soundHandle.isLoop();

        // Playback
        this.pitch = soundHandle.getPitch();
    }

    // Accessible \\

    public SoundPcmStruct getPcm() {
        return pcm;
    }

    // True when the sample has a sustain loop to repeat
    public boolean hasLoop() {
        return loopEndFrame > loopStartFrame;
    }

    public int getLoopStartFrame() {
        return loopStartFrame;
    }

    public int getLoopEndFrame() {
        return loopEndFrame;
    }

    public boolean isLooping() {
        return looping;
    }

    public float getPitch() {
        return pitch;
    }
}
