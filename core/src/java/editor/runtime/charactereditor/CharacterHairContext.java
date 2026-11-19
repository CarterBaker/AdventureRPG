package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.runtime.EditorSetting;

public class CharacterHairContext extends CharacterToolContext {

    /*
     * The Hair character tool tab: the strands of the groom open, grown over
     * the whole character's surface and groomed with the Tools and Brushes
     * tabs, the material they are painted with picked in the Hair Materials
     * tab. Its File and Edit menus make, open, save, revert, undo, export and
     * import the groom rather than the character.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.HAIR;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_HAIR;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_HAIR;
    }
}
