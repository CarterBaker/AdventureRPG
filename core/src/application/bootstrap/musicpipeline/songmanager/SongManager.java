package application.bootstrap.musicpipeline.songmanager;

import java.io.File;

import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.song.SongHandle;
import application.bootstrap.musicpipeline.util.SongMidiUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.io.FileUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SongManager extends ManagerPackage {

    /*
     * Owns the song palette for the engine lifetime, with IDs assigned in
     * registration order. A song is an arrangement of clips on lanes over a
     * tempo map; the project's songs load on demand, when a song clip, a
     * player or a tool first names one, and SONG_NONE (0) is the reserved
     * sentinel. A player's songs are MIDI files in SONG_USER_DIRECTORY,
     * listed newest first, read through SongMidiUtility when played and kept
     * by name apart from the palette, held by every play that loads one and
     * let go once the last of them releases it; an unreadable one is logged
     * and plays nothing. Auto-triggers an on-demand load via SongLoader on a
     * name-based cache miss.
     */

    // Internal
    private InstrumentManager instrumentManager;

    // Palette
    private Object2IntOpenHashMap<String> songName2SongID;
    private ObjectArrayList<SongHandle> songID2SongHandle;

    // User Songs — the player's songs being played, and how many plays hold each
    private File userSongDirectory;
    private Object2ObjectOpenHashMap<String, SongHandle> userSongName2SongHandle;
    private Object2IntOpenHashMap<String> userSongName2HoldCount;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.songName2SongID = RegistryUtility.createNameIndex();
        this.songID2SongHandle = RegistryUtility.createPalette();

        // User Songs
        this.userSongDirectory = new File(internal.path, EngineSetting.SONG_USER_DIRECTORY);
        this.userSongName2SongHandle = new Object2ObjectOpenHashMap<>();
        this.userSongName2HoldCount = new Object2IntOpenHashMap<>();

        create(SongLoader.class);
    }

    @Override
    protected void get() {
        this.instrumentManager = get(InstrumentManager.class);
    }

    // Management \\

    short registerSongName(String songName) {
        return (short) RegistryUtility.registerID(
                songName2SongID, songID2SongHandle, songName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addSong(SongHandle handle) {
        songID2SongHandle.set(handle.getSongID(), handle);
    }

    boolean hasSongFile(String songName) {
        return ((SongLoader) internalLoader).hasFile(songName);
    }

    // User Songs \\

    // Every player song's name, the most recently changed first
    public ObjectArrayList<String> getUserSongNames() {

        ObjectArrayList<File> songFiles = collectUserSongFiles();

        songFiles.sort((first, second) -> Long.compare(second.lastModified(), first.lastModified()));

        ObjectArrayList<String> songNames = new ObjectArrayList<>(songFiles.size());

        for (int i = 0; i < songFiles.size(); i++)
            songNames.add(FileUtility.getFileName(songFiles.get(i)));

        return songNames;
    }

    // The player's song held for one more play, read from its MIDI file when no play holds it yet; null, logged,
    // when it is gone or unreadable
    public SongHandle loadUserSong(String songName) {

        SongHandle handle = userSongName2SongHandle.get(songName);

        if (handle == null)
            handle = readUserSong(songName);

        if (handle == null)
            return null;

        userSongName2SongHandle.put(songName, handle);
        userSongName2HoldCount.addTo(songName, 1);

        return handle;
    }

    // One play's hold on a player's song let go; the song is let go with the last
    public void releaseUserSong(SongHandle handle) {

        String songName = handle.getSongName();

        if (userSongName2SongHandle.get(songName) != handle)
            return;

        if (userSongName2HoldCount.addTo(songName, -1) > 1)
            return;

        userSongName2HoldCount.removeInt(songName);
        userSongName2SongHandle.remove(songName);
    }

    public boolean isUserSongHeld(String songName) {
        return userSongName2SongHandle.containsKey(songName);
    }

    private SongHandle readUserSong(String songName) {

        File songFile = findUserSongFile(songName);

        if (songFile == null) {
            errorLog("Song '" + songName + "' no longer exists in: " + userSongDirectory.getAbsolutePath());
            return null;
        }

        try {

            SongHandle handle = create(SongHandle.class);
            handle.constructor(SongMidiUtility.read(songFile, instrumentManager));

            return handle;
        } catch (InternalException e) {
            errorLog("Song '" + songName + "' could not be read and was skipped: " + e.getMessage());
            return null;
        }
    }

    private File findUserSongFile(String songName) {

        ObjectArrayList<File> songFiles = collectUserSongFiles();

        for (int i = 0; i < songFiles.size(); i++)
            if (FileUtility.getFileName(songFiles.get(i)).equals(songName))
                return songFiles.get(i);

        return null;
    }

    private ObjectArrayList<File> collectUserSongFiles() {

        ObjectArrayList<File> songFiles = new ObjectArrayList<>();
        File[] files = userSongDirectory.listFiles();

        if (files != null)
            for (File file : files)
                if (file.isFile() && FileUtility.hasExtension(file, EngineSetting.SONG_USER_FILE_EXTENSION))
                    songFiles.add(file);

        return songFiles;
    }

    // Accessible \\

    public boolean hasSong(String songName) {
        return RegistryUtility.getHandle(songName2SongID, songID2SongHandle, songName) != null;
    }

    public short getSongIDFromSongName(String songName) {

        if (!hasSong(songName))
            ((SongLoader) internalLoader).request(songName);

        if (!hasSong(songName))
            throwException("Song could not be loaded: \"" + songName + "\"");

        return (short) songName2SongID.getInt(songName);
    }

    public SongHandle getSongHandleFromSongID(short songID) {

        SongHandle handle = RegistryUtility.getHandle(songID2SongHandle, songID);

        if (handle == null)
            throwException("Song ID not found: " + songID);

        return handle;
    }

    public SongHandle getSongHandleFromSongName(String songName) {
        return getSongHandleFromSongID(getSongIDFromSongName(songName));
    }
}
