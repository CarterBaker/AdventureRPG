package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterRigContext extends CharacterToolContext {

    /*
     * The Rig character tool tab: the bones of the rig in the exact bind pose,
     * in the region picked in the Layers tab; the bone selected is the one the
     * Weights tab paints.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.RIG;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_RIG;
    }
}
