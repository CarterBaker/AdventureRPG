package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterFaceContext extends CharacterToolContext {

    /*
     * The Face character tool tab: the controls that shape the face through
     * its bones, framed on the Head.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.FACE;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_FACE;
    }
}
