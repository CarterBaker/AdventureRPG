package editor.runtime.notelibrary.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import application.bootstrap.musicpipeline.util.NoteUtility;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.notelibrarypipeline.notelibrarymanager.NoteLibraryManager;
import editor.runtime.EditorSetting;
import editor.runtime.notelibrary.NoteLibrarySetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class NoteLibraryPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the note library.
     * When the library's revision moves it refills the list — a head for
     * every octave and beneath it a row for each note the instrument plays,
     * named and numbered, picked up to drag it onto the Timeline, with a
     * button to sound it, the note picked last marked — the instrument's
     * details and the status line. Both lists stay empty until an instrument
     * or a note set is picked.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private NoteLibraryManager noteLibraryManager;

    // Menus
    private MenuInstance libraryMenu;
    private ElementInstance statusLabel;
    private ElementInstance listTitle;

    // State
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.noteLibraryManager = get(NoteLibraryManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.libraryMenu = menuManager.openMenu(EditorSetting.NOTE_LIBRARY_MENU, window);

        this.statusLabel = requireEntryPoint(NoteLibrarySetting.ENTRY_STATUS);
        this.listTitle = requireEntryPoint(NoteLibrarySetting.ENTRY_LIST_TITLE);

        requireEntryPoint(NoteLibrarySetting.ENTRY_NOTES);
        requireEntryPoint(NoteLibrarySetting.ENTRY_DETAILS);
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = libraryMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Note library menu '" + EditorSetting.NOTE_LIBRARY_MENU + "' has no entry point "
                    + entryPoint + ".");

        return element;
    }

    // Update \\

    @Override
    protected void update() {

        if (noteLibraryManager.getRevision() == shownRevision)
            return;

        this.shownRevision = noteLibraryManager.getRevision();
        statusLabel.setFontText(noteLibraryManager.getStatusText());
        refreshNotes();
        refreshDetails();
    }

    // Notes \\

    // A row for every note the instrument plays, lowest first, each octave headed by its number
    private void refreshNotes() {

        menuManager.ejectAll(libraryMenu, NoteLibrarySetting.ENTRY_NOTES);

        if (!noteLibraryManager.hasInstrument()) {
            listTitle.setFontText(NoteLibrarySetting.TEXT_LIST_TITLE);
            return;
        }

        InstrumentHandle instrument = noteLibraryManager.getInstrument();

        listTitle.setFontText(String.format(NoteLibrarySetting.FORMAT_LIST_TITLE,
                instrument.getDisplayName(), instrument.getHigh() - instrument.getLow() + 1));

        for (int pitch = instrument.getLow(); pitch <= instrument.getHigh(); pitch++) {

            if (pitch == instrument.getLow() || pitch % NoteLibrarySetting.NOTES_PER_OCTAVE == 0)
                injectHeader(NoteLibrarySetting.ENTRY_NOTES, String.format(NoteLibrarySetting.FORMAT_OCTAVE,
                        Math.floorDiv(pitch, NoteLibrarySetting.NOTES_PER_OCTAVE) - 1));

            injectNote(pitch);
        }
    }

    // A row picked up to drag its note onto the Timeline, with its number and a button to sound it
    private void injectNote(int pitch) {

        String pitchText = Integer.toString(pitch);
        ElementInstance row = menuManager.inject(
                libraryMenu,
                NoteLibrarySetting.ENTRY_NOTES,
                pitch == noteLibraryManager.getSelectedPitch()
                        ? NoteLibrarySetting.TEMPLATE_NOTE_ROW_SELECTED
                        : NoteLibrarySetting.TEMPLATE_NOTE_ROW);

        row.findChildById(NoteLibrarySetting.ELEMENT_ROW_SELECT).setOnDragArgOverride(pitchText);
        row.findChildById(NoteLibrarySetting.ELEMENT_ROW_PLAY).setActionArgOverride(pitchText);
        row.findChildById(NoteLibrarySetting.ELEMENT_ROW_LABEL).setFontText(NoteUtility.toNoteName(pitch));
        row.findChildById(NoteLibrarySetting.ELEMENT_ROW_VALUE).setFontText(pitchText);
    }

    // Details \\

    // The instrument the notes belong to, and how to lay them
    private void refreshDetails() {

        menuManager.ejectAll(libraryMenu, NoteLibrarySetting.ENTRY_DETAILS);

        if (!noteLibraryManager.hasInstrument())
            return;

        InstrumentHandle instrument = noteLibraryManager.getInstrument();

        injectHeader(NoteLibrarySetting.ENTRY_DETAILS, instrument.getDisplayName());
        injectDetail(NoteLibrarySetting.TEXT_INSTRUMENT, instrument.getInstrumentName());
        injectDetail(NoteLibrarySetting.TEXT_NOTE_SET, instrument.getNoteSetHandle().getNoteSetName());
        injectDetail(NoteLibrarySetting.TEXT_RANGE, String.format(NoteLibrarySetting.FORMAT_RANGE,
                NoteUtility.toNoteName(instrument.getLow()), NoteUtility.toNoteName(instrument.getHigh())));
        injectDetail(NoteLibrarySetting.TEXT_POLYPHONY, Integer.toString(instrument.getPolyphony()));
        injectDetail(NoteLibrarySetting.TEXT_PERCUSSION,
                instrument.isPercussion() ? NoteLibrarySetting.TEXT_YES : NoteLibrarySetting.TEXT_NO);
        injectText(NoteLibrarySetting.ENTRY_DETAILS, NoteLibrarySetting.TEXT_DRAG_HINT);
        injectText(NoteLibrarySetting.ENTRY_DETAILS, NoteLibrarySetting.TEXT_CHORD_HINT);
    }

    private void injectDetail(String label, String value) {

        ElementInstance row = menuManager.inject(
                libraryMenu, NoteLibrarySetting.ENTRY_DETAILS, NoteLibrarySetting.TEMPLATE_DETAIL_ROW);

        row.findChildById(NoteLibrarySetting.ELEMENT_ROW_LABEL).setFontText(label);
        row.findChildById(NoteLibrarySetting.ELEMENT_ROW_VALUE).setFontText(value);
    }

    // Utility \\

    private void injectHeader(int entryPoint, String text) {
        menuManager.inject(libraryMenu, entryPoint, NoteLibrarySetting.TEMPLATE_HEADER_ROW)
                .findChildById(NoteLibrarySetting.ELEMENT_ROW_LABEL).setFontText(text);
    }

    private void injectText(int entryPoint, String text) {
        menuManager.inject(libraryMenu, entryPoint, NoteLibrarySetting.TEMPLATE_TEXT_ROW)
                .findChildById(NoteLibrarySetting.ELEMENT_ROW_LABEL).setFontText(text);
    }
}
