package editor.bootstrap;

import editor.bootstrap.itemeditorpipeline.ItemEditorPipeline;
import editor.bootstrap.tabpipeline.TabPipeline;
import engine.editor.menueventsmanager.EditorMenuEventsManager;
import engine.root.AssemblyPackage;

public class EditorBootstrapAssembly extends AssemblyPackage {

    /*
     * Editor bootstrap root. Registers editor-global bootstrap managers that
     * must be available before runtime editor contexts and menu reflection run:
     * the tab pipeline, the item editor pipeline shared by every item editor
     * and hierarchy tab, and the one set of editor menu branches the toolbar,
     * every tab's chrome, and every tool route their callbacks to.
     */

    // Internal \\

    @Override
    protected void create() {
        create(TabPipeline.class);
        create(ItemEditorPipeline.class);
        create(EditorMenuEventsManager.class);
    }
}
