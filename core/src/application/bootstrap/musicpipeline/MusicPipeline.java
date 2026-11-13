package application.bootstrap.musicpipeline;

import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.notesetmanager.NoteSetManager;
import application.bootstrap.musicpipeline.samplermanager.SamplerManager;
import engine.root.PipelinePackage;

public class MusicPipeline extends PipelinePackage {

    /*
     * Registers the note set manager, which loads the samples an instrument
     * plays from the first time it plays, the instrument manager, which loads
     * each instrument when something names it, and the sampler, which plays
     * instruments note by note through the sound pipeline's voices.
     */

    @Override
    protected void create() {
        create(NoteSetManager.class);
        create(InstrumentManager.class);
        create(SamplerManager.class);
    }
}
