package application.bootstrap.musicpipeline.notesetmanager;

import java.io.File;

import application.bootstrap.musicpipeline.noteset.NoteSetHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class NoteSetLoader extends LoaderPackage {

    /*
     * Scans the note set ARPG directory and remembers every file by name,
     * queueing none: a note set loads only when an instrument names it, at
     * bootstrap or while the game runs, so the loader persists for the engine
     * lifetime.
     */

    // Internal
    private File root;
    private NoteSetManager noteSetManager;
    private NoteSetBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> noteSetName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.NOTE_SET_PATH);
        this.noteSetName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Note set ARPG directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS))
            noteSetName2File.put(FileUtility.getPathWithFileNameWithoutExtension(root, file), file);
    }

    @Override
    protected void create() {
        this.internalBuilder = create(NoteSetBuilder.class);
    }

    @Override
    protected void get() {
        this.noteSetManager = get(NoteSetManager.class);
    }

    // Kept for the engine lifetime, so a note set loads whenever an instrument first names it
    @Override
    protected boolean persists() {
        return true;
    }

    // Load \\

    @Override
    protected void load(File file) {

        String noteSetName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        NoteSetHandle handle = internalBuilder.build(file, noteSetName);

        if (handle == null)
            throwException("Failed to build note set from: " + file.getAbsolutePath());

        noteSetManager.addNoteSet(handle);
    }

    // On-Demand \\

    void request(String noteSetName) {

        File file = noteSetName2File.get(noteSetName);

        if (file == null)
            throwException("On-demand note set load failed — not found in scan registry: \"" + noteSetName + "\"");

        request(file);
    }
}
