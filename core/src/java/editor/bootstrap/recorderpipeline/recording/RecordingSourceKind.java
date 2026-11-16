package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum RecordingSourceKind {

    /*
     * What a clip on a recording's timeline plays: a take from its own bin, a
     * whole other recording laid inside it by name as a nested sequence is, a
     * song from the project's songs by name, heard through the sampler, or a
     * notes clip of its own — an instrument and its notes, timed by the
     * recording's tempo map. Recording files name a clip's source by its
     * lower-case constant name.
     */

    TAKE(EditorSetting.RECORDER_SOURCE_LABEL_TAKE),
    RECORDING(EditorSetting.RECORDER_SOURCE_LABEL_RECORDING),
    SONG(EditorSetting.RECORDER_SOURCE_LABEL_SONG),
    NOTES(EditorSetting.RECORDER_SOURCE_LABEL_NOTES);

    // Values
    public static final RecordingSourceKind[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    RecordingSourceKind(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
