package editor.bootstrap.commandpipeline.commandmanager;

import java.util.Arrays;

import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.bootstrap.commandpipeline.command.CommandStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.dev.DevContext;
import engine.editor.EditorSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandManager extends ManagerPackage {

    /*
     * Owns every console command and is the one route from the command
     * console to every open Dev window. Commands load from JSON, one group per
     * file, and groups are listed in name order. A submitted line is echoed to
     * the log, parsed once into a CommandStruct, checked against its command's
     * definition, and queued on every Dev window that has not crashed, each of
     * which runs it inside its own frame. TabManager's open tabs are the only
     * record of which Dev windows exist, so nothing registers here.
     */

    // Internal
    private TabManager tabManager;

    // Palette
    private Object2ObjectOpenHashMap<String, CommandHandle> commandName2CommandHandle;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<CommandHandle>> groupName2CommandHandles;
    private ObjectArrayList<String> groupNames;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.commandName2CommandHandle = new Object2ObjectOpenHashMap<>();
        this.groupName2CommandHandles = new Object2ObjectOpenHashMap<>();
        this.groupNames = new ObjectArrayList<>();

        create(CommandLoader.class);
    }

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
    }

    @Override
    protected void awake() {

        ((CommandLoader) internalLoader).requestAll();
        groupNames.sort(String.CASE_INSENSITIVE_ORDER);
    }

    // Management \\

    void addCommandHandle(CommandHandle commandHandle) {

        String commandName = commandHandle.getCommandName();
        String groupName = commandHandle.getGroupName();

        if (commandName2CommandHandle.containsKey(commandName))
            throwException("Command '" + commandName + "' is declared by both group '"
                    + commandName2CommandHandle.get(commandName).getGroupName() + "' and group '" + groupName + "'.");

        commandName2CommandHandle.put(commandName, commandHandle);

        if (!groupName2CommandHandles.containsKey(groupName)) {
            groupName2CommandHandles.put(groupName, new ObjectArrayList<>());
            groupNames.add(groupName);
        }

        groupName2CommandHandles.get(groupName).add(commandHandle);
    }

    public void executeCommand(String commandLine) {

        String trimmedLine = commandLine.trim();

        if (trimmedLine.isEmpty())
            return;

        log(EditorSetting.COMMAND_ECHO_PREFIX + trimmedLine);

        CommandStruct command = parseCommand(trimmedLine);
        CommandHandle commandHandle = commandName2CommandHandle.get(command.getCommandName());

        if (commandHandle == null) {
            errorLog(EditorSetting.COMMAND_MESSAGE_UNKNOWN + command.getCommandName());
            return;
        }

        if (command.getArgumentCount() != commandHandle.getArgumentCount()) {
            errorLog(EditorSetting.COMMAND_MESSAGE_USAGE + commandHandle.getUsage());
            return;
        }

        if (dispatchCommand(command) == 0)
            log(EditorSetting.COMMAND_MESSAGE_NO_DEV_WINDOWS);
    }

    private CommandStruct parseCommand(String commandLine) {

        String[] tokens = commandLine.split(EditorSetting.COMMAND_TOKEN_SEPARATOR_PATTERN);

        return new CommandStruct(commandLine, tokens[0], Arrays.copyOfRange(tokens, 1, tokens.length));
    }

    private int dispatchCommand(CommandStruct command) {

        ObjectArrayList<TabHandle> openTabs = tabManager.getOpenTabs();
        int receivers = 0;

        for (int i = 0; i < openTabs.size(); i++) {

            if (!(openTabs.get(i).getTabContext().getContentContext() instanceof DevContext devContext)
                    || devContext.isCrashed())
                continue;

            devContext.queueCommand(command);
            receivers++;
        }

        return receivers;
    }

    // Accessible \\

    public ObjectArrayList<String> getGroupNames() {
        return groupNames;
    }

    public ObjectArrayList<CommandHandle> getCommandHandles(String groupName) {
        return groupName2CommandHandles.get(groupName);
    }
}
