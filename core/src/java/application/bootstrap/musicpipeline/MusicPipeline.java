package application.bootstrap.musicpipeline;

import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.notesetmanager.NoteSetManager;
import application.bootstrap.musicpipeline.samplermanager.SamplerManager;
import application.bootstrap.musicpipeline.songmanager.SongManager;
import engine.root.PipelinePackage;

public class MusicPipeline extends PipelinePackage {

    /*
     * Registers the note set manager, which loads the samples an instrument
     * plays from the first time it plays, the instrument manager, which loads
     * each instrument when something names it, the sampler, which plays
     * instruments note by note through the sound pipeline's voices, and the
     * song manager, which loads the project's songs when something names one
     * and reads a player's MIDI songs when they are played.
     */

    @Override
    protected void create() {
        create(NoteSetManager.class);
        create(InstrumentManager.class);
        create(SamplerManager.class);
        create(SongManager.class);
    }
}
