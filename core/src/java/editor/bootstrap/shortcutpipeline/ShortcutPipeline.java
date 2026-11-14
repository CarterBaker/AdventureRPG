package editor.bootstrap.shortcutpipeline;

import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import engine.root.PipelinePackage;

public class ShortcutPipeline extends PipelinePackage {

    /*
     * Registers the editor's keyboard shortcuts and their one dispatch, so
     * every tool reads its keys from the same files and runs them through the
     * same route its menu buttons take.
     */

    // Internal \\

    @Override
    protected void create() {
        create(ShortcutManager.class);
    }
}
