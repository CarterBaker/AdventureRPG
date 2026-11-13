package application.bootstrap.musicpipeline.notesetmanager;

import application.bootstrap.musicpipeline.noteset.NoteRegionStruct;
import application.bootstrap.musicpipeline.noteset.NoteSetHandle;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class NoteSetManager extends ManagerPackage {

    /*
     * Owns the note set palette for the engine lifetime, with IDs assigned in
     * registration order. A note set is the samples one instrument plays from;
     * its definition loads on demand when an instrument names it, and its
     * samples — nearly all of its cost — load the first time it plays, never
     * at bootstrap: prepare() asks the sound pipeline for every sample, which
     * decodes them on a worker, and the note set is ready once all of them
     * have landed. ID 0 is the reserved sentinel. Auto-triggers an on-demand
     * load via NoteSetLoader on a name-based cache miss.
     */

    // Internal
    private SoundManager soundManager;

    // Palette
    private Object2IntOpenHashMap<String> noteSetName2NoteSetID;
    private ObjectArrayList<NoteSetHandle> noteSetID2NoteSetHandle;

    // Samples — how far each note set's samples have loaded, and those still loading
    private ByteArrayList noteSetID2SampleState;
    private ObjectArrayList<NoteSetHandle> preparingNoteSets;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.noteSetName2NoteSetID = RegistryUtility.createNameIndex();
        this.noteSetID2NoteSetHandle = RegistryUtility.createPalette();

        // Samples
        this.noteSetID2SampleState = new ByteArrayList();
        this.noteSetID2SampleState.add(EngineSetting.NOTE_SET_SAMPLES_UNLOADED);
        this.preparingNoteSets = new ObjectArrayList<>();

        create(NoteSetLoader.class);
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
    }

    // Management \\

    short registerNoteSetName(String noteSetName) {

        short noteSetID = (short) RegistryUtility.registerID(
                noteSetName2NoteSetID, noteSetID2NoteSetHandle, noteSetName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);

        while (noteSetID2SampleState.size() <= noteSetID)
            noteSetID2SampleState.add(EngineSetting.NOTE_SET_SAMPLES_UNLOADED);

        return noteSetID;
    }

    void addNoteSet(NoteSetHandle handle) {
        noteSetID2NoteSetHandle.set(handle.getNoteSetID(), handle);
    }

    // Samples \\

    // The note set's samples asked for, the first time it plays; nothing more once asked
    public void prepare(short noteSetID) {

        if (noteSetID2SampleState.getByte(noteSetID) != EngineSetting.NOTE_SET_SAMPLES_UNLOADED)
            return;

        NoteSetHandle handle = getNoteSetHandleFromNoteSetID(noteSetID);

        for (int i = 0; i < handle.getRegionCount(); i++) {

            NoteRegionStruct region = handle.getRegion(i);
            int soundID = soundManager.requestSoundAsync(region.getSoundName());

            if (soundID == EngineSetting.INDEX_NOT_FOUND)
                throwException("Note set \"" + handle.getNoteSetName() + "\" plays sound \"" + region.getSoundName()
                        + "\", which is not under '" + EngineSetting.SOUND_PATH + "'.");

            region.resolveSound(soundID);
        }

        noteSetID2SampleState.set(noteSetID, EngineSetting.NOTE_SET_SAMPLES_LOADING);
        preparingNoteSets.add(handle);
    }

    public boolean isReady(short noteSetID) {
        return noteSetID2SampleState.getByte(noteSetID) == EngineSetting.NOTE_SET_SAMPLES_READY;
    }

    // Update \\

    // Every preparing note set whose samples have all landed checked and made ready
    @Override
    protected void update() {

        for (int i = preparingNoteSets.size() - 1; i >= 0; i--) {

            NoteSetHandle handle = preparingNoteSets.get(i);

            if (!hasLoadedSamples(handle))
                continue;

            validateLoops(handle);
            noteSetID2SampleState.set(handle.getNoteSetID(), EngineSetting.NOTE_SET_SAMPLES_READY);
            preparingNoteSets.set(i, preparingNoteSets.get(preparingNoteSets.size() - 1));
            preparingNoteSets.remove(preparingNoteSets.size() - 1);
        }
    }

    private boolean hasLoadedSamples(NoteSetHandle handle) {

        for (int i = 0; i < handle.getRegionCount(); i++)
            if (!soundManager.isSoundLoaded(handle.getRegion(i).getSoundID()))
                return false;

        return true;
    }

    // A region that loops plays a sample with loop points
    private void validateLoops(NoteSetHandle handle) {

        for (int i = 0; i < handle.getRegionCount(); i++) {

            NoteRegionStruct region = handle.getRegion(i);

            if (region.getLoopMode().loopsHeld()
                    && !soundManager.getSoundHandleFromSoundID(region.getSoundID()).hasLoopPoints())
                throwException("Note set \"" + handle.getNoteSetName() + "\" loops sound \"" + region.getSoundName()
                        + "\", whose companion sets no \"loopStart\" and \"loopEnd\".");
        }
    }

    // Accessible \\

    public boolean hasNoteSet(String noteSetName) {
        return RegistryUtility.getHandle(noteSetName2NoteSetID, noteSetID2NoteSetHandle, noteSetName) != null;
    }

    public short getNoteSetIDFromNoteSetName(String noteSetName) {

        if (!hasNoteSet(noteSetName))
            ((NoteSetLoader) internalLoader).request(noteSetName);

        if (!hasNoteSet(noteSetName))
            throwException("Note set could not be loaded: \"" + noteSetName + "\"");

        return (short) noteSetName2NoteSetID.getInt(noteSetName);
    }

    public NoteSetHandle getNoteSetHandleFromNoteSetID(short noteSetID) {

        NoteSetHandle handle = RegistryUtility.getHandle(noteSetID2NoteSetHandle, noteSetID);

        if (handle == null)
            throwException("Note set ID not found: " + noteSetID);

        return handle;
    }

    public NoteSetHandle getNoteSetHandleFromNoteSetName(String noteSetName) {
        return getNoteSetHandleFromNoteSetID(getNoteSetIDFromNoteSetName(noteSetName));
    }
}
