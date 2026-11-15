package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterUVContext extends CharacterToolContext {

    /*
     * The UV character tool tab: the seams that cut the part open into
     * islands, marked on the edges between selected vertices, and the faces
     * selected unwrapped flat, in the region picked in the Layers tab; the
     * UV Editor beside it lays out the UVs of the faces selected here.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.UV;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_UV;
    }
}
