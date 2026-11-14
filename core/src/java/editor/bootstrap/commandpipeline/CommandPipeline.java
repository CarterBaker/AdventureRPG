package editor.bootstrap.commandpipeline;

import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import engine.root.PipelinePackage;

public class CommandPipeline extends PipelinePackage {

    /*
     * Registers the editor's command definitions and its one command route,
     * so every command console lists the same commands and submits to the
     * same manager, and every open Dev window receives the same commands.
     */

    // Internal \\

    @Override
    protected void create() {
        create(CommandManager.class);
    }
}
