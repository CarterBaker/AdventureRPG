package editor.bootstrap.propertiespipeline.propertiesmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class PropertiesManager extends ManagerPackage {

    /*
     * Resolves which window the Properties tab shows: the window
     * ActiveWindowManager names worked in last among those that provide
     * properties, so activating one that provides none, the Properties tab
     * itself among them, keeps the target and a value can be set there for
     * the window it belongs to.
     * Every window that provides properties opens itself here for as long as
     * it lives, through openWindow() and closeWindow(), the one pair that
     * owns them; a closed target leaves the tab with none until another is
     * focused.
     */

    // Internal
    private ActiveWindowManager activeWindowManager;

    // Windows
    private Object2ObjectOpenHashMap<WindowInstance, PropertiesProvider> window2Provider;

    // Base \\

    @Override
    protected void create() {

        // Windows
        this.window2Provider = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.activeWindowManager = get(ActiveWindowManager.class);
    }

    // Windows \\

    public void openWindow(WindowInstance window, PropertiesProvider provider) {
        window2Provider.put(window, provider);
    }

    public void closeWindow(WindowInstance window) {
        window2Provider.remove(window);
    }

    // Accessible \\

    // What the window worked in last shows in the Properties tab, null before any is worked in
    public PropertiesProvider getProvider() {

        WindowInstance targetWindow = activeWindowManager.findRecent(window2Provider);

        return targetWindow != null ? window2Provider.get(targetWindow) : null;
    }
}
