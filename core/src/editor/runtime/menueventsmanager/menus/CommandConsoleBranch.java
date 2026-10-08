package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.commandconsole.CommandConsoleContext;
import engine.root.BranchPackage;

public class CommandConsoleBranch extends BranchPackage {

    /*
     * Menu event handlers for the command console tab. A command picked from
     * the command tree runs exactly as if it had been typed; a group or
     * category toggles, an item or vehicle tile is picked up, and a chunk
     * field is focused or its command run, in the command console paired with
     * the window it was pressed in.
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

    public void toggleTreeNode(String nodeKey, WindowInstance window) {
        if (window.getContext() instanceof CommandConsoleContext commandConsoleContext)
            commandConsoleContext.toggleTreeNode(nodeKey);
    }

    public void dragTile(WindowInstance window, ElementInstance element) {
        if (window.getContext() instanceof CommandConsoleContext commandConsoleContext)
            commandConsoleContext.dragTile(element, window);
    }

    public void focusChunkField(String fieldKey, WindowInstance window) {
        if (window.getContext() instanceof CommandConsoleContext commandConsoleContext)
            commandConsoleContext.focusChunkField(fieldKey);
    }

    public void runChunkCommand(String commandName, WindowInstance window) {
        if (window.getContext() instanceof CommandConsoleContext commandConsoleContext)
            commandConsoleContext.runChunkCommand(commandName);
    }
}
