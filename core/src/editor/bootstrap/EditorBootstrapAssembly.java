package editor.bootstrap;

import editor.bootstrap.tabpipeline.TabPipeline;
import engine.editor.menueventsmanager.EditorMenuEventsManager;
import engine.root.AssemblyPackage;

public class EditorBootstrapAssembly extends AssemblyPackage {

    /*
     * Editor bootstrap root. Registers editor-global bootstrap managers that
     * must be available before runtime editor contexts and menu reflection run:
     * the tab pipeline, and the one set of editor menu branches every editor
     * window's chrome routes its callbacks to.
     */

    // Internal \\

    @Override
    protected void create() {
        create(TabPipeline.class);
        create(EditorMenuEventsManager.class);
    }
}
