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
     * under the short song ID SongManager assigns its name. "tempos" lists at
     * least one tempo change, the first at tick 0 and each later one at a
     * later tick, its "bpm" inside SONG_BPM_MIN to SONG_BPM_MAX;
     * "beats_per_bar" and "beat_unit" set the bar, the unit a power of two;
     * "key_root" and "key_scale" the key; "lanes" at most SONG_LANE_COUNT
     * lanes; "clips" the clips laid on them, read through SongArpgUtility. A
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

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        short songID = songManager.registerSongName(songName);
        String owner = "Song \"" + songName + "\"";

        SongTempoStruct tempo = parseTempos(ArpgUtility.validateArray(arpg, "tempos"), owner, file);
        int beatsPerBar = ArpgUtility.getInt(arpg, "beats_per_bar", EngineSetting.DEFAULT_SONG_BEATS_PER_BAR);
        int beatUnit = ArpgUtility.getInt(arpg, "beat_unit", EngineSetting.DEFAULT_SONG_BEAT_UNIT);
        int keyRoot = ArpgUtility.getInt(arpg, "key_root", EngineSetting.DEFAULT_SONG_KEY_ROOT);

        if (beatsPerBar < 1 || beatsPerBar > EngineSetting.SONG_BEATS_PER_BAR_MAX)
            throwException(owner + " holds " + beatsPerBar + " \"beats_per_bar\", outside 1 to "
                    + EngineSetting.SONG_BEATS_PER_BAR_MAX + ". File: " + file.getName());

        if (beatUnit < 1 || beatUnit > EngineSetting.SONG_BEAT_UNIT_MAX || Integer.bitCount(beatUnit) != 1)
            throwException(owner + " counts its beats in a \"beat_unit\" of " + beatUnit
                    + "; a beat is a whole, half, quarter, eighth, sixteenth or thirty-second note (1 to "
                    + EngineSetting.SONG_BEAT_UNIT_MAX + "). File: " + file.getName());

        if (keyRoot < 0 || keyRoot > EngineSetting.SONG_KEY_ROOT_MAX)
            throwException(owner + " names a \"key_root\" of " + keyRoot + "; a key's root is a step from C, 0 to "
                    + EngineSetting.SONG_KEY_ROOT_MAX + ". File: " + file.getName());

        String displayName = ArpgUtility.getString(arpg, "display_name", EngineSetting.SONG_DISPLAY_NAME_NONE);

        if (displayName.isEmpty())
            displayName = songName.substring(songName.lastIndexOf(EngineSetting.PATH_SEPARATOR) + 1);

        SongLaneStruct[] lanes = parseLanes(ArpgUtility.validateArray(arpg, "lanes"), owner, file);

        buildingSongNames.add(songName);

        try {

            SongData songData = new SongData(
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

            SongHandle handle = create(SongHandle.class);
            handle.constructor(songData);

            return handle;
        } finally {
            buildingSongNames.pop();
        }
    }

    // Tempo Parsing \\

    private SongTempoStruct parseTempos(ArpgArrayStruct temposArpg, String owner, File file) {

        if (temposArpg.size() == 0)
            throwException(owner + " has no tempo; its first plays from tick 0. File: " + file.getName());

        int[] ticks = new int[temposArpg.size()];
        float[] bpms = new float[temposArpg.size()];

        for (int i = 0; i < ticks.length; i++) {

            ArpgObjectStruct tempoArpg = temposArpg.get(i).getAsObject();

            ticks[i] = ArpgUtility.validateInt(tempoArpg, "tick");
            bpms[i] = ArpgUtility.validateFloat(tempoArpg, "bpm");

            if (i == 0 && ticks[i] != 0)
                throwException(owner + " starts its tempos at tick " + ticks[i] + "; the first plays from tick 0. "
                        + "File: " + file.getName());

            if (i > 0 && ticks[i] <= ticks[i - 1])
                throwException(owner + " tempo " + i + " at tick " + ticks[i] + " is not after the one before it, at "
                        + ticks[i - 1] + "; tempos run in order of their tick. File: " + file.getName());

            if (bpms[i] < EngineSetting.SONG_BPM_MIN || bpms[i] > EngineSetting.SONG_BPM_MAX)
                throwException(owner + " tempo " + i + " plays at " + bpms[i] + " beats a minute, outside "
                        + EngineSetting.SONG_BPM_MIN + " to " + EngineSetting.SONG_BPM_MAX + ". File: "
                        + file.getName());
        }

        return new SongTempoStruct(ticks, bpms);
    }

    // Lane Parsing \\

    private SongLaneStruct[] parseLanes(ArpgArrayStruct lanesArpg, String owner, File file) {

        if (lanesArpg.size() > EngineSetting.SONG_LANE_COUNT)
            throwException(owner + " has " + lanesArpg.size() + " lanes, more than " + EngineSetting.SONG_LANE_COUNT
                    + ". File: " + file.getName());

        SongLaneStruct[] lanes = new SongLaneStruct[lanesArpg.size()];

        for (int i = 0; i < lanes.length; i++) {

            ArpgObjectStruct laneArpg = lanesArpg.get(i).getAsObject();
            float volume = ArpgUtility.getFloat(laneArpg, "volume", EngineSetting.DEFAULT_SONG_LANE_VOLUME);
            float pan = ArpgUtility.getFloat(laneArpg, "pan", EngineSetting.DEFAULT_SONG_LANE_PAN);

            if (volume < 0f || volume > 1f || pan < -1f || pan > 1f)
                throwException(owner + " lane " + i + " plays at a \"volume\" outside 0 to 1 or a \"pan\" outside -1 "
                        + "to 1. File: " + file.getName());

            lanes[i] = new SongLaneStruct(
                    ArpgUtility.getString(laneArpg, "name", EngineSetting.SONG_LANE_NAME_NONE),
                    volume,
                    pan,
                    ArpgUtility.getBoolean(laneArpg, "muted", EngineSetting.DEFAULT_SONG_LANE_MUTED),
                    ArpgUtility.getBoolean(laneArpg, "solo", EngineSetting.DEFAULT_SONG_LANE_SOLO));
        }

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
