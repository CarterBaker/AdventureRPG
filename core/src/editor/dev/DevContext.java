package editor.dev;

import application.runtime.RuntimeContext;
import editor.bootstrap.commandpipeline.command.CommandStruct;
import editor.dev.command.DevCommandSystem;

public class DevContext extends RuntimeContext {

    /*
     * Dev mode entry point for testing. Runs every system RuntimeContext runs,
     * exactly as the game does, and adds only a command system that receives
     * what is typed into the editor console. The editor pairs it with a Dev
     * Mode tab exactly as it does a preview. Dev-only systems live beside it
     * in the editor, never in runtime.
     */

    // Dev
    private DevCommandSystem devCommandSystem;

    // Internal \\

    @Override
    protected void create() {

        super.create();

        // Dev
        this.devCommandSystem = create(DevCommandSystem.class);
    }

    // Management \\

    public void queueCommand(CommandStruct command) {
        devCommandSystem.queueCommand(command);
    }
}
