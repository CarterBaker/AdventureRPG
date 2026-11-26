package editor.bootstrap.propertiespipeline.propertiesmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.propertiespipeline.properties.PaletteProvider;
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
     * focused. The Palette tab follows the same way the window worked in last
     * among those that have a palette.
     */

    // Internal
    private ActiveWindowManager activeWindowManager;

    // Windows
    private Object2ObjectOpenHashMap<WindowInstance, PropertiesProvider> window2Provider;
    private Object2ObjectOpenHashMap<WindowInstance, PaletteProvider> window2Palette;

    // Base \\

    @Override
    protected void create() {

        // Windows
        this.window2Provider = new Object2ObjectOpenHashMap<>();
        this.window2Palette = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.activeWindowManager = get(ActiveWindowManager.class);
    }

    // Windows \\

    public void openWindow(WindowInstance window, PropertiesProvider provider) {

        window2Provider.put(window, provider);

        if (provider instanceof PaletteProvider palette)
            window2Palette.put(window, palette);
    }

    public void closeWindow(WindowInstance window) {
        window2Provider.remove(window);
        window2Palette.remove(window);
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

    // What the window worked in last that has a palette offers in the Palette tab, null before any is
    public PaletteProvider getPalette() {
        WindowInstance targetWindow = getPaletteWindow();
        return targetWindow != null ? window2Palette.get(targetWindow) : null;
    }

    // The window the Palette tab shows, null before any is worked in
    public WindowInstance getPaletteWindow() {
        return activeWindowManager.findRecent(window2Palette);
    }
}
