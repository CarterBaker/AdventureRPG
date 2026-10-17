package editor.bootstrap.charactereditorpipeline.util;

import editor.runtime.EditorSetting;

public enum CharacterEditorColor {

    /*
     * The two colors a character's appearance carries, skin and hair. Each
     * has a default color field and a palette of tones in the template, and
     * its own row keys in the character editor's panel.
     */

    SKIN(
            EditorSetting.CHARACTER_EDITOR_HEADER_SKIN,
            EditorSetting.CHARACTER_EDITOR_FIELD_SKIN_COLOR,
            EditorSetting.CHARACTER_EDITOR_FIELD_SKIN_PALETTE,
            EditorSetting.CHARACTER_EDITOR_KEY_SKIN),
    HAIR(
            EditorSetting.CHARACTER_EDITOR_HEADER_HAIR,
            EditorSetting.CHARACTER_EDITOR_FIELD_HAIR_COLOR,
            EditorSetting.CHARACTER_EDITOR_FIELD_HAIR_PALETTE,
            EditorSetting.CHARACTER_EDITOR_KEY_HAIR);

    // Values
    public static final CharacterEditorColor[] VALUES = values();

    // Internal
    private final String label;
    private final String colorField;
    private final String paletteField;
    private final String keyPrefix;

    // Constructor \\

    CharacterEditorColor(String label, String colorField, String paletteField, String keyPrefix) {
        this.label = label;
        this.colorField = colorField;
        this.paletteField = paletteField;
        this.keyPrefix = keyPrefix;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public String getColorField() {
        return colorField;
    }

    public String getPaletteField() {
        return paletteField;
    }

    // The default color's channels
    public String getColorKey() {
        return keyPrefix + EditorSetting.CHARACTER_EDITOR_KEY_COLOR_SUFFIX;
    }

    // The palette tone the preview wears
    public String getToneKey() {
        return keyPrefix + EditorSetting.CHARACTER_EDITOR_KEY_TONE_SUFFIX;
    }

    // The channels of the tone the preview wears
    public String getToneColorKey() {
        return keyPrefix + EditorSetting.CHARACTER_EDITOR_KEY_TONE_COLOR_SUFFIX;
    }

    public String getAddToneKey() {
        return keyPrefix + EditorSetting.CHARACTER_EDITOR_KEY_ADD_TONE_SUFFIX;
    }

    public String getRemoveToneKey() {
        return keyPrefix + EditorSetting.CHARACTER_EDITOR_KEY_REMOVE_TONE_SUFFIX;
    }

    public static CharacterEditorColor fromKeyName(String keyName) {

        for (CharacterEditorColor color : VALUES)
            if (keyName.startsWith(color.keyPrefix))
                return color;

        return null;
    }
}
