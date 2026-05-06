package editor.runtime;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.dockpipeline.dockmanager.DockManager;
import editor.bootstrap.dockpipeline.container.ContainerInstance;
import editor.bootstrap.dockpipeline.dockinputsystem.DockInputSystem;
import editor.bootstrap.dockpipeline.dockrendersystem.DockRenderSystem;
import editor.runtime.menueventsmanager.EditorMenuEventsManager;
import engine.root.ContextPackage;
import engine.root.EngineSetting;

public class EditorWindowMain extends ContextPackage {

    /*
     * Editor runtime entry point. Creates and owns all editor systems.
     * Paired with the main window by EditorEngine.
     * Creates dock container for main window on start.
     * DockRenderSystem draws chrome. DockInputSystem handles interaction.
     */

    // Managers
    private DockManager dockManager;
    private WindowManager windowManager;

    @Override
    protected void create() {
        create(EditorMenuEventsManager.class);
        create(EditorMenuSystem.class);
        create(DockRenderSystem.class);
        create(DockInputSystem.class);
    }

    @Override
    protected void get() {
        this.dockManager = get(DockManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void start() {
        WindowInstance mainWindow = windowManager.getMainWindow();
        dockManager.createContainer(mainWindow);
    }
}