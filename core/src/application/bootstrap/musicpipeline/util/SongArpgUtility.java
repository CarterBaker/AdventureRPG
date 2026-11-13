package application.bootstrap.musicpipeline.util;

import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

public class SongArpgUtility extends EngineUtility {

    /*
     * The one codec between a song's clips and ARPG, used by SongBuilder and
     * by the editor alike. A clip is read with every rule the game holds it
     * to — its kind, a lane the song has, a start, a length of at least one
     * tick, an offset into its source, a volume of 0 to 1 and a transpose of
     * up to SONG_CLIP_MAX_TRANSPOSE semitones either way — and what its kind
     * names: a notes clip's instrument and notes, a song clip's song, a sound
     * clip's sound. Notes are a flat array of four ints to a note, each note's
     * pitch and velocity in range and the note inside its clip's length, put
     * in order of their start. Writing gives back the same tree, so a clip
     * read and written again is unchanged. Never held — all methods static.
     */

    // Internal
    private static final int[] NO_NOTES = new int[0];

    // Clips \\

    public static SongClipStruct readClip(ArpgObjectStruct clipArpg, int laneCount, String owner) {

        SongClipKind kind = ArpgUtility.toEnum(ArpgUtility.validateString(clipArpg, "kind"), SongClipKind.class);
        int lane = ArpgUtility.validateInt(clipArpg, "lane");
        int start = ArpgUtility.validateInt(clipArpg, "start");
        int length = ArpgUtility.validateInt(clipArpg, "length");
        int offset = ArpgUtility.getInt(clipArpg, "offset", EngineSetting.DEFAULT_SONG_CLIP_OFFSET);
        float volume = ArpgUtility.getFloat(clipArpg, "volume", EngineSetting.DEFAULT_SONG_CLIP_VOLUME);
        int transpose = ArpgUtility.getInt(clipArpg, "transpose", EngineSetting.DEFAULT_SONG_CLIP_TRANSPOSE);

        if (lane < 0 || lane >= laneCount)
            throwException(owner + " lies on lane " + lane + ", but the song has lanes 0 to " + (laneCount - 1)
                    + ".");

        if (start < 0 || offset < 0)
            throwException(owner + " starts at tick " + start + " from an offset of " + offset
                    + " ticks; neither can be negative.");

        if (length < 1)
            throwException(owner + " lasts " + length + " ticks; a clip lasts at least one.");

        if (volume < 0f || volume > 1f)
            throwException(owner + " plays at a \"volume\" outside 0 to 1.");

        if (Math.abs(transpose) > EngineSetting.SONG_CLIP_MAX_TRANSPOSE)
            throwException(owner + " transposes by " + transpose + " semitones, more than "
                    + EngineSetting.SONG_CLIP_MAX_TRANSPOSE + " either way.");

        return new SongClipStruct(
                kind,
                lane,
                start,
                length,
                offset,
                ArpgUtility.getBoolean(clipArpg, "muted", EngineSetting.DEFAULT_SONG_CLIP_MUTED),
                volume,
                transpose,
                kind == SongClipKind.NOTES
                        ? validateName(clipArpg, "instrument", owner)
                        : EngineSetting.SONG_REFERENCE_NONE,
                kind == SongClipKind.NOTES
                        ? readNotes(ArpgUtility.validateArray(clipArpg, "notes"), length, owner)
                        : NO_NOTES,
                kind == SongClipKind.SONG ? validateName(clipArpg, "song", owner) : EngineSetting.SONG_REFERENCE_NONE,
                kind == SongClipKind.SOUND
                        ? validateName(clipArpg, "sound", owner)
                        : EngineSetting.SONG_REFERENCE_NONE);
    }

    public static ArpgObjectStruct writeClip(SongClipStruct clip) {

        ArpgObjectStruct clipArpg = new ArpgObjectStruct();

        clipArpg.addProperty("kind", ArpgUtility.toEnumName(clip.getKind()));
        clipArpg.addProperty("lane", clip.getLane());
        clipArpg.addProperty("start", clip.getStart());
        clipArpg.addProperty("length", clip.getLength());
        clipArpg.addProperty("offset", clip.getOffset());
        clipArpg.addProperty("muted", clip.isMuted());
        clipArpg.addProperty("volume", clip.getVolume());
        clipArpg.addProperty("transpose", clip.getTranspose());

        switch (clip.getKind()) {
            case NOTES -> {
                clipArpg.addProperty("instrument", clip.getInstrumentName());
                clipArpg.add("notes", writeNotes(clip.getNotes()));
            }
            case SONG -> clipArpg.addProperty("song", clip.getSongName());
            case SOUND -> clipArpg.addProperty("sound", clip.getSoundName());
        }

        return clipArpg;
    }

    private static String validateName(ArpgObjectStruct clipArpg, String key, String owner) {

        String name = ArpgUtility.validateString(clipArpg, key);

        if (name.isEmpty())
            throwException(owner + " names no \"" + key + "\".");

        return name;
    }

    // Notes \\

    // A clip's notes, every one in range and inside the clip's length, in order of their start
    public static int[] readNotes(ArpgArrayStruct notesArpg, int clipLength, String owner) {

        if (notesArpg.size() % EngineSetting.SONG_NOTE_STRIDE != 0)
            throwException(owner + " holds " + notesArpg.size() + " note values; each note is "
                    + EngineSetting.SONG_NOTE_STRIDE + " (start, length, pitch, velocity).");

        int[] notes = new int[notesArpg.size()];

        for (int i = 0; i < notes.length; i++)
            notes[i] = notesArpg.get(i).getAsInt();

        for (int at = 0; at < notes.length; at += EngineSetting.SONG_NOTE_STRIDE)
            validateNote(notes, at, clipLength, owner + " note " + at / EngineSetting.SONG_NOTE_STRIDE);

        sortNotes(notes);
        return notes;
    }

    public static ArpgArrayStruct writeNotes(int[] notes) {

        ArpgArrayStruct notesArpg = new ArpgArrayStruct();

        for (int i = 0; i < notes.length; i++)
            notesArpg.add(notes[i]);

        return notesArpg;
    }

    private static void validateNote(int[] notes, int at, int clipLength, String owner) {

        int start = notes[at + EngineSetting.SONG_NOTE_START];
        int length = notes[at + EngineSetting.SONG_NOTE_LENGTH];
        int pitch = notes[at + EngineSetting.SONG_NOTE_PITCH];
        int velocity = notes[at + EngineSetting.SONG_NOTE_VELOCITY];

        if (start < 0 || length < 1 || start + length > clipLength)
            throwException(owner + " runs from tick " + start + " for " + length + " ticks, outside its clip's "
                    + clipLength + " ticks.");

        if (pitch < EngineSetting.NOTE_PITCH_MIN || pitch > EngineSetting.NOTE_PITCH_MAX)
            throwException(owner + " plays pitch " + pitch + ", outside " + EngineSetting.NOTE_PITCH_MIN + " to "
                    + EngineSetting.NOTE_PITCH_MAX + ".");

        if (velocity < EngineSetting.NOTE_VELOCITY_MIN || velocity > EngineSetting.NOTE_VELOCITY_MAX)
            throwException(owner + " plays at velocity " + velocity + ", outside " + EngineSetting.NOTE_VELOCITY_MIN
                    + " to " + EngineSetting.NOTE_VELOCITY_MAX + ".");
    }

    // Packed notes put in order of their start, then of their pitch, notes already in order left where they stand
    public static void sortNotes(int[] notes) {

        int stride = EngineSetting.SONG_NOTE_STRIDE;

        for (int at = stride; at < notes.length; at += stride) {

            int start = notes[at + EngineSetting.SONG_NOTE_START];
            int length = notes[at + EngineSetting.SONG_NOTE_LENGTH];
            int pitch = notes[at + EngineSetting.SONG_NOTE_PITCH];
            int velocity = notes[at + EngineSetting.SONG_NOTE_VELOCITY];
            int to = at;

            while (to > 0 && isAfter(notes, to - stride, start, pitch)) {
                System.arraycopy(notes, to - stride, notes, to, stride);
                to -= stride;
            }

            notes[to + EngineSetting.SONG_NOTE_START] = start;
            notes[to + EngineSetting.SONG_NOTE_LENGTH] = length;
            notes[to + EngineSetting.SONG_NOTE_PITCH] = pitch;
            notes[to + EngineSetting.SONG_NOTE_VELOCITY] = velocity;
        }
    }

    private static boolean isAfter(int[] notes, int at, int start, int pitch) {

        int otherStart = notes[at + EngineSetting.SONG_NOTE_START];

        return otherStart > start || (otherStart == start && notes[at + EngineSetting.SONG_NOTE_PITCH] > pitch);
    }
}
