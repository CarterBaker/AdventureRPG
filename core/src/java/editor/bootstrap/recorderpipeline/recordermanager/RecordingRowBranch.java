package editor.bootstrap.recorderpipeline.recordermanager;

import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.util.NoteScale;
import application.bootstrap.musicpipeline.util.NoteUtility;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingLaneStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingSourceKind;
import editor.bootstrap.recorderpipeline.recording.VideoFormat;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingRowBranch extends BranchPackage {

    /*
     * The Properties tab's rows for a media file, and the Character
     * Outliner's while a Recorder tab is worked in. The Recorder tab's are its
     * tempo, the beats of its bar and the note value of a beat, its key's
     * root and scale; the Timeline tab's the mix of the lane selected — its
     * name, volume, pan, mute and solo — and the clip selected's channels,
     * mute and time ramp, with a notes clip's instrument, volume and
     * transpose. The Outliner's are the recording's settings: the channels
     * armed for the next take, and the export's size, frame rate, format,
     * motion blur, supersampling and range. Every row is read and changed
     * here by its key: a number stepped, slid across its range or typed, a
     * choice stepped through every value it may take or typed, and every
     * change made through the one RecorderManager method that owns it.
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

        if (recording == null) {
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
    }

    void buildTimelineRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null) {
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

        RecordingClipStruct selected = findSelectedClip();

        if (selected == null)
            return;

        rows.add(CharacterViewRowStruct.header(String.format(EditorSetting.RECORDER_ROWS_HEADER_CLIP_FORMAT,
                selected.resolveLabelName())));

        int sourceChannels = selected.resolveSourceChannelMask();

        for (RecorderChannel channel : RecorderChannel.VALUES)
            if (channel.isIn(sourceChannels))
                addChoice(rows, EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX + channel.getKey(), channel.getLabel());

        addChoice(rows, EditorSetting.RECORDER_KEY_CLIP_MUTE, EditorSetting.RECORDER_ROW_LABEL_MUTED);
        addChoice(rows, EditorSetting.RECORDER_KEY_CLIP_RAMP, EditorSetting.RECORDER_ROW_LABEL_TIME_RAMP);

        RecordingClipStruct clip = findMusicClip();

        if (clip == null)
            return;

        SongClipStruct codec = clip.getSongClip();

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

    // The recording's settings: every channel armed or not, then its export
    void buildSettingsRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.RECORDER_ROWS_HEADER_RECORD));

        for (RecorderChannel channel : RecorderChannel.VALUES)
            addChoice(rows, EditorSetting.RECORDER_KEY_ARM_PREFIX + channel.getKey(), channel.getLabel());

        if (recorderManager.getRecording() == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.RECORDER_ROWS_NO_EXPORT));
            return;
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.RECORDER_ROWS_HEADER_EXPORT));
        addChoice(rows, EditorSetting.RECORDER_KEY_RESOLUTION, EditorSetting.RECORDER_ROW_LABEL_RESOLUTION);
        addChoice(rows, EditorSetting.RECORDER_KEY_FRAME_RATE, EditorSetting.RECORDER_ROW_LABEL_FRAME_RATE);
        addChoice(rows, EditorSetting.RECORDER_KEY_FORMAT, EditorSetting.RECORDER_ROW_LABEL_FORMAT);
        addChoice(rows, EditorSetting.RECORDER_KEY_MOTION_BLUR, EditorSetting.RECORDER_ROW_LABEL_MOTION_BLUR);
        addChoice(rows, EditorSetting.RECORDER_KEY_SUPERSAMPLE, EditorSetting.RECORDER_ROW_LABEL_SUPERSAMPLE);
        addChoice(rows, EditorSetting.RECORDER_KEY_RANGE_START, EditorSetting.RECORDER_ROW_LABEL_RANGE_START);
        addChoice(rows, EditorSetting.RECORDER_KEY_RANGE_END, EditorSetting.RECORDER_ROW_LABEL_RANGE_END);
    }

    private void addChoice(ObjectArrayList<CharacterViewRowStruct> rows, String key, String label) {
        rows.add(CharacterViewRowStruct.choice(key, label, getValueText(key)));
    }

    // The clip selected, null when none is
    private RecordingClipStruct findSelectedClip() {

        RecordingInstance recording = recorderManager.getRecording();
        int selected = recorderManager.getSelectedClip();

        if (recording == null || selected < 0 || selected >= recording.getClips().size())
            return null;

        return recording.getClips().get(selected);
    }

    // The clip selected when it carries its codec clip, as a notes clip does; null otherwise
    private RecordingClipStruct findMusicClip() {

        RecordingClipStruct clip = findSelectedClip();

        return clip != null && clip.getSongClip() != null ? clip : null;
    }

    // Values \\

    String getValueText(String key) {

        if (key.startsWith(EditorSetting.RECORDER_KEY_ARM_PREFIX))
            return toOnOff(resolveChannel(key, EditorSetting.RECORDER_KEY_ARM_PREFIX)
                    .isIn(recorderManager.getArmedChannels()));

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null)
            return EditorSetting.RECORDER_ROW_VALUE_NONE;

        if (key.startsWith(EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX)) {

            RecordingClipStruct selected = findSelectedClip();

            return selected != null
                    ? toOnOff(selected.holds(resolveChannel(key, EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX)))
                    : EditorSetting.RECORDER_ROW_VALUE_NONE;
        }

        String exportText = getExportValueText(key, recording.getExportStruct());

        if (exportText != null)
            return exportText;

        RecordingLaneStruct lane = recording.getLane(Math.max(0, recorderManager.getSelectedLane()));
        RecordingClipStruct clip = findMusicClip();

        return switch (key) {
            case EditorSetting.RECORDER_ROW_TEMPO -> CharacterViewUtility.formatNumber(recording.getTempo().getBpm(0));
            case EditorSetting.RECORDER_ROW_BEATS_PER_BAR -> Integer.toString(recording.getBeatsPerBar());
            case EditorSetting.RECORDER_ROW_BEAT_UNIT -> Integer.toString(recording.getBeatUnit());
            case EditorSetting.RECORDER_ROW_KEY_ROOT -> EngineSetting.NOTE_NAMES[recording.getKeyRoot()];
            case EditorSetting.RECORDER_ROW_KEY_SCALE -> CharacterViewUtility.toResourceLabel(
                    toWords(recording.getKeyScale()));
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
            case EditorSetting.RECORDER_KEY_CLIP_MUTE -> findSelectedClip() != null
                    ? toOnOff(findSelectedClip().isMuted())
                    : EditorSetting.RECORDER_ROW_VALUE_NONE;
            case EditorSetting.RECORDER_KEY_CLIP_RAMP -> findSelectedClip() != null
                    ? formatRamp(findSelectedClip().getTimeRampSeconds())
                    : EditorSetting.RECORDER_ROW_VALUE_NONE;
            default -> EditorSetting.RECORDER_ROW_VALUE_NONE;
        };
    }

    // An export setting's value as its row shows it, null for a key that names none
    private String getExportValueText(String key, RecordingExportStruct exportStruct) {
        return switch (key) {
            case EditorSetting.RECORDER_KEY_RESOLUTION -> String.format(EditorSetting.RECORDER_ROW_FORMAT_RESOLUTION,
                    exportStruct.getWidth(), exportStruct.getHeight());
            case EditorSetting.RECORDER_KEY_FRAME_RATE -> String.format(EditorSetting.RECORDER_ROW_FORMAT_FRAME_RATE,
                    exportStruct.getFrameRate());
            case EditorSetting.RECORDER_KEY_FORMAT -> exportStruct.getFormat().getLabel();
            case EditorSetting.RECORDER_KEY_MOTION_BLUR -> exportStruct.getMotionBlurSamples() > 1
                    ? String.format(EditorSetting.RECORDER_ROW_FORMAT_MOTION_BLUR, exportStruct.getMotionBlurSamples())
                    : EditorSetting.RECORDER_ROW_VALUE_OFF;
            case EditorSetting.RECORDER_KEY_SUPERSAMPLE -> toOnOff(exportStruct.isSupersampled());
            case EditorSetting.RECORDER_KEY_RANGE_START -> exportStruct.hasRangeStart()
                    ? String.format(EditorSetting.RECORDER_ROW_FORMAT_SECONDS, exportStruct.getRangeStart())
                    : EditorSetting.RECORDER_ROW_VALUE_RANGE_START;
            case EditorSetting.RECORDER_KEY_RANGE_END -> exportStruct.hasRangeEnd()
                    ? String.format(EditorSetting.RECORDER_ROW_FORMAT_SECONDS, exportStruct.getRangeEnd())
                    : EditorSetting.RECORDER_ROW_VALUE_RANGE_END;
            default -> null;
        };
    }

    private String formatRamp(float seconds) {
        return seconds > 0f
                ? String.format(EditorSetting.RECORDER_ROW_FORMAT_RAMP, seconds)
                : EditorSetting.RECORDER_ROW_VALUE_RAMP_CUT;
    }

    private RecorderChannel resolveChannel(String key, String prefix) {

        RecorderChannel channel = RecorderChannel.findByKey(key.substring(prefix.length()));

        if (channel == null)
            throwException("Unknown recorder channel: " + key);

        return channel;
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

        if (recording == null)
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

        if (direction == 0)
            return;

        if (key.startsWith(EditorSetting.RECORDER_KEY_ARM_PREFIX)) {
            recorderManager.toggleArm(resolveChannel(key, EditorSetting.RECORDER_KEY_ARM_PREFIX));
            return;
        }

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null)
            return;

        if (key.startsWith(EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX)) {
            recorderManager.toggleClipChannel(resolveChannel(key, EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX));
            return;
        }

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
            case EditorSetting.RECORDER_ROW_LANE_MUTED -> recorderManager.toggleLaneMute(laneIndex);
            case EditorSetting.RECORDER_ROW_LANE_SOLO -> recorderManager.toggleLaneSolo(laneIndex);
            case EditorSetting.RECORDER_ROW_CLIP_INSTRUMENT -> {
                if (clip != null)
                    replaceClip(clip, cycleInstrument(clip.getSongClip().getInstrumentName(), direction),
                            clip.getSongClip().getVolume(), clip.getSongClip().getTranspose());
            }
            case EditorSetting.RECORDER_KEY_CLIP_MUTE -> recorderManager.toggleClipMute();
            case EditorSetting.RECORDER_KEY_CLIP_RAMP -> recorderManager.cycleClipTimeRamp(direction);
            case EditorSetting.RECORDER_KEY_RESOLUTION -> recorderManager.cycleResolution(direction);
            case EditorSetting.RECORDER_KEY_FRAME_RATE -> recorderManager.cycleFrameRate(direction);
            case EditorSetting.RECORDER_KEY_FORMAT -> recorderManager.cycleFormat(direction);
            case EditorSetting.RECORDER_KEY_MOTION_BLUR -> recorderManager.cycleMotionBlur(direction);
            case EditorSetting.RECORDER_KEY_SUPERSAMPLE -> recorderManager.toggleSupersample();
            case EditorSetting.RECORDER_KEY_RANGE_START -> recorderManager.setRangeStart();
            case EditorSetting.RECORDER_KEY_RANGE_END -> recorderManager.setRangeEnd();
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

        if (key.startsWith(EditorSetting.RECORDER_KEY_ARM_PREFIX)
                || key.startsWith(EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX))
            return isOnOff(text);

        return switch (key) {
            case EditorSetting.RECORDER_KEY_CLIP_MUTE, EditorSetting.RECORDER_KEY_SUPERSAMPLE -> isOnOff(text);
            case EditorSetting.RECORDER_KEY_FORMAT -> findFormat(text) != null;
            case EditorSetting.RECORDER_KEY_RESOLUTION -> findIndex(EditorSetting.RECORDER_EXPORT_HEIGHTS, text)
                    != EngineSetting.INDEX_NOT_FOUND;
            case EditorSetting.RECORDER_KEY_FRAME_RATE -> findIndex(EditorSetting.RECORDER_EXPORT_FRAME_RATES, text)
                    != EngineSetting.INDEX_NOT_FOUND;
            case EditorSetting.RECORDER_KEY_MOTION_BLUR -> findIndex(
                    EditorSetting.RECORDER_EXPORT_MOTION_BLUR_SAMPLES, text) != EngineSetting.INDEX_NOT_FOUND;
            case EditorSetting.RECORDER_KEY_CLIP_RAMP -> findRamp(text) != EngineSetting.INDEX_NOT_FOUND;
            case EditorSetting.RECORDER_ROW_LANE_NAME -> text.length() <= EditorSetting.RECORDER_LABEL_MAX_LENGTH;
            case EditorSetting.RECORDER_ROW_KEY_ROOT -> NoteUtility.parseNoteName(
                    text.trim() + EditorSetting.RECORDER_ROW_KEY_OCTAVE) != EngineSetting.INDEX_NOT_FOUND;
            case EditorSetting.RECORDER_ROW_KEY_SCALE -> findScale(text) != null;
            case EditorSetting.RECORDER_ROW_CLIP_INSTRUMENT -> findInstrument(text) != null;
            case EditorSetting.RECORDER_ROW_LANE_MUTED, EditorSetting.RECORDER_ROW_LANE_SOLO -> isOnOff(text);
            default -> CharacterViewUtility.isNumber(text);
        };
    }

    void setValue(String key, String text) {

        if (!isValueValid(key, text))
            return;

        if (key.startsWith(EditorSetting.RECORDER_KEY_ARM_PREFIX)
                || key.startsWith(EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX)) {

            if (!getValueText(key).equalsIgnoreCase(text.trim()))
                cycle(key, 1);

            return;
        }

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null)
            return;

        if (setExportValue(key, text, recording.getExportStruct()))
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

    // A typed export setting, or the clip's mute or time ramp, stepped straight to; false for any other key
    private boolean setExportValue(String key, String text, RecordingExportStruct exportStruct) {

        float number = CharacterViewUtility.isNumber(text) ? CharacterViewUtility.parseNumber(text) : 0f;

        switch (key) {
            case EditorSetting.RECORDER_KEY_CLIP_MUTE, EditorSetting.RECORDER_KEY_SUPERSAMPLE -> {
                if (!getValueText(key).equalsIgnoreCase(text.trim()))
                    cycle(key, 1);
            }
            case EditorSetting.RECORDER_KEY_FORMAT -> recorderManager.cycleFormat(
                    findFormat(text).ordinal() - exportStruct.getFormat().ordinal());
            case EditorSetting.RECORDER_KEY_RESOLUTION -> recorderManager.cycleResolution(
                    findIndex(EditorSetting.RECORDER_EXPORT_HEIGHTS, text)
                            - findIndex(EditorSetting.RECORDER_EXPORT_HEIGHTS, exportStruct.getHeight()));
            case EditorSetting.RECORDER_KEY_FRAME_RATE -> recorderManager.cycleFrameRate(
                    findIndex(EditorSetting.RECORDER_EXPORT_FRAME_RATES, text)
                            - findIndex(EditorSetting.RECORDER_EXPORT_FRAME_RATES, exportStruct.getFrameRate()));
            case EditorSetting.RECORDER_KEY_MOTION_BLUR -> recorderManager.cycleMotionBlur(
                    findIndex(EditorSetting.RECORDER_EXPORT_MOTION_BLUR_SAMPLES, text)
                            - findIndex(EditorSetting.RECORDER_EXPORT_MOTION_BLUR_SAMPLES,
                                    exportStruct.getMotionBlurSamples()));
            case EditorSetting.RECORDER_KEY_CLIP_RAMP -> {
                RecordingClipStruct selected = findSelectedClip();

                if (selected != null)
                    recorderManager.cycleClipTimeRamp(findRamp(text) - findRamp(selected.getTimeRampSeconds()));
            }
            case EditorSetting.RECORDER_KEY_RANGE_START -> recorderManager.setRangeStartAt(number);
            case EditorSetting.RECORDER_KEY_RANGE_END -> recorderManager.setRangeEndAt(number);
            default -> {
                return false;
            }
        }

        return true;
    }

    // The place of a typed number among a setting's values, -1 when it is none of them
    private int findIndex(int[] values, String text) {
        return CharacterViewUtility.isNumber(text)
                ? findIndex(values, Math.round(CharacterViewUtility.parseNumber(text)))
                : EngineSetting.INDEX_NOT_FOUND;
    }

    private int findIndex(int[] values, int value) {

        for (int i = 0; i < values.length; i++)
            if (values[i] == value)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private int findRamp(String text) {
        return CharacterViewUtility.isNumber(text)
                ? findRamp(CharacterViewUtility.parseNumber(text))
                : EngineSetting.INDEX_NOT_FOUND;
    }

    private int findRamp(float seconds) {

        float[] ramps = EditorSetting.RECORDER_TIME_RAMP_SECONDS;

        for (int i = 0; i < ramps.length; i++)
            if (ramps[i] == seconds)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private VideoFormat findFormat(String text) {

        for (int i = 0; i < VideoFormat.VALUES.length; i++)
            if (VideoFormat.VALUES[i].getLabel().equalsIgnoreCase(text.trim()))
                return VideoFormat.VALUES[i];

        return null;
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
