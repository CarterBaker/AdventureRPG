package editor.bootstrap.recorderpipeline.recordermanager;

import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.util.NoteScale;
import application.bootstrap.musicpipeline.util.NoteUtility;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingLaneStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingSourceKind;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingRowBranch extends BranchPackage {

    /*
     * The Properties tab's rows for a Music file. The Recorder tab's are its
     * tempo, the beats of its bar and the note value of a beat, its key's
     * root and scale, and the metronome; the Timeline tab's the mix of the
     * lane selected — its name, volume, pan, mute and solo — and the clip
     * selected's instrument, volume and transpose. Every row is read and
     * changed here by its key: a number stepped, slid across its range or
     * typed, a choice stepped through every value it may take or typed, and
     * every change made through the one RecorderManager method that owns it.
     */

    // Internal
    private RecorderManager recorderManager;
    private InstrumentManager instrumentManager;

    // Scratch
    private ObjectArrayList<String> instrumentNames;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.instrumentNames = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.instrumentManager = get(InstrumentManager.class);
    }

    // Build \\

    void buildTransportRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null || recording.getKind().isFilmed()) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.RECORDER_ROWS_NO_MUSIC));
            return;
        }

        float bpm = recording.getTempo().getBpm(0);

        rows.add(CharacterViewRowStruct.header(EditorSetting.RECORDER_ROWS_HEADER_TIME));
        rows.add(CharacterViewRowStruct.ranged(EditorSetting.RECORDER_ROW_TEMPO, EditorSetting.RECORDER_ROW_LABEL_TEMPO,
                getValueText(EditorSetting.RECORDER_ROW_TEMPO), bpm, EngineSetting.SONG_BPM_MIN,
                EngineSetting.SONG_BPM_MAX, 1f));
        rows.add(CharacterViewRowStruct.ranged(EditorSetting.RECORDER_ROW_BEATS_PER_BAR,
                EditorSetting.RECORDER_ROW_LABEL_BEATS_PER_BAR,
                getValueText(EditorSetting.RECORDER_ROW_BEATS_PER_BAR), recording.getBeatsPerBar(), 1f,
                EngineSetting.SONG_BEATS_PER_BAR_MAX, 1f));
        rows.add(CharacterViewRowStruct.choice(EditorSetting.RECORDER_ROW_BEAT_UNIT,
                EditorSetting.RECORDER_ROW_LABEL_BEAT_UNIT, getValueText(EditorSetting.RECORDER_ROW_BEAT_UNIT)));
        rows.add(CharacterViewRowStruct.header(EditorSetting.RECORDER_ROWS_HEADER_KEY));
        rows.add(CharacterViewRowStruct.choice(EditorSetting.RECORDER_ROW_KEY_ROOT,
                EditorSetting.RECORDER_ROW_LABEL_KEY_ROOT, getValueText(EditorSetting.RECORDER_ROW_KEY_ROOT)));
        rows.add(CharacterViewRowStruct.choice(EditorSetting.RECORDER_ROW_KEY_SCALE,
                EditorSetting.RECORDER_ROW_LABEL_KEY_SCALE, getValueText(EditorSetting.RECORDER_ROW_KEY_SCALE)));
        rows.add(CharacterViewRowStruct.choice(EditorSetting.RECORDER_ROW_METRONOME,
                EditorSetting.RECORDER_ROW_LABEL_METRONOME, getValueText(EditorSetting.RECORDER_ROW_METRONOME)));
    }

    void buildTimelineRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null || recording.getKind().isFilmed()) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.RECORDER_ROWS_NO_MUSIC));
            return;
        }

        RecordingLaneStruct lane = recording.getLane(Math.max(0, recorderManager.getSelectedLane()));

        rows.add(CharacterViewRowStruct.header(String.format(EditorSetting.RECORDER_ROWS_HEADER_LANE_FORMAT,
                Math.max(0, recorderManager.getSelectedLane()) + 1)));
        rows.add(CharacterViewRowStruct.text(EditorSetting.RECORDER_ROW_LANE_NAME,
                EditorSetting.RECORDER_ROW_LABEL_LANE_NAME, lane.getName()));
        rows.add(CharacterViewRowStruct.ranged(EditorSetting.RECORDER_ROW_LANE_VOLUME,
                EditorSetting.RECORDER_ROW_LABEL_VOLUME, getValueText(EditorSetting.RECORDER_ROW_LANE_VOLUME),
                lane.getVolume(), 0f, 1f, EditorSetting.RECORDER_ROW_MIX_STEP));
        rows.add(CharacterViewRowStruct.ranged(EditorSetting.RECORDER_ROW_LANE_PAN,
                EditorSetting.RECORDER_ROW_LABEL_PAN, getValueText(EditorSetting.RECORDER_ROW_LANE_PAN),
                lane.getPan(), -1f, 1f, EditorSetting.RECORDER_ROW_MIX_STEP));
        rows.add(CharacterViewRowStruct.choice(EditorSetting.RECORDER_ROW_LANE_MUTED,
                EditorSetting.RECORDER_ROW_LABEL_MUTED, getValueText(EditorSetting.RECORDER_ROW_LANE_MUTED)));
        rows.add(CharacterViewRowStruct.choice(EditorSetting.RECORDER_ROW_LANE_SOLO,
                EditorSetting.RECORDER_ROW_LABEL_SOLO, getValueText(EditorSetting.RECORDER_ROW_LANE_SOLO)));

        RecordingClipStruct clip = findMusicClip();

        if (clip == null)
            return;

        SongClipStruct codec = clip.getSongClip();

        rows.add(CharacterViewRowStruct.header(String.format(EditorSetting.RECORDER_ROWS_HEADER_CLIP_FORMAT,
                clip.getSourceName())));

        if (clip.getSourceKind() == RecordingSourceKind.NOTES)
            rows.add(CharacterViewRowStruct.choice(EditorSetting.RECORDER_ROW_CLIP_INSTRUMENT,
                    EditorSetting.RECORDER_ROW_LABEL_INSTRUMENT,
                    getValueText(EditorSetting.RECORDER_ROW_CLIP_INSTRUMENT)));

        rows.add(CharacterViewRowStruct.ranged(EditorSetting.RECORDER_ROW_CLIP_VOLUME,
                EditorSetting.RECORDER_ROW_LABEL_VOLUME, getValueText(EditorSetting.RECORDER_ROW_CLIP_VOLUME),
                codec.getVolume(), 0f, 1f, EditorSetting.RECORDER_ROW_MIX_STEP));
        rows.add(CharacterViewRowStruct.ranged(EditorSetting.RECORDER_ROW_CLIP_TRANSPOSE,
                EditorSetting.RECORDER_ROW_LABEL_TRANSPOSE, getValueText(EditorSetting.RECORDER_ROW_CLIP_TRANSPOSE),
                codec.getTranspose(), -EngineSetting.SONG_CLIP_MAX_TRANSPOSE, EngineSetting.SONG_CLIP_MAX_TRANSPOSE,
                1f));
    }

    // The clip selected when it carries its codec clip — a notes clip, or a song a Music file lays — null otherwise
    private RecordingClipStruct findMusicClip() {

        RecordingInstance recording = recorderManager.getRecording();
        int selected = recorderManager.getSelectedClip();

        if (recording == null || selected < 0 || selected >= recording.getClips().size())
            return null;

        RecordingClipStruct clip = recording.getClips().get(selected);

        return clip.getSongClip() != null ? clip : null;
    }

    // Values \\

    String getValueText(String key) {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null)
            return EditorSetting.RECORDER_ROW_VALUE_NONE;

        RecordingLaneStruct lane = recording.getLane(Math.max(0, recorderManager.getSelectedLane()));
        RecordingClipStruct clip = findMusicClip();

        return switch (key) {
            case EditorSetting.RECORDER_ROW_TEMPO -> CharacterViewUtility.formatNumber(recording.getTempo().getBpm(0));
            case EditorSetting.RECORDER_ROW_BEATS_PER_BAR -> Integer.toString(recording.getBeatsPerBar());
            case EditorSetting.RECORDER_ROW_BEAT_UNIT -> Integer.toString(recording.getBeatUnit());
            case EditorSetting.RECORDER_ROW_KEY_ROOT -> EngineSetting.NOTE_NAMES[recording.getKeyRoot()];
            case EditorSetting.RECORDER_ROW_KEY_SCALE -> CharacterViewUtility.toResourceLabel(
                    toWords(recording.getKeyScale()));
            case EditorSetting.RECORDER_ROW_METRONOME -> toOnOff(recorderManager.isMetronomeOn());
            case EditorSetting.RECORDER_ROW_LANE_NAME -> lane.getName();
            case EditorSetting.RECORDER_ROW_LANE_VOLUME -> CharacterViewUtility.formatNumber(lane.getVolume());
            case EditorSetting.RECORDER_ROW_LANE_PAN -> CharacterViewUtility.formatNumber(lane.getPan());
            case EditorSetting.RECORDER_ROW_LANE_MUTED -> toOnOff(lane.isMuted());
            case EditorSetting.RECORDER_ROW_LANE_SOLO -> toOnOff(lane.isSolo());
            case EditorSetting.RECORDER_ROW_CLIP_INSTRUMENT -> clip != null
                    ? CharacterViewUtility.toResourceLabel(clip.getSongClip().getInstrumentName())
                    : EditorSetting.RECORDER_ROW_VALUE_NONE;
            case EditorSetting.RECORDER_ROW_CLIP_VOLUME -> clip != null
                    ? CharacterViewUtility.formatNumber(clip.getSongClip().getVolume())
                    : EditorSetting.RECORDER_ROW_VALUE_NONE;
            case EditorSetting.RECORDER_ROW_CLIP_TRANSPOSE -> clip != null
                    ? Integer.toString(clip.getSongClip().getTranspose())
                    : EditorSetting.RECORDER_ROW_VALUE_NONE;
            default -> EditorSetting.RECORDER_ROW_VALUE_NONE;
        };
    }

    // A scale's name as the row shows it: MAJOR_PENTATONIC is MajorPentatonic, read as Major Pentatonic
    private String toWords(NoteScale scale) {

        String[] parts = scale.name().split(EditorSetting.RECORDER_ROW_ENUM_SEPARATOR);
        StringBuilder words = new StringBuilder();

        for (int i = 0; i < parts.length; i++)
            words.append(parts[i].charAt(0)).append(parts[i].substring(1).toLowerCase());

        return words.toString();
    }

    private String toOnOff(boolean on) {
        return on ? EditorSetting.RECORDER_ROW_VALUE_ON : EditorSetting.RECORDER_ROW_VALUE_OFF;
    }

    // Step \\

    void step(String key, int steps) {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null || recording.getKind().isFilmed())
            return;

        int laneIndex = Math.max(0, recorderManager.getSelectedLane());
        RecordingLaneStruct lane = recording.getLane(laneIndex);
        RecordingClipStruct clip = findMusicClip();

        switch (key) {
            case EditorSetting.RECORDER_ROW_TEMPO -> recorderManager.setTempo(recording.getTempo().getBpm(0) + steps);
            case EditorSetting.RECORDER_ROW_BEATS_PER_BAR -> recorderManager.setMeter(
                    Math.clamp(recording.getBeatsPerBar() + steps, 1, EngineSetting.SONG_BEATS_PER_BAR_MAX),
                    recording.getBeatUnit());
            case EditorSetting.RECORDER_ROW_LANE_VOLUME -> recorderManager.setLaneVolume(laneIndex,
                    lane.getVolume() + steps * EditorSetting.RECORDER_ROW_MIX_STEP);
            case EditorSetting.RECORDER_ROW_LANE_PAN -> recorderManager.setLanePan(laneIndex,
                    lane.getPan() + steps * EditorSetting.RECORDER_ROW_MIX_STEP);
            case EditorSetting.RECORDER_ROW_CLIP_VOLUME -> {
                if (clip != null)
                    replaceClip(clip, clip.getSongClip().getInstrumentName(),
                            clip.getSongClip().getVolume() + steps * EditorSetting.RECORDER_ROW_MIX_STEP,
                            clip.getSongClip().getTranspose());
            }
            case EditorSetting.RECORDER_ROW_CLIP_TRANSPOSE -> {
                if (clip != null)
                    replaceClip(clip, clip.getSongClip().getInstrumentName(), clip.getSongClip().getVolume(),
                            clip.getSongClip().getTranspose() + steps);
            }
            default -> cycle(key, Integer.signum(steps));
        }
    }

    // A choice stepped to the value before or after it, round from the last to the first
    void cycle(String key, int direction) {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null || recording.getKind().isFilmed() || direction == 0)
            return;

        int laneIndex = Math.max(0, recorderManager.getSelectedLane());
        RecordingClipStruct clip = findMusicClip();

        switch (key) {
            case EditorSetting.RECORDER_ROW_BEAT_UNIT -> recorderManager.setMeter(recording.getBeatsPerBar(),
                    cycleBeatUnit(recording.getBeatUnit(), direction));
            case EditorSetting.RECORDER_ROW_KEY_ROOT -> recorderManager.setKey(recording.getKeyRoot() + direction,
                    recording.getKeyScale());
            case EditorSetting.RECORDER_ROW_KEY_SCALE -> recorderManager.setKey(recording.getKeyRoot(),
                    NoteScale.VALUES[Math.floorMod(recording.getKeyScale().ordinal() + direction,
                            NoteScale.VALUES.length)]);
            case EditorSetting.RECORDER_ROW_METRONOME -> recorderManager.toggleMetronome();
            case EditorSetting.RECORDER_ROW_LANE_MUTED -> recorderManager.toggleLaneMute(laneIndex);
            case EditorSetting.RECORDER_ROW_LANE_SOLO -> recorderManager.toggleLaneSolo(laneIndex);
            case EditorSetting.RECORDER_ROW_CLIP_INSTRUMENT -> {
                if (clip != null)
                    replaceClip(clip, cycleInstrument(clip.getSongClip().getInstrumentName(), direction),
                            clip.getSongClip().getVolume(), clip.getSongClip().getTranspose());
            }
            default -> {
            }
        }
    }

    // The beat unit halved or doubled, round within 1 to SONG_BEAT_UNIT_MAX
    private int cycleBeatUnit(int beatUnit, int direction) {

        int steps = Integer.numberOfTrailingZeros(EngineSetting.SONG_BEAT_UNIT_MAX) + 1;

        return 1 << Math.floorMod(Integer.numberOfTrailingZeros(beatUnit) + direction, steps);
    }

    private String cycleInstrument(String instrumentName, int direction) {

        instrumentNames.clear();
        instrumentManager.collectInstrumentNames(instrumentNames);

        if (instrumentNames.isEmpty())
            return instrumentName;

        int at = instrumentNames.indexOf(instrumentName);

        return instrumentNames.get(Math.floorMod(at + direction, instrumentNames.size()));
    }

    // Typed \\

    boolean isValueValid(String key, String text) {
        return switch (key) {
            case EditorSetting.RECORDER_ROW_LANE_NAME -> text.length() <= EditorSetting.RECORDER_LABEL_MAX_LENGTH;
            case EditorSetting.RECORDER_ROW_KEY_ROOT -> NoteUtility.parseNoteName(
                    text.trim() + EditorSetting.RECORDER_ROW_KEY_OCTAVE) != EngineSetting.INDEX_NOT_FOUND;
            case EditorSetting.RECORDER_ROW_KEY_SCALE -> findScale(text) != null;
            case EditorSetting.RECORDER_ROW_CLIP_INSTRUMENT -> findInstrument(text) != null;
            case EditorSetting.RECORDER_ROW_METRONOME, EditorSetting.RECORDER_ROW_LANE_MUTED,
                    EditorSetting.RECORDER_ROW_LANE_SOLO -> isOnOff(text);
            default -> CharacterViewUtility.isNumber(text);
        };
    }

    void setValue(String key, String text) {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null || recording.getKind().isFilmed() || !isValueValid(key, text))
            return;

        int laneIndex = Math.max(0, recorderManager.getSelectedLane());
        RecordingLaneStruct lane = recording.getLane(laneIndex);
        RecordingClipStruct clip = findMusicClip();
        float number = CharacterViewUtility.parseNumber(text);

        switch (key) {
            case EditorSetting.RECORDER_ROW_TEMPO -> recorderManager.setTempo(number);
            case EditorSetting.RECORDER_ROW_BEATS_PER_BAR -> recorderManager.setMeter(Math.round(number),
                    recording.getBeatUnit());
            case EditorSetting.RECORDER_ROW_BEAT_UNIT -> recorderManager.setMeter(recording.getBeatsPerBar(),
                    Math.round(number));
            case EditorSetting.RECORDER_ROW_KEY_ROOT -> recorderManager.setKey(NoteUtility.parseNoteName(
                    text.trim() + EditorSetting.RECORDER_ROW_KEY_OCTAVE), recording.getKeyScale());
            case EditorSetting.RECORDER_ROW_KEY_SCALE -> recorderManager.setKey(recording.getKeyRoot(),
                    findScale(text));
            case EditorSetting.RECORDER_ROW_METRONOME -> {
                if (isOn(text) != recorderManager.isMetronomeOn())
                    recorderManager.toggleMetronome();
            }
            case EditorSetting.RECORDER_ROW_LANE_NAME -> recorderManager.setLaneName(laneIndex, text.trim());
            case EditorSetting.RECORDER_ROW_LANE_VOLUME -> recorderManager.setLaneVolume(laneIndex, number);
            case EditorSetting.RECORDER_ROW_LANE_PAN -> recorderManager.setLanePan(laneIndex, number);
            case EditorSetting.RECORDER_ROW_LANE_MUTED -> {
                if (isOn(text) != lane.isMuted())
                    recorderManager.toggleLaneMute(laneIndex);
            }
            case EditorSetting.RECORDER_ROW_LANE_SOLO -> {
                if (isOn(text) != lane.isSolo())
                    recorderManager.toggleLaneSolo(laneIndex);
            }
            case EditorSetting.RECORDER_ROW_CLIP_INSTRUMENT -> {
                if (clip != null)
                    replaceClip(clip, findInstrument(text), clip.getSongClip().getVolume(),
                            clip.getSongClip().getTranspose());
            }
            case EditorSetting.RECORDER_ROW_CLIP_VOLUME -> {
                if (clip != null)
                    replaceClip(clip, clip.getSongClip().getInstrumentName(), number,
                            clip.getSongClip().getTranspose());
            }
            case EditorSetting.RECORDER_ROW_CLIP_TRANSPOSE -> {
                if (clip != null)
                    replaceClip(clip, clip.getSongClip().getInstrumentName(), clip.getSongClip().getVolume(),
                            Math.round(number));
            }
            default -> {
            }
        }
    }

    private NoteScale findScale(String text) {

        String wanted = text.replace(EditorSetting.RECORDER_ROW_WORD_SEPARATOR, "").trim();

        for (int i = 0; i < NoteScale.VALUES.length; i++)
            if (toWords(NoteScale.VALUES[i]).equalsIgnoreCase(wanted))
                return NoteScale.VALUES[i];

        return null;
    }

    // The instrument typed, by its file name or as its row shows it
    private String findInstrument(String text) {

        instrumentNames.clear();
        instrumentManager.collectInstrumentNames(instrumentNames);

        String wanted = text.trim();

        for (int i = 0; i < instrumentNames.size(); i++)
            if (instrumentNames.get(i).equalsIgnoreCase(wanted)
                    || CharacterViewUtility.toResourceLabel(instrumentNames.get(i)).equalsIgnoreCase(wanted))
                return instrumentNames.get(i);

        return null;
    }

    private boolean isOnOff(String text) {
        return text.trim().equalsIgnoreCase(EditorSetting.RECORDER_ROW_VALUE_ON)
                || text.trim().equalsIgnoreCase(EditorSetting.RECORDER_ROW_VALUE_OFF);
    }

    private boolean isOn(String text) {
        return text.trim().equalsIgnoreCase(EditorSetting.RECORDER_ROW_VALUE_ON);
    }

    // Clip \\

    // The clip's codec clip made again with another instrument, volume or transpose, each held to its range
    private void replaceClip(RecordingClipStruct clip, String instrumentName, float volume, int transpose) {

        SongClipStruct codec = clip.getSongClip();

        recorderManager.replaceSongClip(clip, new SongClipStruct(
                codec.getKind(),
                codec.getLane(),
                codec.getStart(),
                codec.getLength(),
                codec.getOffset(),
                codec.isMuted(),
                Math.clamp(CharacterViewUtility.round(volume), 0f, 1f),
                Math.clamp(transpose, -EngineSetting.SONG_CLIP_MAX_TRANSPOSE, EngineSetting.SONG_CLIP_MAX_TRANSPOSE),
                instrumentName,
                codec.getNotes(),
                codec.getSongName(),
                codec.getSoundName()));
    }
}
