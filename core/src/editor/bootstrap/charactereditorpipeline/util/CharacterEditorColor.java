package editor.bootstrap.charactereditorpipeline.util;

import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import editor.runtime.EditorSetting;
import engine.util.arpg.ArpgUtility;

public enum CharacterEditorColor {

    /*
     * The colors a character's appearance carries, one per SkinnedColor in
     * the same order. Each has a default color in the template's "colors"
     * block and a palette of tones in its "palettes" block, both under the
     * color's own name, and its own row keys in the character editor's panel.
     */

    SKIN(SkinnedColor.SKIN),
    EYES(SkinnedColor.EYES),
    HAIR(SkinnedColor.HAIR),
    BROWS(SkinnedColor.BROWS),
    LIPS(SkinnedColor.LIPS),
    BLUSH(SkinnedColor.BLUSH),
    FRECKLES(SkinnedColor.FRECKLES),
    CLOTH(SkinnedColor.CLOTH),
    NIPPLES(SkinnedColor.NIPPLES),
    BODY_HAIR(SkinnedColor.BODY_HAIR),
    TEETH(SkinnedColor.TEETH);

    // Values
    public static final CharacterEditorColor[] VALUES = values();

    // Internal
    private final SkinnedColor skinnedColor;
    private final String field;
    private final String keyPrefix;

    // Constructor \\

    CharacterEditorColor(SkinnedColor skinnedColor) {
        this.skinnedColor = skinnedColor;
        this.field = ArpgUtility.toEnumName(skinnedColor);
        this.keyPrefix = EditorSetting.CHARACTER_EDITOR_KEY_COLOR_PREFIX + field;
    }

    // Accessible \\

    public SkinnedColor getSkinnedColor() {
        return skinnedColor;
    }

    public String getLabel() {
        return EditorSetting.CHARACTER_EDITOR_COLOR_HEADERS[skinnedColor.ordinal()];
    }

    // The color's key in the template's "colors" and "palettes" blocks
    public String getField() {
        return field;
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
            if (keyName.startsWith(color.keyPrefix + EditorSetting.CHARACTER_EDITOR_WORD_SEPARATOR))
                return color;

        return null;
    }
}
