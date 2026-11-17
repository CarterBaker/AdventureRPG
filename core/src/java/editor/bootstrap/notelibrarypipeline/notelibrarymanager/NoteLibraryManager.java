package editor.bootstrap.notelibrarypipeline.notelibrarymanager;

import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.notesetmanager.NoteSetManager;
import application.bootstrap.musicpipeline.samplermanager.SamplerManager;
import application.bootstrap.musicpipeline.util.NoteUtility;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;

public class NoteLibraryManager extends ManagerPackage {

    /*
     * Owns the note library every Note Library tab shows: the instrument
     * picked in the Hierarchy, or the instrument a note set picked there is
     * played as, every note it plays from its lowest to its highest, the
     * note picked last, and the one preview that sounds it. Nothing is
     * listed until one is picked.
     * Picking an instrument loads its samples, so its notes sound at once
     * when previewed or laid; a note previewed sounds for a moment through
     * the sampler and is then let go, fading as the instrument fades. Notes
     * are dragged onto the Timeline through the recorder itself. Each change
     * moves the revision the tab redraws on.
     */

    // Internal
    private InstrumentManager instrumentManager;
    private NoteSetManager noteSetManager;
    private SamplerManager samplerManager;
    private InfoManager infoManager;

    // Selection
    private short instrumentID;
    private int selectedPitch;

    // Preview — the note sounding, and how long it has left before it is let go
    private int previewHandle;
    private float previewLeft;

    // Status
    private int revision;
    private String statusText;

    // Base \\

    @Override
    protected void create() {

        // Selection
        this.instrumentID = EngineSetting.INSTRUMENT_NONE;
        this.selectedPitch = EngineSetting.INDEX_NOT_FOUND;

        // Preview
        this.previewHandle = EngineSetting.INDEX_NOT_FOUND;

        // Status
        this.statusText = EditorSetting.NOTE_LIBRARY_STATUS_READY;
    }

    @Override
    protected void get() {
        this.instrumentManager = get(InstrumentManager.class);
        this.noteSetManager = get(NoteSetManager.class);
        this.samplerManager = get(SamplerManager.class);
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSelectionListener(
                EditorSetting.INFO_SCHEMA_INSTRUMENTS, entry -> selectInstrument(entry.getEntryName()));
        infoManager.addSelectionListener(
                EditorSetting.INFO_SCHEMA_NOTE_SETS, entry -> selectNoteSet(entry.getEntryName()));
    }

    // Update \\

    @Override
    protected void update() {

        if (previewHandle == EngineSetting.INDEX_NOT_FOUND)
            return;

        this.previewLeft -= internal.getDeltaTime();

        if (previewLeft > 0f)
            return;

        letGoPreview();
    }

    // Selection \\

    // The instrument named becomes the one listed, its samples asked for; refused, the status says why
    public void selectInstrument(String instrumentName) {

        short chosenID;

        try {
            chosenID = instrumentManager.getInstrumentIDFromInstrumentName(instrumentName);
        } catch (InternalException e) {
            setStatus(EditorSetting.NOTE_LIBRARY_STATUS_REFUSED + e.getMessage());
            return;
        }

        choose(chosenID);
    }

    // The instrument the note set named is played as becomes the one listed; with none, the status says so
    public void selectNoteSet(String noteSetName) {

        short chosenID = instrumentManager.getInstrumentIDFromNoteSetName(noteSetName);

        if (chosenID == EngineSetting.INSTRUMENT_NONE) {
            setStatus(EditorSetting.NOTE_LIBRARY_STATUS_UNPLAYED + noteSetName);
            return;
        }

        choose(chosenID);
    }

    // The one way the instrument listed changes: its samples asked for, any preview of the last let go
    private void choose(short chosenID) {

        if (chosenID == instrumentID)
            return;

        letGoPreview();
        this.instrumentID = chosenID;
        this.selectedPitch = EngineSetting.INDEX_NOT_FOUND;
        noteSetManager.prepare(getInstrument().getNoteSetID());
        setStatus(EditorSetting.NOTE_LIBRARY_STATUS_SELECTED + getInstrument().getDisplayName());
    }

    // The note picked and sounded for a moment, any note still sounding let go first
    public void previewNote(int pitch) {

        if (!hasInstrument() || !getInstrument().plays(pitch))
            return;

        letGoPreview();
        this.selectedPitch = pitch;
        this.previewHandle = samplerManager.noteOn(instrumentID, pitch, EditorSetting.RECORDER_NOTE_VELOCITY);
        this.previewLeft = EditorSetting.NOTE_LIBRARY_PREVIEW_SECONDS;
        setStatus(EditorSetting.NOTE_LIBRARY_STATUS_PLAYING + NoteUtility.toNoteName(pitch));
    }

    // The one way a preview ends; the note fades out as the instrument's release says
    private void letGoPreview() {

        if (previewHandle == EngineSetting.INDEX_NOT_FOUND)
            return;

        samplerManager.noteOff(previewHandle);
        this.previewHandle = EngineSetting.INDEX_NOT_FOUND;
    }

    // Utility \\

    private void setStatus(String statusText) {
        this.statusText = statusText;
        revision++;
    }

    // Accessible \\

    public boolean hasInstrument() {
        return instrumentID != EngineSetting.INSTRUMENT_NONE;
    }

    public InstrumentHandle getInstrument() {
        return instrumentManager.getInstrumentHandleFromInstrumentID(instrumentID);
    }

    public int getSelectedPitch() {
        return selectedPitch;
    }

    public int getRevision() {
        return revision;
    }

    public String getStatusText() {
        return statusText;
    }
}
