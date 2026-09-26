package editor.bootstrap.commandpipeline.commandmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import editor.bootstrap.commandpipeline.command.CommandData;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import engine.editor.EditorSetting;
import engine.root.BuilderPackage;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CommandBuilder extends BuilderPackage {

    /*
     * Parses one command group JSON file into a CommandHandle per entry of its
     * "commands". Every command needs a name, which must be a single word so
     * it can be typed; the label defaults to the name, and "arguments" lists
     * the argument names in the order they are typed. A command that takes
     * no arguments runs with one click from the console's command tree.
     */

    // Build \\

    ObjectArrayList<CommandHandle> build(File file, String groupName) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        JsonArray commandsJson = JsonUtility.validateArray(json, "commands");
        ObjectArrayList<CommandHandle> commandHandles = new ObjectArrayList<>(commandsJson.size());

        for (int i = 0; i < commandsJson.size(); i++)
            commandHandles.add(buildCommand(commandsJson.get(i).getAsJsonObject(), groupName));

        return commandHandles;
    }

    private CommandHandle buildCommand(JsonObject commandJson, String groupName) {

        String commandName = JsonUtility.validateString(commandJson, "name");

        if (!isSingleWord(commandName))
            throwException("Command group '" + groupName + "' declares command '" + commandName
                    + "', but a command name must be a single word.");

        String label = JsonUtility.getString(commandJson, "label", commandName);
        String[] argumentNames = parseArgumentNames(commandJson, commandName, groupName);

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

    private String[] parseArgumentNames(JsonObject commandJson, String commandName, String groupName) {

        if (!JsonUtility.hasArray(commandJson, "arguments"))
            return new String[0];

        JsonArray argumentsJson = JsonUtility.validateArray(commandJson, "arguments");
        String[] argumentNames = new String[argumentsJson.size()];

        for (int i = 0; i < argumentNames.length; i++) {

            argumentNames[i] = argumentsJson.get(i).getAsString();

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
