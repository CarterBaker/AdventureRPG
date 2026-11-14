package application.bootstrap.musicpipeline.songmanager;

import java.io.File;

import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.song.SongData;
import application.bootstrap.musicpipeline.song.SongHandle;
import application.bootstrap.musicpipeline.song.SongLaneStruct;
import application.bootstrap.musicpipeline.song.SongTempoStruct;
import application.bootstrap.musicpipeline.util.NoteScale;
import application.bootstrap.musicpipeline.util.SongArpgUtility;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SongBuilder extends BuilderPackage {

    /*
     * Parses one song ARPG file into a SongData and wraps it in a SongHandle,
     * under the short song ID SongManager assigns its name. "tempos" is the
     * tempo map and "beats_per_bar" and "beat_unit" the bar, both read and
     * held to their rules by SongArpgUtility;
     * "key_root" and "key_scale" the key; "lanes" at most SONG_LANE_COUNT
     * lanes, each read through SongArpgUtility; "clips" the clips laid on
     * them, read through SongArpgUtility. A song the editor saves again is
     * read into the handle it already has. A
     * notes clip's instrument and a song clip's song load on demand, and a
     * sound clip's sound must stand under the sound directory. The songs
     * being built are kept as a chain, so a song clip naming one of them —
     * the song itself or one that lays it, at any depth — is refused naming
     * the loop before anything loads twice.
     */

    // Internal
    private SongManager songManager;
    private InstrumentManager instrumentManager;
    private SoundManager soundManager;

    // Chain — the songs being built, each laying the next
    private ObjectArrayList<String> buildingSongNames;

    // Base \\

    @Override
    protected void create() {

        // Chain
        this.buildingSongNames = new ObjectArrayList<>();
    }

    @Override
    protected void get() {

        // Internal
        this.songManager = get(SongManager.class);
        this.instrumentManager = get(InstrumentManager.class);
        this.soundManager = get(SoundManager.class);
    }

    // Build \\

    SongHandle build(File file, String songName) {

        SongHandle handle = create(SongHandle.class);

        handle.constructor(read(file, songName, songManager.registerSongName(songName)));
        return handle;
    }

    // The song saved again by the editor read into the handle it already has, so every clip laying it plays it as it
    // now stands; the handle keeps what it held when the file is refused
    void rebuild(File file, String songName, SongHandle handle) {
        handle.constructor(read(file, songName, handle.getSongID()));
    }

    private SongData read(File file, String songName, short songID) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        String owner = "Song \"" + songName + "\"";

        String fileOwner = owner + " (" + file.getName() + ")";
        SongTempoStruct tempo = SongArpgUtility.readTempo(ArpgUtility.validateArray(arpg, "tempos"), fileOwner);
        int beatsPerBar = ArpgUtility.getInt(arpg, "beats_per_bar", EngineSetting.DEFAULT_SONG_BEATS_PER_BAR);
        int beatUnit = ArpgUtility.getInt(arpg, "beat_unit", EngineSetting.DEFAULT_SONG_BEAT_UNIT);
        int keyRoot = ArpgUtility.getInt(arpg, "key_root", EngineSetting.DEFAULT_SONG_KEY_ROOT);

        SongArpgUtility.validateMeter(beatsPerBar, beatUnit, fileOwner);
        SongArpgUtility.validateKeyRoot(keyRoot, fileOwner);

        String displayName = ArpgUtility.getString(arpg, "display_name", EngineSetting.SONG_DISPLAY_NAME_NONE);

        if (displayName.isEmpty())
            displayName = songName.substring(songName.lastIndexOf(EngineSetting.PATH_SEPARATOR) + 1);

        SongLaneStruct[] lanes = parseLanes(ArpgUtility.validateArray(arpg, "lanes"), owner, file);

        buildingSongNames.add(songName);

        try {
            return new SongData(
                    songName,
                    songID,
                    displayName,
                    tempo,
                    beatsPerBar,
                    beatUnit,
                    keyRoot,
                    ArpgUtility.getEnum(arpg, "key_scale", NoteScale.class, NoteScale.MAJOR),
                    lanes,
                    parseClips(ArpgUtility.validateArray(arpg, "clips"), lanes.length, owner, file));
        } finally {
            buildingSongNames.pop();
        }
    }

    // Lane Parsing \\

    private SongLaneStruct[] parseLanes(ArpgArrayStruct lanesArpg, String owner, File file) {

        if (lanesArpg.size() > EngineSetting.SONG_LANE_COUNT)
            throwException(owner + " has " + lanesArpg.size() + " lanes, more than " + EngineSetting.SONG_LANE_COUNT
                    + ". File: " + file.getName());

        SongLaneStruct[] lanes = new SongLaneStruct[lanesArpg.size()];

        for (int i = 0; i < lanes.length; i++)
            lanes[i] = SongArpgUtility.readLane(lanesArpg.get(i).getAsObject(),
                    owner + " lane " + i + " (" + file.getName() + ")");

        return lanes;
    }

    // Clip Parsing \\

    private SongClipStruct[] parseClips(ArpgArrayStruct clipsArpg, int laneCount, String owner, File file) {

        SongClipStruct[] clips = new SongClipStruct[clipsArpg.size()];

        for (int i = 0; i < clips.length; i++) {

            String clipOwner = owner + " clip " + i;
            SongClipStruct clip = SongArpgUtility.readClip(clipsArpg.get(i).getAsObject(), laneCount, clipOwner);

            resolveClip(clip, clipOwner, file);
            clips[i] = clip;
        }

        return clips;
    }

    // What the clip names resolved once: its instrument, its song, or its sound checked to exist
    private void resolveClip(SongClipStruct clip, String owner, File file) {

        if (clip.getKind() == SongClipKind.NOTES)
            clip.resolveInstrument(instrumentManager.getInstrumentIDFromInstrumentName(clip.getInstrumentName()));

        if (clip.getKind() == SongClipKind.SONG)
            clip.resolveSong(resolveSong(clip.getSongName(), owner, file));

        if (clip.getKind() == SongClipKind.SOUND && !soundManager.hasSoundFile(clip.getSoundName()))
            throwException(owner + " plays sound \"" + clip.getSoundName() + "\", which is not under '"
                    + EngineSetting.SOUND_PATH + "'. File: " + file.getName());
    }

    // The song a song clip lays, loaded on demand, refused when it is one of the songs laying it
    private SongHandle resolveSong(String songName, String owner, File file) {

        int loopStart = buildingSongNames.indexOf(songName);

        if (loopStart != EngineSetting.INDEX_NOT_FOUND)
            throwException(owner + " lays song \"" + songName + "\", which reaches itself through its song clips: "
                    + describeLoop(loopStart, songName) + ". File: " + file.getName());

        if (!songManager.hasSongFile(songName))
            throwException(owner + " lays song \"" + songName + "\", which is not under '" + EngineSetting.SONG_PATH
                    + "'. File: " + file.getName());

        return songManager.getSongHandleFromSongName(songName);
    }

    // The loop read from the first song in it, through every song laying the next, back to it
    private String describeLoop(int loopStart, String songName) {

        StringBuilder loop = new StringBuilder();

        for (int i = loopStart; i < buildingSongNames.size(); i++)
            loop.append(buildingSongNames.get(i)).append(EngineSetting.SONG_LOOP_SEPARATOR);

        return loop.append(songName).toString();
    }
}
