package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterEquipmentContext extends CharacterToolContext {

    /*
     * The Equipment character tool tab: the anchors worn and held gear hangs
     * from, posed as its outliner picks.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.EQUIPMENT;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_EQUIPMENT;
    }
}
