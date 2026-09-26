package editor.bootstrap.commandpipeline.commandmanager;

import java.util.Arrays;

import editor.bootstrap.commandpipeline.command.CommandStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.dev.DevContext;
import engine.editor.EditorSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandManager extends ManagerPackage {

    /*
     * The one route from the editor console to every open Dev window. A
     * submitted line is echoed to the log, parsed once into a CommandStruct,
     * and queued on every Dev window that has not crashed, each of which runs
     * it inside its own frame. TabManager's open tabs are the only record of
     * which Dev windows exist, so nothing registers here.
     */

    // Internal
    private TabManager tabManager;

    // Base \\

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
    }

    // Management \\

    public void executeCommand(String commandLine) {

        String trimmedLine = commandLine.trim();

        if (trimmedLine.isEmpty())
            return;

        log(EditorSetting.COMMAND_ECHO_PREFIX + trimmedLine);

        if (dispatchCommand(parseCommand(trimmedLine)) == 0)
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
}
