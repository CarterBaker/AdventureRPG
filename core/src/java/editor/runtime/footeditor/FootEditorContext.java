package editor.runtime.footeditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.runtime.charactereditor.CharacterModelingContext;

public class FootEditorContext extends CharacterModelingContext {

    /*
     * Editor tab for the feet, shown one side at a time and framed on it
     * with its toes: the body's mesh where it weighs on the bones the rig
     * names as its feet, sculpted and weighted, and the foot and toe bones
     * rigged, every edit mirrored to the other foot.
     */

    // Window \\

    @Override
    public CharacterEditorRegion getRegion() {
        return CharacterEditorRegion.FEET;
    }

    @Override
    public CharacterEditorMode getStartingMode() {
        return CharacterEditorMode.MESH;
    }
}
