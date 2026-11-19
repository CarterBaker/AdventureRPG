package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterTopologyContext extends CharacterToolContext {

    /*
     * The Topology character tool tab: the edges and faces the part's
     * vertices are joined by, in the region picked in the Layers tab, its
     * wireframe drawn whole over it.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.TOPOLOGY;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_TOPOLOGY;
    }
}
