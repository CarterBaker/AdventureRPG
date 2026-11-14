package application.bootstrap.musicpipeline.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.song.SongCursorStruct;
import application.bootstrap.musicpipeline.song.SongData;
import application.bootstrap.musicpipeline.song.SongLaneStruct;
import application.bootstrap.musicpipeline.song.SongTempoStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.UtilityPackage.InternalException;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SongMidiUtility extends EngineUtility {

    /*
     * Reads and writes Standard MIDI Files as songs, through javax.sound.midi.
     * Reading takes a type 0 or 1 file at any resolution, rescaled to
     * SONG_TICKS_PER_BEAT: its tempo changes, its first time and key
     * signatures, and a lane holding one notes clip from tick 0 for every
     * track and channel that plays notes, named after its track and set by its
     * first volume and pan controls. A part plays on the instrument whose
     * midi_program matches the program its channel holds at its first note,
     * channel 10 on the instrument marked percussion, and anything else on
     * SONG_DEFAULT_INSTRUMENT. Writing gives a type 1 file: a first track
     * holding the tempo map and signatures, then a track for every lane with
     * a channel for each instrument it plays, exactly the notes the song
     * plays — nested song clips laid out flat through SongFlattenUtility,
     * muted clips and lanes left silent — and no sound clips, which MIDI
     * cannot hold; it hands back how many it left out. A file that cannot be
     * read or written fails with a catchable InternalException naming the
     * reason. Never held — all methods static.
     */

    // Read \\

    public static SongData read(File file, InstrumentManager instrumentManager) {

        Sequence sequence = openSequence(file);
        int resolution = sequence.getResolution();
        Track[] tracks = sequence.getTracks();

        Int2FloatRBTreeMap tick2Bpm = new Int2FloatRBTreeMap();
        Int2ObjectRBTreeMap<byte[]> tick2Meter = new Int2ObjectRBTreeMap<>();
        Int2ObjectRBTreeMap<byte[]> tick2Key = new Int2ObjectRBTreeMap<>();
        ObjectArrayList<Int2IntRBTreeMap> channel2Programs = new ObjectArrayList<>();

        for (int channel = 0; channel < EngineSetting.SONG_MIDI_CHANNEL_COUNT; channel++)
            channel2Programs.add(new Int2IntRBTreeMap());

        for (int i = 0; i < tracks.length; i++)
            readSettings(tracks[i], resolution, tick2Bpm, tick2Meter, tick2Key, channel2Programs);

        if (!tick2Bpm.containsKey(0))
            tick2Bpm.put(0, EngineSetting.SONG_MIDI_DEFAULT_BPM);

        ObjectArrayList<SongLaneStruct> lanes = new ObjectArrayList<>();
        ObjectArrayList<SongClipStruct> clips = new ObjectArrayList<>();
        String displayName = FileUtility.getFileName(file);

        for (int i = 0; i < tracks.length; i++) {

            String trackName = findTrackName(tracks[i]);

            if (i == 0 && tracks.length > 1 && trackName != null && !playsNotes(tracks[i]))
                displayName = trackName;

            readParts(tracks[i], i, trackName, resolution, channel2Programs, instrumentManager, lanes, clips, file);
        }

        byte[] meter = tick2Meter.isEmpty() ? null : tick2Meter.get(tick2Meter.firstIntKey());
        byte[] key = tick2Key.isEmpty() ? null : tick2Key.get(tick2Key.firstIntKey());

        return new SongData(
                FileUtility.getFileName(file),
                EngineSetting.SONG_NONE,
                displayName,
                toTempo(tick2Bpm),
                meter != null
                        ? Math.clamp(meter[0], 1, EngineSetting.SONG_BEATS_PER_BAR_MAX)
                        : EngineSetting.DEFAULT_SONG_BEATS_PER_BAR,
                meter != null
                        ? 1 << Math.clamp(meter[1], 0, Integer.numberOfTrailingZeros(EngineSetting.SONG_BEAT_UNIT_MAX))
                        : EngineSetting.DEFAULT_SONG_BEAT_UNIT,
                key != null ? toKeyRoot(key[0], key[1]) : EngineSetting.DEFAULT_SONG_KEY_ROOT,
                key != null && key[1] == EngineSetting.SONG_MIDI_KEY_MINOR ? NoteScale.MINOR : NoteScale.MAJOR,
                lanes.toArray(new SongLaneStruct[0]),
                clips.toArray(new SongClipStruct[0]));
    }

    private static Sequence openSequence(File file) {

        Sequence sequence;

        try {
            sequence = MidiSystem.getSequence(file);
        } catch (InvalidMidiDataException | IOException e) {
            throw new InternalException("Failed to read MIDI file: " + file.getAbsolutePath() + " (" + e.getMessage()
                    + ")", e);
        }

        if (sequence.getDivisionType() != Sequence.PPQ)
            throw new InternalException("MIDI file counts its time in SMPTE frames rather than beats: "
                    + file.getAbsolutePath());

        return sequence;
    }

    // Every tempo change, time and key signature and program change a track holds, the first of a tick kept
    private static void readSettings(
            Track track,
            int resolution,
            Int2FloatRBTreeMap tick2Bpm,
            Int2ObjectRBTreeMap<byte[]> tick2Meter,
            Int2ObjectRBTreeMap<byte[]> tick2Key,
            ObjectArrayList<Int2IntRBTreeMap> channel2Programs) {

        for (int i = 0; i < track.size(); i++) {

            MidiEvent event = track.get(i);
            MidiMessage message = event.getMessage();
            int tick = rescale(event.getTick(), resolution);

            if (message instanceof ShortMessage shortMessage
                    && shortMessage.getCommand() == ShortMessage.PROGRAM_CHANGE)
                channel2Programs.get(shortMessage.getChannel()).put(tick, shortMessage.getData1());

            if (!(message instanceof MetaMessage metaMessage))
                continue;

            byte[] data = metaMessage.getData();

            if (metaMessage.getType() == EngineSetting.SONG_MIDI_META_TEMPO
                    && data.length >= EngineSetting.SONG_MIDI_TEMPO_BYTES)
                tick2Bpm.put(tick, toBpm(data));

            if (metaMessage.getType() == EngineSetting.SONG_MIDI_META_TIME_SIGNATURE
                    && data.length >= EngineSetting.SONG_MIDI_SIGNATURE_BYTES)
                tick2Meter.putIfAbsent(tick, data);

            if (metaMessage.getType() == EngineSetting.SONG_MIDI_META_KEY_SIGNATURE
                    && data.length >= EngineSetting.SONG_MIDI_SIGNATURE_BYTES)
                tick2Key.putIfAbsent(tick, data);
        }
    }

    // A lane and a notes clip for every channel of the track that plays notes, in channel order
    private static void readParts(
            Track track,
            int trackIndex,
            String trackName,
            int resolution,
            ObjectArrayList<Int2IntRBTreeMap> channel2Programs,
            InstrumentManager instrumentManager,
            ObjectArrayList<SongLaneStruct> lanes,
            ObjectArrayList<SongClipStruct> clips,
            File file) {

        ObjectArrayList<IntArrayList> channel2Notes = new ObjectArrayList<>();
        int[] channel2Volume = new int[EngineSetting.SONG_MIDI_CHANNEL_COUNT];
        int[] channel2Pan = new int[EngineSetting.SONG_MIDI_CHANNEL_COUNT];

        for (int channel = 0; channel < EngineSetting.SONG_MIDI_CHANNEL_COUNT; channel++) {
            channel2Notes.add(new IntArrayList());
            channel2Volume[channel] = EngineSetting.INDEX_NOT_FOUND;
            channel2Pan[channel] = EngineSetting.INDEX_NOT_FOUND;
        }

        readNotes(track, resolution, channel2Notes, channel2Volume, channel2Pan);

        int parts = 0;

        for (int channel = 0; channel < EngineSetting.SONG_MIDI_CHANNEL_COUNT; channel++)
            if (!channel2Notes.get(channel).isEmpty())
                parts++;

        for (int channel = 0; channel < EngineSetting.SONG_MIDI_CHANNEL_COUNT; channel++) {

            IntArrayList notes = channel2Notes.get(channel);

            if (notes.isEmpty())
                continue;

            if (lanes.size() == EngineSetting.SONG_LANE_COUNT)
                throw new InternalException("MIDI file plays more than " + EngineSetting.SONG_LANE_COUNT
                        + " parts (a part is a track and channel that plays notes): " + file.getAbsolutePath());

            String laneName = trackName != null
                    ? trackName
                    : String.format(EngineSetting.SONG_MIDI_LANE_NAME_FORMAT, trackIndex + 1);

            if (parts > 1)
                laneName = String.format(EngineSetting.SONG_MIDI_CHANNEL_NAME_FORMAT, laneName, channel + 1);

            int[] packed = notes.toIntArray();
            SongArpgUtility.sortNotes(packed);

            InstrumentHandle instrument = instrumentManager.getInstrumentHandleFromInstrumentID(
                    resolveInstrument(channel, packed[EngineSetting.SONG_NOTE_START], channel2Programs,
                            instrumentManager));
            SongClipStruct clip = new SongClipStruct(
                    SongClipKind.NOTES,
                    lanes.size(),
                    0,
                    findEnd(packed),
                    EngineSetting.DEFAULT_SONG_CLIP_OFFSET,
                    EngineSetting.DEFAULT_SONG_CLIP_MUTED,
                    EngineSetting.DEFAULT_SONG_CLIP_VOLUME,
                    EngineSetting.DEFAULT_SONG_CLIP_TRANSPOSE,
                    instrument.getInstrumentName(),
                    packed,
                    EngineSetting.SONG_REFERENCE_NONE,
                    EngineSetting.SONG_REFERENCE_NONE);

            clip.resolveInstrument(instrument.getInstrumentID());
            clips.add(clip);
            lanes.add(new SongLaneStruct(
                    laneName,
                    channel2Volume[channel] != EngineSetting.INDEX_NOT_FOUND
                            ? (float) channel2Volume[channel] / EngineSetting.SONG_MIDI_CONTROL_MAX
                            : EngineSetting.DEFAULT_SONG_LANE_VOLUME,
                    channel2Pan[channel] != EngineSetting.INDEX_NOT_FOUND
                            ? toPan(channel2Pan[channel])
                            : EngineSetting.DEFAULT_SONG_LANE_PAN,
                    EngineSetting.DEFAULT_SONG_LANE_MUTED,
                    EngineSetting.DEFAULT_SONG_LANE_SOLO));
        }
    }

    // Every note the track plays, packed by channel; a note sounded again before its end ends there
    private static void readNotes(
            Track track,
            int resolution,
            ObjectArrayList<IntArrayList> channel2Notes,
            int[] channel2Volume,
            int[] channel2Pan) {

        int[][] channel2OpenTicks = new int[EngineSetting.SONG_MIDI_CHANNEL_COUNT][EngineSetting.NOTE_PITCH_COUNT];
        int[][] channel2OpenVelocities = new int[EngineSetting.SONG_MIDI_CHANNEL_COUNT][EngineSetting.NOTE_PITCH_COUNT];

        for (int channel = 0; channel < EngineSetting.SONG_MIDI_CHANNEL_COUNT; channel++)
            Arrays.fill(channel2OpenTicks[channel], EngineSetting.INDEX_NOT_FOUND);

        for (int i = 0; i < track.size(); i++) {

            MidiEvent event = track.get(i);

            if (!(event.getMessage() instanceof ShortMessage message))
                continue;

            int channel = message.getChannel();
            int tick = rescale(event.getTick(), resolution);
            int command = message.getCommand();
            boolean noteOn = command == ShortMessage.NOTE_ON && message.getData2() > 0;
            boolean noteOff = command == ShortMessage.NOTE_OFF
                    || (command == ShortMessage.NOTE_ON && message.getData2() == 0);

            if (command == ShortMessage.CONTROL_CHANGE)
                readControl(message, channel2Volume, channel2Pan);

            if (!noteOn && !noteOff)
                continue;

            int pitch = message.getData1();

            closeNote(channel2Notes.get(channel), channel2OpenTicks[channel], channel2OpenVelocities[channel], pitch,
                    tick);

            if (noteOn) {
                channel2OpenTicks[channel][pitch] = tick;
                channel2OpenVelocities[channel][pitch] = message.getData2();
            }
        }

        int end = rescale(track.ticks(), resolution);

        for (int channel = 0; channel < EngineSetting.SONG_MIDI_CHANNEL_COUNT; channel++)
            for (int pitch = 0; pitch < EngineSetting.NOTE_PITCH_COUNT; pitch++)
                closeNote(channel2Notes.get(channel), channel2OpenTicks[channel], channel2OpenVelocities[channel],
                        pitch, end);
    }

    // The channel's first volume and pan, the levels its part is set at
    private static void readControl(ShortMessage message, int[] channel2Volume, int[] channel2Pan) {

        int channel = message.getChannel();

        if (message.getData1() == EngineSetting.SONG_MIDI_CONTROL_VOLUME
                && channel2Volume[channel] == EngineSetting.INDEX_NOT_FOUND)
            channel2Volume[channel] = message.getData2();

        if (message.getData1() == EngineSetting.SONG_MIDI_CONTROL_PAN
                && channel2Pan[channel] == EngineSetting.INDEX_NOT_FOUND)
            channel2Pan[channel] = message.getData2();
    }

    // The pitch's open note, if it has one, ended at the tick and packed, at least one tick long
    private static void closeNote(IntArrayList notes, int[] openTicks, int[] openVelocities, int pitch, int tick) {

        int start = openTicks[pitch];

        if (start == EngineSetting.INDEX_NOT_FOUND)
            return;

        notes.add(start);
        notes.add(Math.max(1, tick - start));
        notes.add(pitch);
        notes.add(openVelocities[pitch]);
        openTicks[pitch] = EngineSetting.INDEX_NOT_FOUND;
    }

    // The instrument a channel plays its part on: the percussion one on channel 10, else the one whose program
    // the channel holds at the part's first note, else SONG_DEFAULT_INSTRUMENT
    private static short resolveInstrument(
            int channel,
            int firstTick,
            ObjectArrayList<Int2IntRBTreeMap> channel2Programs,
            InstrumentManager instrumentManager) {

        Int2IntSortedMap programs = channel2Programs.get(channel).headMap(firstTick + 1);
        int program = programs.isEmpty()
                ? EngineSetting.SONG_MIDI_PROGRAM_DEFAULT
                : programs.get(programs.lastIntKey());
        short instrumentID = channel == EngineSetting.SONG_MIDI_PERCUSSION_CHANNEL
                ? instrumentManager.getPercussionInstrumentID()
                : instrumentManager.getInstrumentIDFromMidiProgram(program);

        return instrumentID != EngineSetting.INSTRUMENT_NONE
                ? instrumentID
                : instrumentManager.getInstrumentIDFromInstrumentName(EngineSetting.SONG_DEFAULT_INSTRUMENT);
    }

    // Write \\

    // The song written as a type 1 file; how many sound clips it left out
    public static int write(SongData song, File file, InstrumentManager instrumentManager) {

        ObjectArrayList<SongCursorStruct> cursors = new ObjectArrayList<>();
        int count = SongFlattenUtility.flatten(song, EngineSetting.SONG_LANE_ALL, cursors);
        Sequence sequence = createSequence();

        writeSettings(sequence.createTrack(), song);

        Track[] laneTracks = new Track[song.getLaneCount()];
        Int2IntOpenHashMap part2Channel = new Int2IntOpenHashMap();
        int soundClips = 0;

        for (int i = 0; i < laneTracks.length; i++) {
            laneTracks[i] = sequence.createTrack();
            addMeta(laneTracks[i], EngineSetting.SONG_MIDI_META_TRACK_NAME,
                    toLaneName(song, i).getBytes(StandardCharsets.UTF_8), 0);
        }

        for (int i = 0; i < count; i++) {

            SongCursorStruct cursor = cursors.get(i);

            if (cursor.getKind() == SongClipKind.SOUND) {
                soundClips++;
                continue;
            }

            InstrumentHandle instrument = instrumentManager.getInstrumentHandleFromInstrumentID(
                    cursor.getInstrumentID());
            int channel = resolveChannel(song, cursor.getLane(), instrument, laneTracks[cursor.getLane()],
                    part2Channel, file);

            writeNotes(laneTracks[cursor.getLane()], cursor, channel);
        }

        try {
            MidiSystem.write(sequence, EngineSetting.SONG_MIDI_FILE_TYPE, file);
        } catch (IOException e) {
            throw new InternalException("Failed to write MIDI file: " + file.getAbsolutePath() + " ("
                    + e.getMessage() + ")", e);
        }

        return soundClips;
    }

    private static Sequence createSequence() {

        try {
            return new Sequence(Sequence.PPQ, EngineSetting.SONG_TICKS_PER_BEAT);
        } catch (InvalidMidiDataException e) {
            throw new InternalException("Failed to start a MIDI sequence (" + e.getMessage() + ")", e);
        }
    }

    // The first track: the song's name, its time signature, its key when MIDI can name it, and its tempo map
    private static void writeSettings(Track track, SongData song) {

        addMeta(track, EngineSetting.SONG_MIDI_META_TRACK_NAME,
                song.getDisplayName().getBytes(StandardCharsets.UTF_8), 0);
        addMeta(track, EngineSetting.SONG_MIDI_META_TIME_SIGNATURE, new byte[] {
                (byte) song.getBeatsPerBar(),
                (byte) Integer.numberOfTrailingZeros(song.getBeatUnit()),
                (byte) EngineSetting.SONG_MIDI_TIME_SIGNATURE_CLOCKS,
                (byte) EngineSetting.SONG_MIDI_TIME_SIGNATURE_32NDS }, 0);

        if (song.getKeyScale() == NoteScale.MAJOR || song.getKeyScale() == NoteScale.MINOR)
            addMeta(track, EngineSetting.SONG_MIDI_META_KEY_SIGNATURE, toKeySignature(song), 0);

        SongTempoStruct tempo = song.getTempo();

        for (int i = 0; i < tempo.getChangeCount(); i++)
            addMeta(track, EngineSetting.SONG_MIDI_META_TEMPO, toTempoBytes(tempo.getBpm(i)), tempo.getTick(i));
    }

    // The channel the lane plays an instrument on, its program, volume and pan set the first time it is taken:
    // every percussion part on channel 10, every other lane and instrument on a channel of its own, in order
    private static int resolveChannel(
            SongData song,
            int lane,
            InstrumentHandle instrument,
            Track track,
            Int2IntOpenHashMap part2Channel,
            File file) {

        // A part packs its lane above the instrument's short ID
        int part = instrument.isPercussion()
                ? EngineSetting.INDEX_NOT_FOUND
                : lane * EngineSetting.REGISTRY_SHORT_ID_COUNT + instrument.getInstrumentID();

        if (part2Channel.containsKey(part))
            return part2Channel.get(part);

        int melodicParts = part2Channel.size() - (part2Channel.containsKey(EngineSetting.INDEX_NOT_FOUND) ? 1 : 0);
        int channel = instrument.isPercussion()
                ? EngineSetting.SONG_MIDI_PERCUSSION_CHANNEL
                : melodicParts < EngineSetting.SONG_MIDI_PERCUSSION_CHANNEL ? melodicParts : melodicParts + 1;

        if (channel >= EngineSetting.SONG_MIDI_CHANNEL_COUNT)
            throw new InternalException("Song \"" + song.getSongName() + "\" plays more lane and instrument parts "
                    + "than MIDI has channels for (" + (EngineSetting.SONG_MIDI_CHANNEL_COUNT - 1)
                    + ", with channel 10 kept for percussion): " + file.getAbsolutePath());

        SongLaneStruct laneStruct = song.getLane(lane);
        int program = instrument.getMidiProgram() != EngineSetting.INSTRUMENT_MIDI_PROGRAM_NONE
                ? instrument.getMidiProgram()
                : EngineSetting.SONG_MIDI_PROGRAM_DEFAULT;

        part2Channel.put(part, channel);
        addShort(track, ShortMessage.PROGRAM_CHANGE, channel, program, 0, 0);
        addShort(track, ShortMessage.CONTROL_CHANGE, channel, EngineSetting.SONG_MIDI_CONTROL_VOLUME,
                Math.round(laneStruct.getVolume() * EngineSetting.SONG_MIDI_CONTROL_MAX), 0);
        addShort(track, ShortMessage.CONTROL_CHANGE, channel, EngineSetting.SONG_MIDI_CONTROL_PAN,
                Math.clamp(EngineSetting.SONG_MIDI_PAN_CENTRE
                        + Math.round(laneStruct.getPan() * EngineSetting.SONG_MIDI_PAN_RANGE), 0,
                        EngineSetting.SONG_MIDI_CONTROL_MAX), 0);

        return channel;
    }

    // Every note the cursor plays, each ending before the next on its pitch begins
    private static void writeNotes(Track track, SongCursorStruct cursor, int channel) {

        SongClipStruct clip = cursor.getClip();

        for (int note = 0; note < clip.getNoteCount(); note++) {

            int pitch = cursor.getNotePitch(note);

            if (!cursor.playsNote(note) || pitch < EngineSetting.NOTE_PITCH_MIN || pitch > EngineSetting.NOTE_PITCH_MAX)
                continue;

            addShort(track, ShortMessage.NOTE_ON, channel, pitch, clip.getNoteVelocity(note), cursor.getNoteTick(note));
            addShort(track, ShortMessage.NOTE_OFF, channel, pitch, 0, cursor.getNoteEndTick(note));
        }
    }

    private static void addShort(Track track, int command, int channel, int data1, int data2, long tick) {

        try {
            track.add(new MidiEvent(new ShortMessage(command, channel, data1, data2), tick));
        } catch (InvalidMidiDataException e) {
            throw new InternalException("Failed to write a MIDI event (" + e.getMessage() + ")", e);
        }
    }

    private static void addMeta(Track track, int type, byte[] data, long tick) {

        try {
            track.add(new MidiEvent(new MetaMessage(type, data, data.length), tick));
        } catch (InvalidMidiDataException e) {
            throw new InternalException("Failed to write a MIDI event (" + e.getMessage() + ")", e);
        }
    }

    // Utility \\

    // A tick at the file's resolution as a song's tick, rounded to the nearest
    private static int rescale(long tick, int resolution) {
        return (int) ((tick * EngineSetting.SONG_TICKS_PER_BEAT + resolution / 2) / resolution);
    }

    // A tempo event's microseconds a beat as beats a minute, kept to three decimals and inside the song range
    private static float toBpm(byte[] data) {

        int microseconds = 0;

        for (int i = 0; i < EngineSetting.SONG_MIDI_TEMPO_BYTES; i++)
            microseconds = (microseconds << EngineSetting.SONG_MIDI_BYTE_BITS)
                    | (data[i] & EngineSetting.SONG_MIDI_BYTE_MASK);

        double bpm = EngineSetting.SONG_MIDI_MICROSECONDS_PER_MINUTE / Math.max(1, microseconds);

        return (float) Math.clamp(Math.round(bpm * EngineSetting.SONG_MIDI_BPM_SCALE)
                / EngineSetting.SONG_MIDI_BPM_SCALE, EngineSetting.SONG_BPM_MIN, EngineSetting.SONG_BPM_MAX);
    }

    private static byte[] toTempoBytes(float bpm) {

        int microseconds = (int) Math.round(EngineSetting.SONG_MIDI_MICROSECONDS_PER_MINUTE / bpm);
        byte[] data = new byte[EngineSetting.SONG_MIDI_TEMPO_BYTES];

        for (int i = data.length - 1; i >= 0; i--) {
            data[i] = (byte) (microseconds & EngineSetting.SONG_MIDI_BYTE_MASK);
            microseconds >>= EngineSetting.SONG_MIDI_BYTE_BITS;
        }

        return data;
    }

    // The root of the key a signature's sharps (flats below zero) and mode name
    private static int toKeyRoot(byte sharps, byte mode) {

        int majorRoot = Math.floorMod(sharps * EngineSetting.SONG_MIDI_KEY_FIFTH_STEPS,
                EngineSetting.NOTE_SEMITONES_PER_OCTAVE);

        return mode == EngineSetting.SONG_MIDI_KEY_MINOR
                ? Math.floorMod(majorRoot + EngineSetting.SONG_MIDI_KEY_MINOR_STEPS,
                        EngineSetting.NOTE_SEMITONES_PER_OCTAVE)
                : majorRoot;
    }

    // The signature of a major or minor key, in the fewest sharps or flats that name it
    private static byte[] toKeySignature(SongData song) {

        boolean minor = song.getKeyScale() == NoteScale.MINOR;
        int majorRoot = minor
                ? Math.floorMod(song.getKeyRoot() - EngineSetting.SONG_MIDI_KEY_MINOR_STEPS,
                        EngineSetting.NOTE_SEMITONES_PER_OCTAVE)
                : song.getKeyRoot();
        int sharps = 0;

        for (int count = 0; count <= EngineSetting.SONG_MIDI_KEY_SHARPS_MAX; count++) {

            if (Math.floorMod(count * EngineSetting.SONG_MIDI_KEY_FIFTH_STEPS,
                    EngineSetting.NOTE_SEMITONES_PER_OCTAVE) == majorRoot) {
                sharps = count;
                break;
            }

            if (Math.floorMod(-count * EngineSetting.SONG_MIDI_KEY_FIFTH_STEPS,
                    EngineSetting.NOTE_SEMITONES_PER_OCTAVE) == majorRoot) {
                sharps = -count;
                break;
            }
        }

        return new byte[] { (byte) sharps,
                (byte) (minor ? EngineSetting.SONG_MIDI_KEY_MINOR : EngineSetting.SONG_MIDI_KEY_MAJOR) };
    }

    private static float toPan(int control) {
        return Math.clamp((float) (control - EngineSetting.SONG_MIDI_PAN_CENTRE) / EngineSetting.SONG_MIDI_PAN_RANGE,
                -1f, 1f);
    }

    private static SongTempoStruct toTempo(Int2FloatRBTreeMap tick2Bpm) {

        int[] ticks = tick2Bpm.keySet().toIntArray();
        float[] bpms = tick2Bpm.values().toFloatArray();

        return new SongTempoStruct(ticks, bpms);
    }

    private static String findTrackName(Track track) {

        for (int i = 0; i < track.size(); i++)
            if (track.get(i).getMessage() instanceof MetaMessage metaMessage
                    && metaMessage.getType() == EngineSetting.SONG_MIDI_META_TRACK_NAME)
                return new String(metaMessage.getData(), StandardCharsets.UTF_8).trim();

        return null;
    }

    private static boolean playsNotes(Track track) {

        for (int i = 0; i < track.size(); i++)
            if (track.get(i).getMessage() instanceof ShortMessage message
                    && message.getCommand() == ShortMessage.NOTE_ON && message.getData2() > 0)
                return true;

        return false;
    }

    private static String toLaneName(SongData song, int lane) {

        String name = song.getLane(lane).getName();

        return name.isEmpty() ? String.format(EngineSetting.SONG_MIDI_LANE_NAME_FORMAT, lane + 1) : name;
    }

    private static int findEnd(int[] notes) {

        int end = 1;

        for (int at = 0; at < notes.length; at += EngineSetting.SONG_NOTE_STRIDE)
            end = Math.max(end, notes[at + EngineSetting.SONG_NOTE_START] + notes[at + EngineSetting.SONG_NOTE_LENGTH]);

        return end;
    }
}
