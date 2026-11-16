package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;

public class CharacterStampContext extends CharacterToolContext {

    /*
     * The Stamp character tool tab: the whole character in the bind pose,
     * where the body part chosen in the Stamps tab is aimed at the body,
     * set down, turned and joined, its bones grafted onto the rig and its
     * mesh merged into the body's, with its twin across the body while
     * mirroring; the Tools tab holds its Scale, Twist and Mirror.
     */

    // Tool \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.STAMP;
    }

    @Override
    protected String getTabTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_STAMP;
    }
}
