package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.commandconsole.CommandConsoleContext;
import engine.root.BranchPackage;

public class CommandConsoleBranch extends BranchPackage {

    /*
     * Menu event handlers for the command console tab. A command picked from
     * the command tree runs exactly as if it had been typed; a group toggles,
     * and an item tile is picked up, in the command console paired with the
     * window it was pressed in.
     */

    // Internal
    private CommandManager commandManager;

    // Base \\

    @Override
    protected void get() {
        this.commandManager = get(CommandManager.class);
    }

    // Command Operations \\

    public void runCommand(String commandName) {
        commandManager.executeCommand(commandName);
    }

    public void toggleCommandGroup(String groupName, WindowInstance window) {
        if (window.getContext() instanceof CommandConsoleContext commandConsoleContext)
            commandConsoleContext.toggleCommandGroup(groupName);
    }

    public void dragItemTile(WindowInstance window, ElementInstance element) {
        if (window.getContext() instanceof CommandConsoleContext commandConsoleContext)
            commandConsoleContext.dragItemTile(element, window);
    }
}
