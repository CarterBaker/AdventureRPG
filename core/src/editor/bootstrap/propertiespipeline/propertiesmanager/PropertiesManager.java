package editor.bootstrap.propertiespipeline.propertiesmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class PropertiesManager extends ManagerPackage {

    /*
     * Keeps which window the Properties tab shows: the window that provides
     * properties focused last, its window or its tab's chrome. Focusing a
     * window that provides none, the Properties tab itself among them, keeps
     * the target, so a value can be set there for the window it belongs to.
     * Every window that provides properties opens itself here for as long as
     * it lives, through openWindow() and closeWindow(), the one pair that
     * owns them; a closed target leaves the tab with none until another is
     * focused.
     */

    // Internal
    private WindowManager windowManager;
    private TabManager tabManager;

    // Windows
    private Object2ObjectOpenHashMap<WindowInstance, PropertiesProvider> window2Provider;

    // Target
    private WindowInstance targetWindow;

    // Base \\

    @Override
    protected void create() {

        // Windows
        this.window2Provider = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.tabManager = get(TabManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance focused = windowManager.getFocusedWindow();
        WindowInstance window = focused != null ? tabManager.resolveContentWindow(focused) : null;

        if (window != null && window2Provider.containsKey(window))
            this.targetWindow = window;
    }

    // Windows \\

    public void openWindow(WindowInstance window, PropertiesProvider provider) {
        window2Provider.put(window, provider);
    }

    public void closeWindow(WindowInstance window) {

        window2Provider.remove(window);

        if (targetWindow == window)
            this.targetWindow = null;
    }

    // Accessible \\

    // What the window worked in last shows in the Properties tab, null before any is worked in
    public PropertiesProvider getProvider() {
        return targetWindow != null ? window2Provider.get(targetWindow) : null;
    }
}
