package editor.bootstrap.recorderpipeline.recordermanager;

import java.io.File;

import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.song.SongData;
import application.bootstrap.musicpipeline.song.SongHandle;
import application.bootstrap.musicpipeline.song.SongLaneStruct;
import application.bootstrap.musicpipeline.song.SongTempoStruct;
import application.bootstrap.musicpipeline.util.NoteScale;
import application.bootstrap.musicpipeline.util.SongArpgUtility;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingKind;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingMusicBranch extends BranchPackage {

    /*
     * A Music file is a song, read into the recorder and written back
     * through the song codec alone, so the game reads exactly what the
     * editor saves. Each of the song's clips becomes a clip of the timeline
     * where its ticks fall on the song's tempo map: a notes clip keeps its
     * notes as its codec clip, a song clip its song, volume and transpose, and
     * a sound clip becomes the bin's take of that sound alone; every lane's
     * mix is kept on the recording's lanes. Written back, every clip's span
     * is turned into ticks again — a notes clip from its start with no
     * offset, holding only the notes heard within it, each cut to it — and
     * every lane is written. The same song, laid out from the clips that can
     * play, is what MIDI export and the render read. A file that cannot be
     * read is logged and comes back null.
     */

    // Internal
    private SoundManager soundManager;
    private RecordingSoundBranch recordingSoundBranch;

    // Scratch
    private IntArrayList noteScratch;
    private ObjectArrayList<SongClipStruct> clipScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.noteScratch = new IntArrayList();
        this.clipScratch = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
        this.recordingSoundBranch = get(RecordingSoundBranch.class);
    }

    // Read \\

    RecordingInstance read(File file, String songName) {

        try {
            return parse(ArpgUtility.readObject(file), songName);
        } catch (InternalException e) {
            errorLog(EditorSetting.RECORDER_MESSAGE_UNREADABLE + file.getAbsolutePath(), e);
            return null;
        }
    }

    private RecordingInstance parse(ArpgObjectStruct songArpg, String songName) {

        String owner = EditorSetting.RECORDER_MUSIC_OWNER_PREFIX + songName;
        SongTempoStruct tempo = SongArpgUtility.readTempo(ArpgUtility.validateArray(songArpg, "tempos"), owner);
        int beatsPerBar = ArpgUtility.getInt(songArpg, "beats_per_bar", EngineSetting.DEFAULT_SONG_BEATS_PER_BAR);
        int beatUnit = ArpgUtility.getInt(songArpg, "beat_unit", EngineSetting.DEFAULT_SONG_BEAT_UNIT);
        int keyRoot = ArpgUtility.getInt(songArpg, "key_root", EngineSetting.DEFAULT_SONG_KEY_ROOT);
        ArpgArrayStruct lanesArpg = ArpgUtility.validateArray(songArpg, "lanes");
        ArpgArrayStruct clipsArpg = ArpgUtility.validateArray(songArpg, "clips");

        SongArpgUtility.validateMeter(beatsPerBar, beatUnit, owner);
        SongArpgUtility.validateKeyRoot(keyRoot, owner);

        if (lanesArpg.size() > EngineSetting.SONG_LANE_COUNT)
            throwException(owner + " has " + lanesArpg.size() + " lanes, more than " + EngineSetting.SONG_LANE_COUNT
                    + ".");

        RecordingInstance recording = create(RecordingInstance.class);
        recording.constructor(
                RecordingKind.MUSIC,
                songName,
                EditorSetting.RECORDER_MUSIC_WORLD_NONE,
                tempo,
                beatsPerBar,
                beatUnit,
                keyRoot,
                ArpgUtility.getEnum(songArpg, "key_scale", NoteScale.class, NoteScale.MAJOR),
                new RecordingExportStruct());
        recording.setDisplayName(ArpgUtility.getString(
                songArpg, "display_name", EngineSetting.SONG_DISPLAY_NAME_NONE));

        for (int i = 0; i < lanesArpg.size(); i++)
            recording.getLane(i).readFrom(SongArpgUtility.readLane(lanesArpg.get(i).getAsObject(),
                    owner + EditorSetting.RECORDER_MUSIC_LANE_OWNER + i));

        for (int i = 0; i < clipsArpg.size(); i++) {

            SongClipStruct songClip = SongArpgUtility.readClip(clipsArpg.get(i).getAsObject(),
                    Math.max(1, lanesArpg.size()), owner + EditorSetting.RECORDER_FILE_CLIP_OWNER + i);
            RecordingClipStruct clip = toRecordingClip(recording, songClip);

            if (clip == null)
                continue;

            clip.setMuted(songClip.isMuted());
            recording.addClip(clip);
        }

        return recording;
    }

    // The song's clip laid on the timeline where its ticks fall: its source's first tick at its start less its
    // offset, heard from its start to its end; null for a sound the game no longer has
    private RecordingClipStruct toRecordingClip(RecordingInstance recording, SongClipStruct songClip) {

        SongTempoStruct tempo = recording.getTempo();
        int startTick = songClip.getStart();
        float start = (float) tempo.toSeconds(startTick);
        float origin = (float) tempo.toSeconds(startTick - songClip.getOffset());
        float end = (float) tempo.toSeconds(songClip.getEnd());

        return switch (songClip.getKind()) {
            case NOTES -> RecordingClipStruct.ofNotes(new SongClipStruct(
                    SongClipKind.NOTES,
                    0,
                    0,
                    songClip.getOffset() + songClip.getLength(),
                    EngineSetting.DEFAULT_SONG_CLIP_OFFSET,
                    false,
                    songClip.getVolume(),
                    songClip.getTranspose(),
                    songClip.getInstrumentName(),
                    songClip.getNotes(),
                    EngineSetting.SONG_REFERENCE_NONE,
                    EngineSetting.SONG_REFERENCE_NONE), start - origin, end - origin, songClip.getLane(), start);
            case SONG -> RecordingClipStruct.ofSongClip(songClip, start - origin, end - origin, songClip.getLane(),
                    start);
            case SOUND -> toSoundClip(recording, songClip, start - origin, end - origin, start);
        };
    }

    private RecordingClipStruct toSoundClip(
            RecordingInstance recording,
            SongClipStruct songClip,
            float inPoint,
            float outPoint,
            float start) {

        int soundID = soundManager.findSoundID(songClip.getSoundName());

        if (soundID == EngineSetting.INDEX_NOT_FOUND) {
            errorLog(EditorSetting.RECORDER_MESSAGE_SOUND_MISSING + songClip.getSoundName());
            return null;
        }

        RecordingTakeStruct take = recording.getTakes().get(recordingSoundBranch.requireSoundTake(
                recording, soundManager.getSoundHandleFromSoundID(soundID)));

        return RecordingClipStruct.ofTake(take, inPoint, outPoint, songClip.getLane(), start,
                RecorderChannel.SOUND.getBit());
    }

    // Write \\

    // The Music file written as its song, every clip it holds by name whether or not it can play now
    boolean write(File file, RecordingInstance recording) {
        return ArpgUtility.tryWriteObject(file, SongArpgUtility.writeSong(toSong(recording, false)));
    }

    // Song \\

    // The Music file as a song: its tempo map, meter, key, lanes and clips; with playable alone, only the clips that
    // can sound now — a notes clip's instrument loaded, a song clip's song found
    SongData toSong(RecordingInstance recording, boolean playable) {

        SongLaneStruct[] lanes = new SongLaneStruct[recording.getLaneCount()];

        for (int i = 0; i < lanes.length; i++)
            lanes[i] = recording.getLane(i).toSongLane();

        clipScratch.clear();

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            SongClipStruct songClip = toSongClip(recording, clips.get(i));

            if (songClip != null && (!playable || isPlayable(songClip)))
                clipScratch.add(songClip);
        }

        return new SongData(
                recording.getRecordingName(),
                EngineSetting.SONG_NONE,
                recording.getDisplayName(),
                recording.getTempo(),
                recording.getBeatsPerBar(),
                recording.getBeatUnit(),
                recording.getKeyRoot(),
                recording.getKeyScale(),
                lanes,
                clipScratch.toArray(new SongClipStruct[clipScratch.size()]));
    }

    private boolean isPlayable(SongClipStruct songClip) {
        return switch (songClip.getKind()) {
            case NOTES -> songClip.getInstrumentID() != EngineSetting.INSTRUMENT_NONE;
            case SONG -> songClip.getSongHandle() != null;
            case SOUND -> soundManager.hasSoundFile(songClip.getSoundName());
        };
    }

    // The timeline's clip as the song codec writes it, its span turned into ticks of the tempo map; null for a clip a
    // song cannot hold
    private SongClipStruct toSongClip(RecordingInstance recording, RecordingClipStruct clip) {

        SongTempoStruct tempo = recording.getTempo();
        int startTick = toTick(tempo, clip.getStart());
        int length = Math.max(1, toTick(tempo, clip.getEnd()) - startTick);
        int offset = Math.max(0, startTick - toTick(tempo, clip.getOrigin()));
        SongClipStruct codec = clip.getSongClip();

        return switch (clip.getSourceKind()) {
            case NOTES -> toNotesClip(clip, codec, startTick, length, offset);
            case SONG -> toSongOfClip(clip, codec, startTick, length, offset);
            case TAKE -> toSoundOfClip(clip, startTick, length, offset);
            case RECORDING -> null;
        };
    }

    // A notes clip from its start with no offset, holding only the notes heard within it, each cut to it
    private SongClipStruct toNotesClip(
            RecordingClipStruct clip,
            SongClipStruct codec,
            int startTick,
            int length,
            int offset) {

        noteScratch.clear();

        for (int note = 0; note < codec.getNoteCount(); note++) {

            int noteStart = codec.getNoteStart(note) - offset;
            int noteEnd = Math.min(noteStart + codec.getNoteLength(note), length);

            noteStart = Math.max(0, noteStart);

            if (noteEnd <= noteStart)
                continue;

            noteScratch.add(noteStart);
            noteScratch.add(noteEnd - noteStart);
            noteScratch.add(codec.getNotePitch(note));
            noteScratch.add(codec.getNoteVelocity(note));
        }

        int[] notes = noteScratch.toIntArray();

        SongArpgUtility.sortNotes(notes);

        SongClipStruct written = new SongClipStruct(
                SongClipKind.NOTES,
                clip.getLane(),
                startTick,
                length,
                EngineSetting.DEFAULT_SONG_CLIP_OFFSET,
                clip.isMuted(),
                codec.getVolume(),
                codec.getTranspose(),
                codec.getInstrumentName(),
                notes,
                EngineSetting.SONG_REFERENCE_NONE,
                EngineSetting.SONG_REFERENCE_NONE);

        written.resolveInstrument(codec.getInstrumentID());
        return written;
    }

    private SongClipStruct toSongOfClip(
            RecordingClipStruct clip,
            SongClipStruct codec,
            int startTick,
            int length,
            int offset) {

        SongClipStruct written = new SongClipStruct(
                SongClipKind.SONG,
                clip.getLane(),
                startTick,
                length,
                offset,
                clip.isMuted(),
                codec != null ? codec.getVolume() : EngineSetting.DEFAULT_SONG_CLIP_VOLUME,
                codec != null ? codec.getTranspose() : EngineSetting.DEFAULT_SONG_CLIP_TRANSPOSE,
                EngineSetting.SONG_REFERENCE_NONE,
                SongArpgUtility.NO_NOTES,
                clip.getSourceName(),
                EngineSetting.SONG_REFERENCE_NONE);
        SongHandle handle = clip.getSong() != null && codec != null ? clip.getSong().getClip(0).getSongHandle() : null;

        if (handle != null)
            written.resolveSong(handle);

        return written;
    }

    // A take of one sound alone as a sound clip; null for any other take, which a song cannot hold
    private SongClipStruct toSoundOfClip(RecordingClipStruct clip, int startTick, int length, int offset) {

        String soundName = recordingSoundBranch.findSoleSound(clip.getTake());

        if (soundName == null)
            return null;

        return new SongClipStruct(
                SongClipKind.SOUND,
                clip.getLane(),
                startTick,
                length,
                offset,
                clip.isMuted(),
                EngineSetting.DEFAULT_SONG_CLIP_VOLUME,
                EngineSetting.DEFAULT_SONG_CLIP_TRANSPOSE,
                EngineSetting.SONG_REFERENCE_NONE,
                SongArpgUtility.NO_NOTES,
                EngineSetting.SONG_REFERENCE_NONE,
                soundName);
    }

    // Utility \\

    private int toTick(SongTempoStruct tempo, float seconds) {
        return (int) Math.round(tempo.toTicks(seconds));
    }
}
