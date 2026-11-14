package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum RecordingSourceKind {

    /*
     * What a clip on a recording's timeline plays: a take from its own bin, a
     * whole other recording laid inside it by name as a nested sequence is, a
     * song from the project's songs by name, heard through the sampler, or a
     * notes clip of its own — an instrument and its notes, timed by the
     * recording's tempo map. Recording files name a clip's source by its
     * lower-case constant name, and the Assets tab lists what each names in
     * a section of its own.
     */

    TAKE(EditorSetting.RECORDER_SOURCE_LABEL_TAKE, RecorderAssetKind.SOUNDS),
    RECORDING(EditorSetting.RECORDER_SOURCE_LABEL_RECORDING, RecorderAssetKind.RECORDINGS),
    SONG(EditorSetting.RECORDER_SOURCE_LABEL_SONG, RecorderAssetKind.SONGS),
    NOTES(EditorSetting.RECORDER_SOURCE_LABEL_NOTES, RecorderAssetKind.INSTRUMENTS);

    // Values
    public static final RecordingSourceKind[] VALUES = values();

    // Internal
    private final String label;
    private final RecorderAssetKind assetKind;

    // Constructor \\

    RecordingSourceKind(String label, RecorderAssetKind assetKind) {
        this.label = label;
        this.assetKind = assetKind;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    // The section of the Assets tab what a clip of the source names is listed in; a take's own assets are gathered
    // from its tracks instead
    public RecorderAssetKind getAssetKind() {
        return assetKind;
    }
}
