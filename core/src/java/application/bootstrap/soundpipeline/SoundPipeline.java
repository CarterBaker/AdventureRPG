package application.bootstrap.soundpipeline;

import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import engine.root.PipelinePackage;

public class SoundPipeline extends PipelinePackage {

    /*
     * Registers the sound manager, which opens the audio device, loads every
     * sound under the sound directory and plays them for any system that asks,
     * then the sound cue manager, which plays what the game names for each
     * kind of moment through it.
     */

    @Override
    protected void create() {
        create(SoundManager.class);
        create(SoundCueManager.class);
    }
}
