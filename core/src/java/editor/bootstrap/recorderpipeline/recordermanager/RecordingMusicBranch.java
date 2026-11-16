package editor.bootstrap.recorderpipeline.recordermanager;

import java.io.File;

import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.song.SongData;
import application.bootstrap.musicpipeline.song.SongLaneStruct;
import application.bootstrap.musicpipeline.song.SongTempoStruct;
import application.bootstrap.musicpipeline.songmanager.SongManager;
import application.bootstrap.musicpipeline.util.SongArpgUtility;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingMusicBranch extends BranchPackage {

    /*
     * A media file as the game's own song, written by the song codec alone
     * so the game reads exactly what the editor exports. Every clip's span is
     * turned into ticks of the file's tempo map — a notes clip from its start
     * with no offset, holding only the notes heard within it, each cut to it,
     * a song clip as its song, a take of one sound alone as a sound clip —
     * and every lane is written. A take of the world and a nested media file
     * have no place in a song and are left out. The same song, laid out from
     * the clips that can play, is what MIDI export and the render read.
     */

    // Internal
    private SoundManager soundManager;
    private SongManager songManager;
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
        this.songManager = get(SongManager.class);
        this.recordingSoundBranch = get(RecordingSoundBranch.class);
    }

    // Write \\

    // The media file exported as its song, every clip a song can hold by name whether or not it can play now
    boolean writeSong(File file, RecordingInstance recording) {
        return ArpgUtility.tryWriteObject(file, SongArpgUtility.writeSong(toSong(recording, false)));
    }

    // Song \\

    // The media file as a song: its tempo map, meter, key, lanes and clips; with playable alone, only the clips that
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
        if (clip.getSong() != null)
            written.resolveSong(songManager.getSongHandleFromSongName(clip.getSourceName()));

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
