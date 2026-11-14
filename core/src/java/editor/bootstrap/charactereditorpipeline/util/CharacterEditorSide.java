package editor.bootstrap.charactereditorpipeline.util;

import editor.runtime.EditorSetting;

public enum CharacterEditorSide {

    /*
     * Which side of the body a part editor shows and frames when its part
     * has a twin across the body, eyes, hands and feet: every edit made on
     * the side shown is mirrored onto the other. Each side carries the
     * suffix naming its bones and the sign of x its vertices stand on.
     */

    LEFT(EditorSetting.CHARACTER_EDITOR_SIDE_LEFT, EditorSetting.CHARACTER_MESH_BONE_SUFFIX_LEFT, 1f),
    RIGHT(EditorSetting.CHARACTER_EDITOR_SIDE_RIGHT, EditorSetting.CHARACTER_MESH_BONE_SUFFIX_RIGHT, -1f);

    // Values
    public static final CharacterEditorSide[] VALUES = values();

    // Internal
    private final String label;
    private final String suffix;
    private final float sign;

    // Constructor \\

    CharacterEditorSide(String label, String suffix, float sign) {
        this.label = label;
        this.suffix = suffix;
        this.sign = sign;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public String getSuffix() {
        return suffix;
    }

    // Whether a bone is this side's by its name
    public boolean namesBone(String boneName) {
        return boneName.endsWith(suffix);
    }

    // Whether a point stands on this side of the body, the centre plane counting for both
    public boolean holdsX(float x) {
        return x * sign >= -EditorSetting.CHARACTER_MESH_CENTER_TOLERANCE;
    }
}
