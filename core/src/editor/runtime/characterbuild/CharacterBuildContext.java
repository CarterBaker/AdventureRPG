package editor.runtime.characterbuild;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.charactereditor.CharacterLookContext;

public class CharacterBuildContext extends CharacterLookContext {

    /*
     * Editor tab for a character's build: the build curve from thin to
     * heavy, the preview's weight, head proportions and the girth of each
     * bone, on the live preview with its joints marked. Everything a
     * Character Editor tab does, and every value it shows in the Properties
     * tab, is CharacterLookContext's.
     */

    // Window \\

    @Override
    public CharacterEditorMode getStartingMode() {
        return CharacterEditorMode.BUILD;
    }
}
