package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum VideoFormat {

    /*
     * The videos a recording exports to. MP4 is H.264 for sharing, MOV is
     * ProRes 422 HQ for editing, both encoded by an installed ffmpeg; AVI is
     * Motion JPEG, written by the engine itself, and always available.
     */

    MP4(EditorSetting.RECORDER_VIDEO_EXTENSION_MP4, EditorSetting.RECORDER_VIDEO_LABEL_MP4,
            EditorSetting.RECORDER_FFMPEG_ARGUMENTS_MP4),
    MOV(EditorSetting.RECORDER_VIDEO_EXTENSION_MOV, EditorSetting.RECORDER_VIDEO_LABEL_MOV,
            EditorSetting.RECORDER_FFMPEG_ARGUMENTS_MOV),
    AVI(EditorSetting.RECORDER_VIDEO_EXTENSION_AVI, EditorSetting.RECORDER_VIDEO_LABEL_AVI, null);

    // Values
    public static final VideoFormat[] VALUES = values();

    // Internal
    private final String extension;
    private final String label;
    private final String[] ffmpegArguments;

    // Constructor \\

    VideoFormat(String extension, String label, String[] ffmpegArguments) {
        this.extension = extension;
        this.label = label;
        this.ffmpegArguments = ffmpegArguments;
    }

    // Accessible \\

    public String getExtension() {
        return extension;
    }

    public String getLabel() {
        return label;
    }

    public boolean needsFfmpeg() {
        return ffmpegArguments != null;
    }

    public String[] getFfmpegArguments() {
        return ffmpegArguments;
    }
}
