package editor.bootstrap.filemenupipeline.filemenumanager;

import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.filemenupipeline.util.FileMenuAction;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.runtime.EditorSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class FileMenuManager extends ManagerPackage {

    /*
     * Keeps which window the editor's File and Edit menus act on: the tool
     * focused last, its window or its tab's chrome. Focusing a window that
     * takes no file actions, the menu bar among them, keeps the target, so a
     * menu item always reaches the tool worked in. Each action runs the
     * method the tool's table names on the tool's own window, through the
     * route its shortcuts take, inside its crash boundary. Every tool opens
     * itself here for as long as it lives, through openWindow() and
     * closeWindow(), the one pair that owns them; its table's methods are
     * resolved as it opens, so a bad one fails there.
     */

    // Internal
    private WindowManager windowManager;
    private TabManager tabManager;
    private ElementHitSystem elementHitSystem;

    // Windows
    private Object2ObjectOpenHashMap<WindowInstance, FileMenuProvider> window2Provider;

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
        this.elementHitSystem = get(ElementHitSystem.class);
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

    public void openWindow(WindowInstance window, FileMenuProvider provider) {

        FileMenuTableStruct table = provider.getFileMenuTable();

        for (FileMenuAction action : FileMenuAction.VALUES)
            if (table.takes(action))
                elementHitSystem.verifyAction(table.getActionClass(), table.getMethod(action));

        window2Provider.put(window, provider);
    }

    public void closeWindow(WindowInstance window) {

        window2Provider.remove(window);

        if (targetWindow == window)
            this.targetWindow = null;
    }

    // Actions \\

    public void run(FileMenuAction action) {

        if (!takes(action))
            return;

        FileMenuTableStruct table = window2Provider.get(targetWindow).getFileMenuTable();

        elementHitSystem.executeAction(
                table.getActionClass(),
                table.getMethod(action),
                table.getArg(action),
                targetWindow);
    }

    // Accessible \\

    public boolean takes(FileMenuAction action) {
        return targetWindow != null && window2Provider.get(targetWindow).getFileMenuTable().takes(action);
    }

    // The name of the tab the menus act on, shown at their head
    public String getTargetTitle() {

        TabHandle tab = targetWindow != null ? tabManager.getTabHandleForContentWindow(targetWindow) : null;

        return tab != null ? tab.getTabTitle() : EditorSetting.FILE_MENU_NO_TARGET;
    }
}
