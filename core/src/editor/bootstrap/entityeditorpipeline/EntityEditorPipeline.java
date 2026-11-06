package editor.bootstrap.entityeditorpipeline;

import editor.bootstrap.entityeditorpipeline.entityeditormanager.EntityEditorManager;
import engine.root.PipelinePackage;

public class EntityEditorPipeline extends PipelinePackage {

    /*
     * Registers the entity editor's shared state, so every Entity Editor
     * window, the Properties tab and the character tabs work on the same
     * entity, the race it is made of and the gender it is previewed as.
     */

    // Internal \\

    @Override
    protected void create() {
        create(EntityEditorManager.class);
    }
}
