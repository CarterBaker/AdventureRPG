package editor.bootstrap.commandpipeline.command;

import engine.root.StructPackage;

public class CommandStruct extends StructPackage {

    /*
     * One submitted console command as every receiver sees it: the line as it
     * was typed, the command name that leads it, and the arguments after it.
     * Parsed once and shared by every Dev window it is routed to.
     */

    // Identity
    private final String commandLine;
    private final String commandName;

    // Arguments
    private final String[] arguments;

    // Constructor \\

    public CommandStruct(String commandLine, String commandName, String[] arguments) {

        // Identity
        this.commandLine = commandLine;
        this.commandName = commandName;

        // Arguments
        this.arguments = arguments;
    }

    // Accessible \\

    public String getCommandLine() {
        return commandLine;
    }

    public String getCommandName() {
        return commandName;
    }

    public int getArgumentCount() {
        return arguments.length;
    }

    public String getArgument(int index) {
        return arguments[index];
    }
}
