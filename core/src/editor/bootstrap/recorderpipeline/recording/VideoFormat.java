package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum VideoFormat {

    /*
     * The videos a recording exports to, every one written by the engine
     * itself so an export never needs anything installed. MP4 is H.264 for
     * sharing and watching, MOV is ProRes 422 HQ for editing, AVI is Motion
     * JPEG.
     */

    MP4(EditorSetting.RECORDER_VIDEO_EXTENSION_MP4, EditorSetting.RECORDER_VIDEO_LABEL_MP4),
    MOV(EditorSetting.RECORDER_VIDEO_EXTENSION_MOV, EditorSetting.RECORDER_VIDEO_LABEL_MOV),
    AVI(EditorSetting.RECORDER_VIDEO_EXTENSION_AVI, EditorSetting.RECORDER_VIDEO_LABEL_AVI);

    // Values
    public static final VideoFormat[] VALUES = values();

    // Internal
    private final String extension;
    private final String label;

    // Constructor \\

    VideoFormat(String extension, String label) {
        this.extension = extension;
        this.label = label;
    }

    // Accessible \\

    public String getExtension() {
        return extension;
    }

    public String getLabel() {
        return label;
    }
}
