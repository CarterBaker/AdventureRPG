package editor.runtime.notelibrary;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.notelibrary.panel.NoteLibraryPanelSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import engine.root.ContextPackage;

public class NoteLibraryContext extends ContextPackage implements PropertiesProvider {

    /*
     * Editor tab for the notes of the instrument picked in the Hierarchy:
     * every note it plays, octave by octave, each with a button that sounds
     * it, and the instrument's details. A note is picked up from its row and
     * let go over the Timeline to lay it there as a note clip, stretched
     * like any clip to hold the note longer. All state lives in
     * NoteLibraryManager; this tab only shows it and hands clicks and drags
     * to NoteLibraryBranch.
     */

    // Internal
    private NoteLibraryPanelSystem noteLibraryPanelSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.noteLibraryPanelSystem = create(NoteLibraryPanelSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Properties \\

    // What this tool works on is the instrument selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }
}
