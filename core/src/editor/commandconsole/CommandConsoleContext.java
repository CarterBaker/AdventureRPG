package editor.commandconsole;

import editor.commandconsole.commandtree.CommandConsoleTreeSystem;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.commandconsole.input.CommandConsoleInputSystem;
import editor.commandconsole.panel.CommandConsolePanelSystem;
import editor.commandconsole.tilegrid.CommandConsoleTileGridSystem;
import editor.commandconsole.tilegrid.CommandConsoleTileRenderSystem;
import editor.runtime.EditorInputSystem;
import engine.root.ContextPackage;

public class CommandConsoleContext extends ContextPackage {

    /*
     * Editor tab for sending commands to every open Dev window. Its command
     * line sends whatever is typed into it, and its command tree sends any
     * command that needs no arguments with a single click and any command
     * that takes an item or a vehicle from tiles grouped by category — clicked
     * for every Dev window, or dragged onto one Dev window to run there alone.
     * What each command reports lands in the log, shown by the Console tab.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CommandConsolePanelSystem commandConsolePanelSystem;
    private CommandConsoleInputSystem commandConsoleInputSystem;
    private CommandConsoleTreeSystem commandConsoleTreeSystem;
    private CommandConsoleTileGridSystem commandConsoleTileGridSystem;
    private CommandConsoleTileRenderSystem commandConsoleTileRenderSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.commandConsolePanelSystem = create(CommandConsolePanelSystem.class);
        this.commandConsoleInputSystem = create(CommandConsoleInputSystem.class);
        this.commandConsoleTreeSystem = create(CommandConsoleTreeSystem.class);
        this.commandConsoleTileGridSystem = create(CommandConsoleTileGridSystem.class);
        this.commandConsoleTileRenderSystem = create(CommandConsoleTileRenderSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    public void toggleTreeNode(String nodeKey) {
        commandConsoleTreeSystem.toggleTreeNode(nodeKey);
    }

    public void dragTile(ElementInstance tileElement, WindowInstance window) {
        commandConsoleTileGridSystem.dragTile(tileElement, window);
    }
}
