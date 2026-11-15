package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterAppearanceContext extends CharacterToolContext {

    /*
     * The Appearance character tool tab: the look the game's creator offers
     * the player: gender, features, marks, colors, details and every shape
     * slider, on the live preview standing idle.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.APPEARANCE;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_APPEARANCE;
    }
}
