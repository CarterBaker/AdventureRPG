package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum RecorderChannel {

    /*
     * What a layer of a recording can hold. The camera is the view; the player
     * is the character played; the world is every resident and thrown item
     * about it; time is the world's clock, and the weather and sky that
     * follow it. A layer's channels are a mask of these bits, and recording
     * files name channels by their lower-case constant names.
     */

    CAMERA(EditorSetting.RECORDER_CHANNEL_LABEL_CAMERA),
    PLAYER(EditorSetting.RECORDER_CHANNEL_LABEL_PLAYER),
    WORLD(EditorSetting.RECORDER_CHANNEL_LABEL_WORLD),
    TIME(EditorSetting.RECORDER_CHANNEL_LABEL_TIME);

    // Values
    public static final RecorderChannel[] VALUES = values();
    public static final int ALL = (1 << VALUES.length) - 1;
    public static final int NONE = 0;

    // Internal
    private final String label;

    // Constructor \\

    RecorderChannel(String label) {
        this.label = label;
    }

    // Accessible \\

    public int getBit() {
        return 1 << ordinal();
    }

    public boolean isIn(int channelMask) {
        return (channelMask & getBit()) != 0;
    }

    public String getLabel() {
        return label;
    }
}
