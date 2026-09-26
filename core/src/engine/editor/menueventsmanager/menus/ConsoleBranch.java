package engine.editor.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.console.ConsoleContext;
import engine.root.BranchPackage;

public class ConsoleBranch extends BranchPackage {

    /*
     * Menu event handlers for the console tab. Each action targets the
     * console paired with the window its button was clicked in; a command
     * picked from the command tree runs exactly as if it had been typed.
     */

    // Internal
    private CommandManager commandManager;

    // Base \\

    @Override
    protected void get() {
        this.commandManager = get(CommandManager.class);
    }

    // Console Operations \\

    public void clear(WindowInstance window) {
        if (window.getContext() instanceof ConsoleContext consoleContext)
            consoleContext.clear();
    }

    // Command Operations \\

    public void runCommand(String commandName) {
        commandManager.executeCommand(commandName);
    }

    public void toggleCommandGroup(String groupName, WindowInstance window) {
        if (window.getContext() instanceof ConsoleContext consoleContext)
            consoleContext.toggleCommandGroup(groupName);
    }
}
