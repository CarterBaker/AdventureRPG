package editor.console;

import editor.console.commandtree.ConsoleCommandTreeSystem;
import editor.console.input.ConsoleInputSystem;
import editor.console.panel.ConsolePanelSystem;
import engine.editor.EditorInputSystem;
import engine.root.ContextPackage;

public class ConsoleContext extends ContextPackage {

    /*
     * Editor tab showing the session log as it is written. Everything the
     * engine logs lands here, including the failure of any tab that crashed
     * and was closed instead of taking the editor down. Its command line sends
     * whatever is typed into it to every open Dev window, and its command tree
     * sends any command that needs no arguments with a single click.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ConsolePanelSystem consolePanelSystem;
    private ConsoleInputSystem consoleInputSystem;
    private ConsoleCommandTreeSystem consoleCommandTreeSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.consolePanelSystem = create(ConsolePanelSystem.class);
        this.consoleInputSystem = create(ConsoleInputSystem.class);
        this.consoleCommandTreeSystem = create(ConsoleCommandTreeSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    public void clear() {
        consolePanelSystem.clear();
    }

    public void toggleCommandGroup(String groupName) {
        consoleCommandTreeSystem.toggleCommandGroup(groupName);
    }
}
