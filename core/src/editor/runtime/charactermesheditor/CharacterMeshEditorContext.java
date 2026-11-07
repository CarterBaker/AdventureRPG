package editor.runtime.charactermesheditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.runtime.charactereditor.CharacterModelingContext;

public class CharacterMeshEditorContext extends CharacterModelingContext {

    /*
     * Editor tab for modelling the whole character: every worn part's mesh
     * and the rig, framed on the whole body. How the character looks is
     * shaped in the Character Appearance, Build and Equipment tabs beside it.
     */

    // Window \\

    @Override
    public CharacterEditorRegion getRegion() {
        return CharacterEditorRegion.WHOLE;
    }

    @Override
    public CharacterEditorMode getStartingMode() {
        return CharacterEditorMode.MESH;
    }
}
