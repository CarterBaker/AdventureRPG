package editor.bootstrap.filemenupipeline.filemenumanager;

import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.filemenupipeline.util.FileMenuAction;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.runtime.EditorSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class FileMenuManager extends ManagerPackage {

    /*
     * Resolves which window the editor's File and Edit menus act on: the tool
     * ActiveWindowManager names worked in last among those that take file
     * actions, so activating one that takes none, the Properties tab among
     * them, keeps the target and a menu item always reaches the tool worked
     * in. Each action runs the
     * method the tool's table names on the tool's own window, through the
     * route its shortcuts take, inside its crash boundary. Every tool opens
     * itself here for as long as it lives, through openWindow() and
     * closeWindow(), the one pair that owns them; its table's methods are
     * resolved as it opens, so a bad one fails there.
     */

    // Internal
    private ActiveWindowManager activeWindowManager;
    private ElementHitSystem elementHitSystem;

    // Windows
    private Object2ObjectOpenHashMap<WindowInstance, FileMenuProvider> window2Provider;

    // Base \\

    @Override
    protected void create() {

        // Windows
        this.window2Provider = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.activeWindowManager = get(ActiveWindowManager.class);
        this.elementHitSystem = get(ElementHitSystem.class);
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
    }

    // Actions \\

    public void run(FileMenuAction action) {

        if (!takes(action))
            return;

        WindowInstance targetWindow = getTargetWindow();
        FileMenuTableStruct table = window2Provider.get(targetWindow).getFileMenuTable();

        elementHitSystem.executeAction(
                table.getActionClass(),
                table.getMethod(action),
                table.getArg(action),
                targetWindow);
    }

    // The tool worked in last among those that take file actions, null for none; windows that take none, the shared
    // tabs among them, never become it, so the menus stay on the last tool that makes, saves and undoes
    public WindowInstance getTargetWindow() {
        return activeWindowManager.findRecent(window2Provider);
    }

    // Accessible \\

    public boolean takes(FileMenuAction action) {

        WindowInstance targetWindow = getTargetWindow();

        return targetWindow != null && window2Provider.get(targetWindow).getFileMenuTable().takes(action);
    }

    // The name of the tab the menus act on, shown at their head
    public String getTargetTitle() {

        String title = activeWindowManager.getWindowTitle(getTargetWindow());

        return title != null ? title : EditorSetting.FILE_MENU_NO_TARGET;
    }

    // An item's name with the tool it acts on after it in parentheses, the name alone when that tool does not take it
    public String nameItem(FileMenuAction action) {

        String name = EditorSetting.FILE_MENU_NAMES[action.ordinal()];

        return takes(action) ? activeWindowManager.nameContext(name, getTargetWindow()) : name;
    }
}
