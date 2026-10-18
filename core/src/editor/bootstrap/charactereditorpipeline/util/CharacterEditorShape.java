package editor.bootstrap.charactereditorpipeline.util;

import editor.runtime.EditorSetting;

public enum CharacterEditorShape {

    /*
     * The shapes a new part of a character mesh starts as, centred on a
     * bone's joint and weighted wholly to it: a single upright quad, a box, a
     * capped cylinder standing along the bone, and a sphere of quad rings
     * closed at its poles.
     */

    PLANE(EditorSetting.CHARACTER_EDITOR_SHAPE_PLANE),
    BOX(EditorSetting.CHARACTER_EDITOR_SHAPE_BOX),
    CYLINDER(EditorSetting.CHARACTER_EDITOR_SHAPE_CYLINDER),
    SPHERE(EditorSetting.CHARACTER_EDITOR_SHAPE_SPHERE);

    // Values
    public static final CharacterEditorShape[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    CharacterEditorShape(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
