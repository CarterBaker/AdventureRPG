package editor.bootstrap;

import editor.bootstrap.tabmanager.TabManager;
import engine.root.AssemblyPackage;

public class BootstrapAssembly extends AssemblyPackage {

    /*
     * Editor bootstrap root. Registers editor-global bootstrap managers that
     * must be available before runtime editor contexts and menu reflection run.
     */

    // Internal \\
    @Override
    protected void create() {
        create(TabManager.class);
    }
}
