package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum RecorderKeyMode {

    /*
     * How a key's handles stand. Auto aims both along the curve's own slope
     * through the key, flat at a peak or a trough, so the curve passes
     * through smoothly and never overshoots its neighbours; aligned keeps
     * both on one line as either is dragged, so the curve bends but stays
     * smooth through the key; free lets each go its own way for a sharp
     * turn; linear aims each straight at its neighbour; constant holds the
     * key's value until the next key. Dragging the handle of an auto,
     * linear or constant key makes it aligned.
     */

    AUTO(EditorSetting.RECORDER_KEY_MODE_LABEL_AUTO),
    ALIGNED(EditorSetting.RECORDER_KEY_MODE_LABEL_ALIGNED),
    FREE(EditorSetting.RECORDER_KEY_MODE_LABEL_FREE),
    LINEAR(EditorSetting.RECORDER_KEY_MODE_LABEL_LINEAR),
    CONSTANT(EditorSetting.RECORDER_KEY_MODE_LABEL_CONSTANT);

    // Values
    public static final RecorderKeyMode[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    RecorderKeyMode(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    // True for a key whose handles the curve sets itself
    public boolean isResolved() {
        return this == AUTO || this == LINEAR || this == CONSTANT;
    }
}
