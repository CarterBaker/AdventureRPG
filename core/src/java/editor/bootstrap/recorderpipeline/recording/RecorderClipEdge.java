package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum RecorderClipEdge {

    /*
     * The two edges of a clip a transition can be laid on: its start, where
     * the timeline turns into it, and its end, where it turns out of it.
     * Recording files name each edge's transition by its lower-case constant
     * name, which is also the key the Recorder tab's transition rows hand
     * back.
     */

    START(EditorSetting.RECORDER_EDGE_LABEL_START),
    END(EditorSetting.RECORDER_EDGE_LABEL_END);

    // Values
    public static final RecorderClipEdge[] VALUES = values();

    // Internal
    private final String label;
    private final String key;

    // Constructor \\

    RecorderClipEdge(String label) {
        this.label = label;
        this.key = name().toLowerCase();
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public String getKey() {
        return key;
    }

    // The edge a key names, null for none
    public static RecorderClipEdge findByKey(String key) {

        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i].key.equals(key))
                return VALUES[i];

        return null;
    }
}
