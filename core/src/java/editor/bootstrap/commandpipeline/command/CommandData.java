package editor.bootstrap.commandpipeline.command;

import engine.root.DataPackage;

public class CommandData extends DataPackage {

    /*
     * Immutable definition of one console command: the name typed to run it,
     * the label the command console's tree shows for it, the group it is
     * listed under, the names of the arguments it takes, the usage line
     * reported when it is typed with the wrong number of them, whether it
     * acts on what every window shares, so it runs in one Dev window only,
     * and the editor tool it runs in.
     */

    // Identity
    private final String commandName;
    private final String label;
    private final String groupName;

    // Arguments
    private final String[] argumentNames;
    private final String usage;

    // Scope
    private final boolean shared;
    private final CommandTarget target;

    // Constructor \\

    public CommandData(
            String commandName,
            String label,
            String groupName,
            String[] argumentNames,
            String usage,
            boolean shared,
            CommandTarget target) {

        // Identity
        this.commandName = commandName;
        this.label = label;
        this.groupName = groupName;

        // Arguments
        this.argumentNames = argumentNames;
        this.usage = usage;

        // Scope
        this.shared = shared;
        this.target = target;
    }

    // Accessible \\

    public String getCommandName() {
        return commandName;
    }

    public String getLabel() {
        return label;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getArgumentCount() {
        return argumentNames.length;
    }

    public String getArgumentName(int index) {
        return argumentNames[index];
    }

    public String getUsage() {
        return usage;
    }

    public boolean isShared() {
        return shared;
    }

    public CommandTarget getTarget() {
        return target;
    }
}
