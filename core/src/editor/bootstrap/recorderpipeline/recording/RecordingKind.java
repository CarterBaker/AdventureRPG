package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;

public enum RecordingKind {

    /*
     * What a recording is, chosen when it is made. A Video is the recorder's
     * own: takes recorded live in a world, nested recordings, songs, notes and
     * sounds on RECORDER_LANE_COUNT lanes, saved beside the recordings. A
     * Music file is a song: notes, songs and sounds alone on SONG_LANE_COUNT
     * lanes, each with its mixer, timed in bars and beats on its tempo map,
     * the timeline snapping to its grid, saved straight into the project's
     * songs through the song codec; it records notes typed in, never a take
     * of the world, and nests songs rather than recordings. Each kind's label
     * is the word New asks for.
     */

    VIDEO(EditorSetting.RECORDER_KIND_LABEL_VIDEO, EditorSetting.RECORDER_LANE_COUNT, RecorderChannel.ALL, true),
    MUSIC(EditorSetting.RECORDER_KIND_LABEL_MUSIC, EngineSetting.SONG_LANE_COUNT, RecorderChannel.SOUND.getBit(),
            false);

    // Values
    public static final RecordingKind[] VALUES = values();

    // Internal
    private final String label;
    private final int laneCount;
    private final int channelMask;
    private final boolean filmed;

    // Constructor \\

    RecordingKind(String label, int laneCount, int channelMask, boolean filmed) {
        this.label = label;
        this.laneCount = laneCount;
        this.channelMask = channelMask;
        this.filmed = filmed;
    }

    // The kind its label names, ignoring case; null for none
    public static RecordingKind findByLabel(String label) {

        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i].label.equalsIgnoreCase(label.trim()))
                return VALUES[i];

        return null;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public int getLaneCount() {
        return laneCount;
    }

    // Every channel a recording of the kind can play
    public int getChannelMask() {
        return channelMask;
    }

    // True for a recording filmed in a world: it records takes, nests recordings and exports video; false for a
    // Music file, which records notes, nests songs, mixes its lanes and keeps to its grid
    public boolean isFilmed() {
        return filmed;
    }
}
