package editor.runtime.handeditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.runtime.charactereditor.CharacterModelingContext;

public class HandEditorContext extends CharacterModelingContext {

    /*
     * Editor tab for the hands, shown one side at a time and framed on it
     * with its fingers: the body's mesh where it weighs on the bones the rig
     * names as its hands, sculpted and weighted, and the hand and finger
     * bones rigged, every edit mirrored to the other hand.
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
