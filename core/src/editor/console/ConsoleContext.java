package editor.console;

import editor.console.panel.ConsolePanelSystem;
import engine.root.ContextPackage;

public class ConsoleContext extends ContextPackage {

    /*
     * Editor tab showing the session log as it is written. Everything the
     * engine logs lands here, including the failure of any tab that crashed
     * and was closed instead of taking the editor down.
     */

    // Internal
    private ConsolePanelSystem consolePanelSystem;

    // Internal \\

    @Override
    protected void create() {
        this.consolePanelSystem = create(ConsolePanelSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    public void clear() {
        consolePanelSystem.clear();
    }
}
