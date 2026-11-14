package editor.runtime.commandconsole;

import editor.runtime.commandconsole.chunkfield.CommandConsoleChunkFieldSystem;
import editor.runtime.commandconsole.commandtree.CommandConsoleTreeSystem;
import editor.runtime.commandconsole.dial.CommandConsoleDialSystem;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.commandconsole.input.CommandConsoleInputSystem;
import editor.runtime.commandconsole.panel.CommandConsolePanelSystem;
import editor.runtime.commandconsole.tilegrid.CommandConsoleTileGridSystem;
import editor.runtime.commandconsole.tilegrid.CommandConsoleTileRenderSystem;
import editor.runtime.input.EditorInputSystem;
import engine.root.ContextPackage;

public class CommandConsoleContext extends ContextPackage {

    /*
     * Editor tab for sending commands to every open Dev window. Its command
     * line sends whatever is typed into it, and its command tree sends any
     * command that needs no arguments with a single click and any command
     * that takes an item or a vehicle from tiles grouped by category — clicked
     * for every Dev window, or dragged onto one Dev window to run there alone —
     * any command that takes a chunk from X and Y fields typed into, and the
     * time command from a round dial per unit of the clock, turned by
     * dragging around it or stepped by its minus and plus buttons. The
     * command line recalls what was sent with Up and Down and completes a
     * command's name with Tab. What each command reports lands in the log,
     * shown by the Console tab.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CommandConsolePanelSystem commandConsolePanelSystem;
    private CommandConsoleInputSystem commandConsoleInputSystem;
    private CommandConsoleTreeSystem commandConsoleTreeSystem;
    private CommandConsoleTileGridSystem commandConsoleTileGridSystem;
    private CommandConsoleTileRenderSystem commandConsoleTileRenderSystem;
    private CommandConsoleChunkFieldSystem commandConsoleChunkFieldSystem;
    private CommandConsoleDialSystem commandConsoleDialSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.commandConsolePanelSystem = create(CommandConsolePanelSystem.class);
        this.commandConsoleInputSystem = create(CommandConsoleInputSystem.class);
        this.commandConsoleTreeSystem = create(CommandConsoleTreeSystem.class);
        this.commandConsoleTileGridSystem = create(CommandConsoleTileGridSystem.class);
        this.commandConsoleTileRenderSystem = create(CommandConsoleTileRenderSystem.class);
        this.commandConsoleChunkFieldSystem = create(CommandConsoleChunkFieldSystem.class);
        this.commandConsoleDialSystem = create(CommandConsoleDialSystem.class);
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

    public void focusChunkField(String fieldKey) {
        commandConsoleChunkFieldSystem.focusField(fieldKey);
        commandConsoleInputSystem.restartCaret();
    }

    public void runChunkCommand(String commandName) {
        commandConsoleChunkFieldSystem.runCommand(commandName);
        commandConsoleInputSystem.restartCaret();
    }

    public void turnDial(ElementInstance faceElement) {
        commandConsoleDialSystem.turnDial(faceElement);
    }

    public void decreaseDial(String unitName) {
        commandConsoleDialSystem.stepDial(unitName, -CommandConsoleSetting.DIAL_STEP);
    }

    public void increaseDial(String unitName) {
        commandConsoleDialSystem.stepDial(unitName, CommandConsoleSetting.DIAL_STEP);
    }
}
