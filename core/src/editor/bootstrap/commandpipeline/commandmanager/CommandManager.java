package editor.bootstrap.commandpipeline.commandmanager;

import java.util.Arrays;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.bootstrap.commandpipeline.command.CommandStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.dev.DevContext;
import editor.runtime.EditorSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandManager extends ManagerPackage {

    /*
     * Owns every console command and is the one route from the command
     * console to every open Dev window. Commands load from ARPG, one group per
     * file, and groups are listed in name order. A submitted line is echoed to
     * the log, parsed once into a CommandStruct, checked against its command's
     * definition, and queued on every Dev window that has not crashed, each of
     * which runs it inside its own frame — or on one Dev window alone, when a
     * command is dropped onto it or acts on what every window shares. Commands carried between windows belong to
     * the drag branch. TabManager's open tabs are the only record of which Dev
     * windows exist, so nothing registers here. A command name being typed
     * completes as far as every command it could still be agrees, and lists
     * them when that is no further.
     */

    // Internal
    private TabManager tabManager;
    private CommandDragBranch commandDragBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, CommandHandle> commandName2CommandHandle;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<CommandHandle>> groupName2CommandHandles;
    private ObjectArrayList<String> groupNames;
    private ObjectArrayList<String> commandNames;

    // Scratch
    private ObjectArrayList<String> matchingCommandNames;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.commandName2CommandHandle = new Object2ObjectOpenHashMap<>();
        this.groupName2CommandHandles = new Object2ObjectOpenHashMap<>();
        this.groupNames = new ObjectArrayList<>();
        this.commandNames = new ObjectArrayList<>();

        // Scratch
        this.matchingCommandNames = new ObjectArrayList<>();

        create(CommandLoader.class);
        this.commandDragBranch = create(CommandDragBranch.class);
    }

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
        groupNames.sort(String.CASE_INSENSITIVE_ORDER);
        commandNames.sort(String.CASE_INSENSITIVE_ORDER);
    }

    // Management \\

    void addCommandHandle(CommandHandle commandHandle) {

        String commandName = commandHandle.getCommandName();
        String groupName = commandHandle.getGroupName();

        if (commandName2CommandHandle.containsKey(commandName))
            throwException("Command '" + commandName + "' is declared by both group '"
                    + commandName2CommandHandle.get(commandName).getGroupName() + "' and group '" + groupName + "'.");

        commandName2CommandHandle.put(commandName, commandHandle);
        commandNames.add(commandName);

        if (!groupName2CommandHandles.containsKey(groupName)) {
            groupName2CommandHandles.put(groupName, new ObjectArrayList<>());
            groupNames.add(groupName);
        }

        groupName2CommandHandles.get(groupName).add(commandHandle);
    }

    public void executeCommand(String commandLine) {

        CommandStruct command = prepareCommand(commandLine);

        if (command != null && dispatchCommand(command) == 0)
            log(EditorSetting.COMMAND_MESSAGE_NO_DEV_WINDOWS);
    }

    public void executeCommand(String commandLine, DevContext devContext) {

        CommandStruct command = prepareCommand(commandLine);

        if (command == null)
            return;

        if (devContext.isCrashed()) {
            errorLog(devContext.getWindow().getTitle() + EditorSetting.COMMAND_MESSAGE_WINDOW_CRASHED
                    + command.getCommandName());
            return;
        }

        devContext.queueCommand(command);
    }

    public void dragCommand(String commandLine, String label, WindowInstance window, ElementInstance element) {
        commandDragBranch.dragCommand(commandLine, label, window, element);
    }

    // The longest name every command starting with this prefix shares, followed by a separator once only one does
    public String completeCommandName(String prefix) {

        matchingCommandNames.clear();

        for (int i = 0; i < commandNames.size(); i++)
            if (commandNames.get(i).startsWith(prefix))
                matchingCommandNames.add(commandNames.get(i));

        if (matchingCommandNames.isEmpty())
            return prefix;

        if (matchingCommandNames.size() == 1)
            return matchingCommandNames.get(0) + EditorSetting.COMMAND_TOKEN_SEPARATOR;

        String completion = resolveSharedPrefix(matchingCommandNames);

        if (completion.length() == prefix.length())
            log(EditorSetting.COMMAND_MESSAGE_MATCHES
                    + String.join(EditorSetting.COMMAND_MESSAGE_LIST_SEPARATOR, matchingCommandNames));

        return completion;
    }

    private String resolveSharedPrefix(ObjectArrayList<String> names) {

        String shared = names.get(0);

        for (int i = 1; i < names.size(); i++) {

            int length = 0;

            while (length < shared.length() && length < names.get(i).length()
                    && shared.charAt(length) == names.get(i).charAt(length))
                length++;

            shared = shared.substring(0, length);
        }

        return shared;
    }

    // Echoes the line to the log and checks it against its command, null when it cannot run
    private CommandStruct prepareCommand(String commandLine) {

        String trimmedLine = commandLine.trim();

        if (trimmedLine.isEmpty())
            return null;

        log(EditorSetting.COMMAND_ECHO_PREFIX + trimmedLine);

        CommandStruct command = parseCommand(trimmedLine);
        CommandHandle commandHandle = commandName2CommandHandle.get(command.getCommandName());

        if (commandHandle == null) {
            errorLog(EditorSetting.COMMAND_MESSAGE_UNKNOWN + command.getCommandName());
            return null;
        }

        if (command.getArgumentCount() != commandHandle.getArgumentCount()) {
            errorLog(EditorSetting.COMMAND_MESSAGE_USAGE + commandHandle.getUsage());
            return null;
        }

        return command;
    }

    private CommandStruct parseCommand(String commandLine) {

        String[] tokens = commandLine.split(EditorSetting.COMMAND_TOKEN_SEPARATOR_PATTERN);

        return new CommandStruct(commandLine, tokens[0], Arrays.copyOfRange(tokens, 1, tokens.length));
    }

    private int dispatchCommand(CommandStruct command) {

        ObjectArrayList<TabHandle> openTabs = tabManager.getOpenTabs();
        boolean shared = commandName2CommandHandle.get(command.getCommandName()).isShared();
        int receivers = 0;

        for (int i = 0; i < openTabs.size(); i++) {

            if (!(openTabs.get(i).getTabContext().getContentContext() instanceof DevContext devContext)
                    || devContext.isCrashed())
                continue;

            devContext.queueCommand(command);
            receivers++;

            if (shared)
                break;
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
