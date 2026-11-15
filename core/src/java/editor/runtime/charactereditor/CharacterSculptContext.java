package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterSculptContext extends CharacterToolContext {

    /*
     * The Sculpt character tool tab: the surface of the part open under the
     * brush, in the region picked in the Layers tab.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.SCULPT;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_SCULPT;
    }
}
