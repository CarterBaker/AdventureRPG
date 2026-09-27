package editor.commandconsole;

import editor.commandconsole.commandtree.CommandConsoleTreeSystem;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.commandconsole.input.CommandConsoleInputSystem;
import editor.commandconsole.itemgrid.CommandConsoleItemGridSystem;
import editor.commandconsole.itemgrid.CommandConsoleItemRenderSystem;
import editor.commandconsole.panel.CommandConsolePanelSystem;
import editor.runtime.EditorInputSystem;
import engine.root.ContextPackage;

public class CommandConsoleContext extends ContextPackage {

    /*
     * Editor tab for sending commands to every open Dev window. Its command
     * line sends whatever is typed into it, and its command tree sends any
     * command that needs no arguments with a single click and any command
     * that takes an item from a grid of item tiles — clicked for every Dev
     * window, or dragged onto one Dev window to run there alone. What each
     * command reports lands in the log, shown by the Console tab.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CommandConsolePanelSystem commandConsolePanelSystem;
    private CommandConsoleInputSystem commandConsoleInputSystem;
    private CommandConsoleTreeSystem commandConsoleTreeSystem;
    private CommandConsoleItemGridSystem commandConsoleItemGridSystem;
    private CommandConsoleItemRenderSystem commandConsoleItemRenderSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.commandConsolePanelSystem = create(CommandConsolePanelSystem.class);
        this.commandConsoleInputSystem = create(CommandConsoleInputSystem.class);
        this.commandConsoleTreeSystem = create(CommandConsoleTreeSystem.class);
        this.commandConsoleItemGridSystem = create(CommandConsoleItemGridSystem.class);
        this.commandConsoleItemRenderSystem = create(CommandConsoleItemRenderSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    public void toggleCommandGroup(String groupName) {
        commandConsoleTreeSystem.toggleCommandGroup(groupName);
    }

    public void dragItemTile(ElementInstance tileElement, WindowInstance window) {
        commandConsoleItemGridSystem.dragTile(tileElement, window);
    }
}
