package editor.bootstrap.commandpipeline.command;

import engine.root.HandlePackage;

public class CommandHandle extends HandlePackage {

    /*
     * Persistent reference to one loaded console command. Registered and
     * owned by CommandManager. Delegates all accessors through CommandData.
     */

    // Internal
    private CommandData commandData;

    // Constructor \\

    public void constructor(CommandData commandData) {

        // Internal
        this.commandData = commandData;
    }

    // Accessible \\

    public CommandData getCommandData() {
        return commandData;
    }

    public String getCommandName() {
        return commandData.getCommandName();
    }

    public String getLabel() {
        return commandData.getLabel();
    }

    public String getGroupName() {
        return commandData.getGroupName();
    }

    public int getArgumentCount() {
        return commandData.getArgumentCount();
    }

    public String getArgumentName(int index) {
        return commandData.getArgumentName(index);
    }

    public String getUsage() {
        return commandData.getUsage();
    }

    public boolean isArgumentFree() {
        return commandData.getArgumentCount() == 0;
    }
}
