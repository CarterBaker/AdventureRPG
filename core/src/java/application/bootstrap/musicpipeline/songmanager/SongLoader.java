package application.bootstrap.musicpipeline.songmanager;

import java.io.File;

import application.bootstrap.musicpipeline.song.SongHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SongLoader extends LoaderPackage {

    /*
     * Scans the song ARPG directory and remembers every file by name,
     * queueing none: a song loads only when something names it — a song clip
     * of another song, a player, a tool — at bootstrap or while the game
     * runs, so the loader persists for the engine lifetime. A song the
     * editor saves is remembered by name as it is written, and read again
     * into its handle when it has loaded already.
     */

    // Internal
    private File root;
    private SongManager songManager;
    private SongBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> songName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SONG_PATH);
        this.songName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Song ARPG directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS))
            songName2File.put(FileUtility.getPathWithFileNameWithoutExtension(root, file), file);
    }

    @Override
    protected void create() {
        this.internalBuilder = create(SongBuilder.class);
    }

    @Override
    protected void get() {
        this.songManager = get(SongManager.class);
    }

    // Kept for the engine lifetime, so a song loads whenever it is first named
    @Override
    protected boolean persists() {
        return true;
    }

    // Load \\

    @Override
    protected void load(File file) {

        String songName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        SongHandle handle = internalBuilder.build(file, songName);

        if (handle == null)
            throwException("Failed to build song from: " + file.getAbsolutePath());

        songManager.addSong(handle);
    }

    // On-Demand \\

    void request(String songName) {

        File file = songName2File.get(songName);

        if (file == null)
            throwException("On-demand song load failed — not found in scan registry: \"" + songName + "\"");

        request(file);
    }

    boolean hasFile(String songName) {
        return songName2File.containsKey(songName);
    }

    // Every song file by name, sorted by name
    void collectNames(ObjectArrayList<String> songNames) {
        songNames.addAll(songName2File.keySet());
        songNames.sort(null);
    }

    File resolveFile(String songName) {
        return ArpgUtility.resolveFile(root, songName);
    }

    // Live Edit \\

    // The song just written under the name remembered, and read again into its handle when it has loaded
    void rebuild(String songName, SongHandle handle) {

        File file = resolveFile(songName);

        songName2File.put(songName, file);

        if (handle != null)
            internalBuilder.rebuild(file, songName, handle);
    }

    // A song file deleted is no longer offered; a handle already loaded keeps playing what it held
    void forget(String songName) {
        songName2File.remove(songName);
    }
}
