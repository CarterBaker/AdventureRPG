package editor.bootstrap.commandpipeline.commandmanager;

import java.io.File;

import editor.bootstrap.commandpipeline.command.CommandData;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.runtime.EditorSetting;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CommandBuilder extends BuilderPackage {

    /*
     * Parses one command group ARPG file into a CommandHandle per entry of its
     * "commands". Every command needs a name, which must be a single word so
     * it can be typed; the label defaults to the name, and "arguments" lists
     * the argument names in the order they are typed. A command that takes
     * no arguments runs with one click from the command console's tree.
     */

    // Build \\

    ObjectArrayList<CommandHandle> build(File file, String groupName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        ArpgArrayStruct commandsArpg = ArpgUtility.validateArray(arpg, "commands");
        ObjectArrayList<CommandHandle> commandHandles = new ObjectArrayList<>(commandsArpg.size());

        for (int i = 0; i < commandsArpg.size(); i++)
            commandHandles.add(buildCommand(commandsArpg.get(i).getAsObject(), groupName));

        return commandHandles;
    }

    private CommandHandle buildCommand(ArpgObjectStruct commandArpg, String groupName) {

        String commandName = ArpgUtility.validateString(commandArpg, "name");

        if (!isSingleWord(commandName))
            throwException("Command group '" + groupName + "' declares command '" + commandName
                    + "', but a command name must be a single word.");

        String label = ArpgUtility.getString(commandArpg, "label", commandName);
        String[] argumentNames = parseArgumentNames(commandArpg, commandName, groupName);

        CommandData commandData = new CommandData(
                commandName,
                label,
                groupName,
                argumentNames,
                buildUsage(commandName, argumentNames));

        CommandHandle commandHandle = create(CommandHandle.class);
        commandHandle.constructor(commandData);

        return commandHandle;
    }

    // Parse \\

    private String[] parseArgumentNames(ArpgObjectStruct commandArpg, String commandName, String groupName) {

        if (!ArpgUtility.hasArray(commandArpg, "arguments"))
            return new String[0];

        ArpgArrayStruct argumentsArpg = ArpgUtility.validateArray(commandArpg, "arguments");
        String[] argumentNames = new String[argumentsArpg.size()];

        for (int i = 0; i < argumentNames.length; i++) {

            argumentNames[i] = argumentsArpg.get(i).getAsString();

            if (!isSingleWord(argumentNames[i]))
                throwException("Command '" + commandName + "' in group '" + groupName + "' declares argument '"
                        + argumentNames[i] + "', but an argument name must be a single word.");
        }

        return argumentNames;
    }

    private String buildUsage(String commandName, String[] argumentNames) {

        StringBuilder usage = new StringBuilder(commandName);

        for (int i = 0; i < argumentNames.length; i++)
            usage.append(EditorSetting.COMMAND_ARGUMENT_OPEN)
                    .append(argumentNames[i])
                    .append(EditorSetting.COMMAND_ARGUMENT_CLOSE);

        return usage.toString();
    }

    // Utility \\

    private boolean isSingleWord(String text) {
        return !text.isEmpty() && text.split(EditorSetting.COMMAND_TOKEN_SEPARATOR_PATTERN)[0].equals(text);
    }
}
