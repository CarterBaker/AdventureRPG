package editor.bootstrap.charactereditorpipeline;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import engine.root.PipelinePackage;

public class CharacterEditorPipeline extends PipelinePackage {

    /*
     * Registers the character editor's shared state, so every Character
     * Editor tab works on the same character.
     */

    // Internal \\

    @Override
    protected void create() {
        create(CharacterEditorManager.class);
    }
}
