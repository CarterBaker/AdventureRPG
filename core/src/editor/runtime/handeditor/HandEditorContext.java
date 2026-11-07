package editor.runtime.handeditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.runtime.charactereditor.CharacterModelingContext;

public class HandEditorContext extends CharacterModelingContext {

    /*
     * Editor tab for the hands, framed on the left with its fingers: the
     * body's mesh where it weighs on the hands, sculpted and weighted, and
     * the hand and finger bones rigged, every edit mirrored to the right
     * hand while mirror is on.
     */

    // Window \\

    @Override
    public CharacterEditorRegion getRegion() {
        return CharacterEditorRegion.HANDS;
    }

    @Override
    public CharacterEditorMode getStartingMode() {
        return CharacterEditorMode.MESH;
    }
}
