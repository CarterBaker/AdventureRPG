package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;

public enum RecorderAssetKind {

    /*
     * The sections the recorder's Assets tab sorts a recording's assets into,
     * in the order it lists them: the world it was made in and its calendar,
     * every character template and gender, the features and items they wore
     * or threw, the vehicles about them, the models, textures and materials
     * all of those are drawn with, the animation trees and animations the
     * characters moved by, the interface targets the take captured, and the
     * sounds it heard or was given from the sound library.
     */

    WORLD(EditorSetting.RECORDER_ASSET_LABEL_WORLD),
    CALENDAR(EditorSetting.RECORDER_ASSET_LABEL_CALENDAR),
    CHARACTERS(EditorSetting.RECORDER_ASSET_LABEL_CHARACTERS),
    GENDERS(EditorSetting.RECORDER_ASSET_LABEL_GENDERS),
    FEATURES(EditorSetting.RECORDER_ASSET_LABEL_FEATURES),
    ITEMS(EditorSetting.RECORDER_ASSET_LABEL_ITEMS),
    VEHICLES(EditorSetting.RECORDER_ASSET_LABEL_VEHICLES),
    MODELS(EditorSetting.RECORDER_ASSET_LABEL_MODELS),
    TEXTURES(EditorSetting.RECORDER_ASSET_LABEL_TEXTURES),
    MATERIALS(EditorSetting.RECORDER_ASSET_LABEL_MATERIALS),
    ANIMATION_TREES(EditorSetting.RECORDER_ASSET_LABEL_ANIMATION_TREES),
    ANIMATIONS(EditorSetting.RECORDER_ASSET_LABEL_ANIMATIONS),
    INTERFACE(EditorSetting.RECORDER_ASSET_LABEL_INTERFACE),
    SOUNDS(EditorSetting.RECORDER_ASSET_LABEL_SOUNDS);

    // Values
    public static final RecorderAssetKind[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    RecorderAssetKind(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
