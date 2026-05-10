package editor.runtime;

import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.dockpipeline.dockmanager.DockManager;
import editor.bootstrap.dockpipeline.dockinputsystem.DockInputSystem;
import editor.bootstrap.dockpipeline.dockrendersystem.DockRenderSystem;
import editor.runtime.menueventsmanager.EditorMenuEventsManager;
import engine.root.ContextPackage;

public class EditorWindowMain extends ContextPackage {

    /*
     * Editor runtime entry point. Creates and owns all editor systems.
     * Paired with a window by EditorEngine — primary or secondary.
     * Creates a dock container for its own window on start so any instance
     * of this context correctly owns the window it was paired with.
     * DockRenderSystem draws chrome. DockInputSystem handles interaction.
     */

    // Managers
    private DockManager dockManager;
    private WindowManager windowManager;

    // Internal \\

    @Override
    protected void create() {
        create(EditorMenuEventsManager.class);
        create(EditorMenuSystem.class);
        create(DockRenderSystem.class);
        create(DockInputSystem.class);
    }

    @Override
    protected void get() {

        // Managers
        this.dockManager = get(DockManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void start() {
        dockManager.createContainer(getWindow());
    }
}