package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterWeightsContext extends CharacterToolContext {

    /*
     * The Weights character tool tab: the bones each vertex of the part open
     * follows, painted for the bone selected here or in the Rig tab, in the
     * region picked in the Layers tab.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.WEIGHTS;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_WEIGHTS;
    }
}
