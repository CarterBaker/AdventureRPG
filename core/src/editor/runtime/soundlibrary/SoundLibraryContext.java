package editor.runtime.soundlibrary;

import editor.runtime.soundlibrary.panel.SoundLibraryPanelSystem;
import engine.root.ContextPackage;

public class SoundLibraryContext extends ContextPackage {

    /*
     * Editor tab for the sound library: every sound the game loads, folder by
     * folder, the selected one's details, and the transport that samples it
     * — play, pause, stop, rewind and loop, scrubbed along its length. A sound
     * is picked up from its row and let go over a Recorder tab's timeline to
     * lay it there as a sound effect over the takes. All state lives in
     * SoundLibraryManager; this tab only shows it and hands clicks and drags
     * to SoundLibraryBranch.
     */

    // Internal
    private SoundLibraryPanelSystem soundLibraryPanelSystem;

    // Internal \\

    @Override
    protected void create() {
        this.soundLibraryPanelSystem = create(SoundLibraryPanelSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
