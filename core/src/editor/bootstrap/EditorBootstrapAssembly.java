package editor.bootstrap;

import editor.bootstrap.commandpipeline.CommandPipeline;
import editor.bootstrap.infopipeline.InfoPipeline;
import editor.bootstrap.itemeditorpipeline.ItemEditorPipeline;
import editor.bootstrap.tabpipeline.TabPipeline;
import editor.runtime.menueventsmanager.EditorMenuEventsManager;
import engine.root.AssemblyPackage;

public class EditorBootstrapAssembly extends AssemblyPackage {

    /*
     * Editor bootstrap root. Registers editor-global bootstrap managers that
     * must be available before runtime editor contexts and menu reflection run:
     * the tab pipeline, the info pipeline behind every hierarchy tab and info
     * panel, the item editor pipeline shared by every item editor, the command
     * pipeline that routes console commands to every Dev window, and the one
     * set of editor menu branches the toolbar, every tab's chrome, and every
     * tool route their callbacks to.
     */

    // Internal \\

    @Override
    protected void create() {
        create(TabPipeline.class);
        create(InfoPipeline.class);
        create(ItemEditorPipeline.class);
        create(CommandPipeline.class);
        create(EditorMenuEventsManager.class);
    }
}
