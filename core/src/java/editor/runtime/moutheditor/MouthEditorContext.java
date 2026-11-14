package editor.runtime.moutheditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.runtime.charactereditor.CharacterModelingContext;

public class MouthEditorContext extends CharacterModelingContext {

    /*
     * Editor tab for the mouth, framed on the jaw: the lips, the jaw and the
     * mouth's inside where the body weighs on the rig's mouth bones,
     * sculpted and weighted, and the teeth and tongue worn there, each picked
     * in the outliner's parts, every edit mirrored across the face while
     * mirror is on.
     */

    // Window \\

    @Override
    public CharacterEditorRegion getRegion() {
        return CharacterEditorRegion.MOUTH;
    }

    @Override
    public CharacterEditorMode getStartingMode() {
        return CharacterEditorMode.SCULPT;
    }
}
