package editor.bootstrap.propertiespipeline.propertiesmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.propertiespipeline.properties.MapsProvider;
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
     * focused. The Maps tab follows the same way the window worked in last
     * among those whose image carries maps.
     */

    // Internal
    private ActiveWindowManager activeWindowManager;

    // Windows
    private Object2ObjectOpenHashMap<WindowInstance, PropertiesProvider> window2Provider;
    private Object2ObjectOpenHashMap<WindowInstance, MapsProvider> window2Maps;

    // Base \\

    @Override
    protected void create() {

        // Windows
        this.window2Provider = new Object2ObjectOpenHashMap<>();
        this.window2Maps = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.activeWindowManager = get(ActiveWindowManager.class);
    }

    // Windows \\

    public void openWindow(WindowInstance window, PropertiesProvider provider) {

        window2Provider.put(window, provider);

        if (provider instanceof MapsProvider maps)
            window2Maps.put(window, maps);
    }

    public void closeWindow(WindowInstance window) {
        window2Provider.remove(window);
        window2Maps.remove(window);
    }

    // Accessible \\

    // What the window worked in last shows in the Properties tab, null before any is worked in
    public PropertiesProvider getProvider() {

        WindowInstance targetWindow = getProviderWindow();

        return targetWindow != null ? window2Provider.get(targetWindow) : null;
    }

    // The window the Properties tab shows, null before any is worked in
    public WindowInstance getProviderWindow() {
        return activeWindowManager.findRecent(window2Provider);
    }

    // What the window worked in last whose image carries maps shows in the Maps tab, null before any is
    public MapsProvider getMaps() {
        WindowInstance targetWindow = getMapsWindow();
        return targetWindow != null ? window2Maps.get(targetWindow) : null;
    }

    // The window the Maps tab shows, null before any is worked in
    public WindowInstance getMapsWindow() {
        return activeWindowManager.findRecent(window2Maps);
    }
}
