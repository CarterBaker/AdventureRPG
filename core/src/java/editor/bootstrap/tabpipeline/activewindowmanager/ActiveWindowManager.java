package editor.bootstrap.tabpipeline.activewindowmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.runtime.EditorSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ActiveWindowManager extends ManagerPackage {

    /*
     * The one owner of the editor's active window: the tab content window
     * focused last, through its own view or its tab's chrome, taken the
     * moment WindowManager hands it the focus so the press that made it
     * active acts on it in the same frame. Focusing a window no tab holds,
     * the menu bar or the bare dock among them, keeps it. Every tab's content
     * window joins the recency list as it opens and leaves it as it closes,
     * through openWindow() and closeWindow(), the one pair that owns them;
     * activating a window moves it to the front. Whatever acts on the window
     * worked in last, the File and Edit menus, the Tools tab, the Properties
     * tab, the shortcuts and the hint bar, reads it here, filtered to the
     * windows it takes through findRecent(), so none keeps a copy. A move of
     * the active window moves the revision. Whatever acts on a window other
     * than its own names it the one way, through nameContext(): its text
     * followed by the window's tab title in parentheses, so the File and Edit
     * menus and every shared tab always say what they are working on.
     */

    // Internal
    private WindowManager windowManager;
    private TabManager tabManager;

    // Windows — most recently active first, then the never-active ones in the order they opened
    private ObjectArrayList<WindowInstance> recentWindows;

    // Active
    private WindowInstance activeWindow;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Windows
        this.recentWindows = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.tabManager = get(TabManager.class);
        windowManager.setFocusListener(this::activate);
    }

    // Windows \\

    public void openWindow(WindowInstance window) {
        if (!recentWindows.contains(window))
            recentWindows.add(window);
    }

    public void closeWindow(WindowInstance window) {

        recentWindows.remove(window);

        if (activeWindow != window)
            return;

        this.activeWindow = null;
        revision++;
    }

    // Active \\

    // The tab content a focused window belongs to made active; a window no tab holds leaves the active one be
    private void activate(WindowInstance focused) {

        WindowInstance window = tabManager.resolveContentWindow(focused);

        if (window == activeWindow || !recentWindows.contains(window))
            return;

        recentWindows.remove(window);
        recentWindows.add(0, window);
        this.activeWindow = window;
        revision++;
    }

    // Accessible \\

    // The window worked in last, null before any is
    public WindowInstance getActiveWindow() {
        return activeWindow;
    }

    public boolean isActive(WindowInstance window) {
        return window != null && window == activeWindow;
    }

    // The window worked in last among those the map holds, else the first of them opened; null for none
    public WindowInstance findRecent(Object2ObjectOpenHashMap<WindowInstance, ?> windows) {

        for (int i = 0; i < recentWindows.size(); i++)
            if (windows.containsKey(recentWindows.get(i)))
                return recentWindows.get(i);

        return null;
    }

    // The window worked in last whose content is of either kind, else the first of them opened; null for none
    public WindowInstance findRecent(Class<?> firstKind, Class<?> secondKind) {

        for (int i = 0; i < recentWindows.size(); i++) {

            WindowInstance window = recentWindows.get(i);

            if (firstKind.isInstance(window.getContext()) || secondKind.isInstance(window.getContext()))
                return window;
        }

        return null;
    }

    public int getRevision() {
        return revision;
    }

    // The title of the tab a window is the content of, null for a window no tab holds
    public String getWindowTitle(WindowInstance window) {

        TabHandle tab = window != null ? tabManager.getTabHandleForContentWindow(window) : null;

        return tab != null ? tab.getBaseTitle() : null;
    }

    // The text followed by the title of the window it acts on in parentheses; the text alone for no window, or when
    // the text already is the window's title
    public String nameContext(String text, WindowInstance window) {

        String title = getWindowTitle(window);

        if (title == null || title.equals(text))
            return text;

        return text + EditorSetting.CONTEXT_OPEN + title + EditorSetting.CONTEXT_CLOSE;
    }
}
