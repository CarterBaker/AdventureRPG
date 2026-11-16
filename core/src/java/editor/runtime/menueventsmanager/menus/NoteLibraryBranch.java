package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.notelibrarypipeline.notelibrarymanager.NoteLibraryManager;
import engine.root.BranchPackage;

public class NoteLibraryBranch extends BranchPackage {

    /*
     * Menu event handlers for the Note Library tab. Every action works on the
     * one note library all tabs share. A row's play button sounds its note;
     * pressing the row itself sounds it and picks it up, to be laid as a note
     * clip of the instrument shown on the Timeline it is let go over. Rows
     * hand back the pitch of the note they show.
     */

    // Internal
    private NoteLibraryManager noteLibraryManager;
    private RecorderBranch recorderBranch;

    // Base \\

    @Override
    protected void get() {
        this.noteLibraryManager = get(NoteLibraryManager.class);
        this.recorderBranch = get(RecorderBranch.class);
    }

    // Notes \\

    public void previewNote(String pitchText) {
        noteLibraryManager.previewNote(Integer.parseInt(pitchText));
    }

    // The note is sounded on the press and carried toward the Timeline's lanes until let go
    public void dragNote(String pitchText, WindowInstance window) {

        if (!noteLibraryManager.hasInstrument())
            return;

        int pitch = Integer.parseInt(pitchText);

        if (pitch != noteLibraryManager.getSelectedPitch())
            noteLibraryManager.previewNote(pitch);

        recorderBranch.dragNote(noteLibraryManager.getInstrument().getInstrumentName(), pitch, window);
    }
}
