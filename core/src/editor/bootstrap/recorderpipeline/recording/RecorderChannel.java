package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum RecorderChannel {

    /*
     * What a take can hold, and what each clip of it on the timeline plays.
     * The camera is the view; the player is the character played; characters
     * are the residents about it; items are everything thrown or let fall;
     * vehicles are the ships and boats about it; weather is the drift of the
     * weather over the world and the gusts of the wind; time is the world's
     * clock, and with it the sun, moon, sky and all their light; the
     * interface is everything drawn over the world — every menu, the HUD,
     * the inventory and the vitality effects. A take's and a clip's channels
     * are a mask of these bits, and recording files name channels by their
     * lower-case constant names, which are also the keys the Recorder tab's
     * channel rows hand back.
     */

    CAMERA(EditorSetting.RECORDER_CHANNEL_LABEL_CAMERA),
    PLAYER(EditorSetting.RECORDER_CHANNEL_LABEL_PLAYER),
    CHARACTERS(EditorSetting.RECORDER_CHANNEL_LABEL_CHARACTERS),
    ITEMS(EditorSetting.RECORDER_CHANNEL_LABEL_ITEMS),
    VEHICLES(EditorSetting.RECORDER_CHANNEL_LABEL_VEHICLES),
    WEATHER(EditorSetting.RECORDER_CHANNEL_LABEL_WEATHER),
    TIME(EditorSetting.RECORDER_CHANNEL_LABEL_TIME),
    INTERFACE(EditorSetting.RECORDER_CHANNEL_LABEL_INTERFACE);

    // Values
    public static final RecorderChannel[] VALUES = values();
    public static final int ALL = (1 << VALUES.length) - 1;
    public static final int NONE = 0;

    // Internal
    private final String label;
    private final String key;

    // Constructor \\

    RecorderChannel(String label) {
        this.label = label;
        this.key = name().toLowerCase();
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

    public String getKey() {
        return key;
    }

    // The channel a key names, null for none
    public static RecorderChannel findByKey(String key) {

        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i].key.equals(key))
                return VALUES[i];

        return null;
    }
}
