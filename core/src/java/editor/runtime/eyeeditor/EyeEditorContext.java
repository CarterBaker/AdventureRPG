package editor.runtime.eyeeditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.runtime.charactereditor.CharacterModelingContext;

public class EyeEditorContext extends CharacterModelingContext {

    /*
     * Editor tab for the eyes, shown one side at a time and framed on it:
     * the socket and lids of the head where the body weighs on the rig's eye
     * bones, sculpted and weighted, and the eyes worn in them, each picked in
     * the outliner's parts, every edit mirrored to the other eye.
     */

    // Window \\

    @Override
    public CharacterEditorRegion getRegion() {
        return CharacterEditorRegion.EYES;
    }

    @Override
    public CharacterEditorMode getStartingMode() {
        return CharacterEditorMode.SCULPT;
    }
}
