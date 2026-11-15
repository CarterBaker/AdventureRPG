package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterPaintContext extends CharacterToolContext {

    /*
     * The Paint character tool tab: the texture maps of the part open, painted
     * on the paint layer picked in the Layers tab.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.PAINT;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_PAINT;
    }
}
