package editor.bootstrap.recorderpipeline.recording;

public enum RecorderExportResult {

    /*
     * What became of the last export asked for. Refused, it never began and
     * the recorder's status says why; exporting, it is rendering; exported,
     * failed and cancelled, it has ended that way and the status names the
     * video or the reason.
     */

    NONE,
    REFUSED,
    EXPORTING,
    EXPORTED,
    FAILED,
    CANCELLED;

    // Values
    public static final RecorderExportResult[] VALUES = values();
}
