package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum RecorderLayerView {

    /*
     * How the Layers tab lists the recorder while a Recorder tab is worked
     * in, each pane keeping its own: the layers of the selected clip's take —
     * the camera, the player, the characters, items and vehicles it recorded,
     * the weather, the clock and its sounds — the one picked being what the
     * Keys tab shows; or every take in the bin, the one picked being what Lay
     * Take lays.
     */

    LAYERS(EditorSetting.RECORDER_LAYER_VIEW_LABEL_LAYERS),
    TAKES(EditorSetting.RECORDER_LAYER_VIEW_LABEL_TAKES);

    // Values
    public static final RecorderLayerView[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    RecorderLayerView(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
