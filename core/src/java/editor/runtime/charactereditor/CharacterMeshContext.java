package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterMeshContext extends CharacterToolContext {

    /*
     * The Mesh character tool tab: the vertices and faces of the part open, in
     * the region picked in the Layers tab.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.MESH;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_MESH;
    }
}
