package editor.dev.command;

import editor.bootstrap.commandpipeline.command.CommandStruct;
import engine.editor.EditorSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DevCommandSystem extends SystemPackage {

    /*
     * Receives the commands the editor console routes to this Dev window and
     * runs them during this window's own update, inside its crash boundary.
     * No command acts yet — each one is acknowledged in the log against the
     * window that received it.
     */

    // Commands
    private ObjectArrayList<CommandStruct> pendingCommands;

    // Base \\

    @Override
    protected void create() {

        // Commands
        this.pendingCommands = new ObjectArrayList<>();
    }

    // Update \\

    @Override
    protected void update() {

        for (int i = 0; i < pendingCommands.size(); i++)
            executeCommand(pendingCommands.get(i));

        pendingCommands.clear();
    }

    private void executeCommand(CommandStruct command) {
        log(context.getWindow().getTitle() + EditorSetting.COMMAND_MESSAGE_RECEIVED + command.getCommandName());
    }

    // Management \\

    public void queueCommand(CommandStruct command) {
        pendingCommands.add(command);
    }
}
