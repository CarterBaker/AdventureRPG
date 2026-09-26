package editor.commandconsole;

import editor.commandconsole.commandtree.CommandConsoleTreeSystem;
import editor.commandconsole.input.CommandConsoleInputSystem;
import editor.commandconsole.panel.CommandConsolePanelSystem;
import editor.runtime.EditorInputSystem;
import engine.root.ContextPackage;

public class CommandConsoleContext extends ContextPackage {

    /*
     * Editor tab for sending commands to every open Dev window. Its command
     * line sends whatever is typed into it, and its command tree sends any
     * command that needs no arguments with a single click. What each command
     * reports lands in the log, shown by the Console tab.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CommandConsolePanelSystem commandConsolePanelSystem;
    private CommandConsoleInputSystem commandConsoleInputSystem;
    private CommandConsoleTreeSystem commandConsoleTreeSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.commandConsolePanelSystem = create(CommandConsolePanelSystem.class);
        this.commandConsoleInputSystem = create(CommandConsoleInputSystem.class);
        this.commandConsoleTreeSystem = create(CommandConsoleTreeSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    public void toggleCommandGroup(String groupName) {
        commandConsoleTreeSystem.toggleCommandGroup(groupName);
    }
}
