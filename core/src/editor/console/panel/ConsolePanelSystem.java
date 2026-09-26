package editor.console.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.console.ConsoleSetting;
import engine.root.SystemPackage;
import engine.util.log.LogLevel;
import engine.util.log.LogLineStruct;
import engine.util.log.LogUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ConsolePanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and mirrors the session log into
     * the console menu. Each frame it injects the lines written since the last
     * one, keeps the newest lines on screen up to the console's limit, and
     * follows the newest line until the list is scrolled up. The command line
     * shows whatever text ConsoleInputSystem hands it, and the menu itself is
     * shared with ConsoleCommandTreeSystem, which fills the command tree.
     */

    // Internal
    private MenuManager menuManager;
    private FboManager fboManager;

    // Menus
    private MenuInstance consoleMenu;
    private ElementInstance lineList;
    private ElementInstance commandLabel;

    // Lines
    private ObjectArrayList<LogLineStruct> pendingLines;
    private ObjectArrayList<ElementInstance> lineElements;
    private long nextLine;

    // Scroll
    private boolean following;
    private float followedScrollY;

    // Base \\

    @Override
    protected void create() {

        // Lines
        this.pendingLines = new ObjectArrayList<>();
        this.lineElements = new ObjectArrayList<>();

        // Scroll
        this.following = true;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.consoleMenu = menuManager.openMenu(ConsoleSetting.MENU_CONSOLE, window);
        this.lineList = consoleMenu.getEntryPoint(ConsoleSetting.ENTRY_LINES);
        this.commandLabel = consoleMenu.getEntryPoint(ConsoleSetting.ENTRY_COMMAND);

        if (lineList == null)
            throwException("Console menu '" + ConsoleSetting.MENU_CONSOLE + "' has no line list entry point.");

        if (commandLabel == null)
            throwException("Console menu '" + ConsoleSetting.MENU_CONSOLE + "' has no command line entry point.");
    }

    @Override
    protected void dispose() {

        menuManager.closeMenu(consoleMenu);
        menuManager.setMenuTargetFbo(context.getWindow(), null);
    }

    // Update \\

    @Override
    protected void update() {
        pullLines();
        followNewestLine();
    }

    private void pullLines() {

        this.nextLine = LogUtility.copyLines(nextLine, pendingLines);

        for (int i = Math.max(0, pendingLines.size() - ConsoleSetting.MAX_LINES); i < pendingLines.size(); i++)
            injectLine(pendingLines.get(i));

        while (lineElements.size() > ConsoleSetting.MAX_LINES)
            menuManager.eject(consoleMenu, ConsoleSetting.ENTRY_LINES, lineElements.remove(0));
    }

    private void injectLine(LogLineStruct line) {

        String template = line.getLogLevel() == LogLevel.ERROR
                ? ConsoleSetting.MENU_LINE_ERROR
                : ConsoleSetting.MENU_LINE;
        String text = LogUtility.formatLine(line)
                .replace(ConsoleSetting.TAB_CHARACTER, ConsoleSetting.TAB_REPLACEMENT);

        lineElements.add(menuManager.inject(
                consoleMenu,
                ConsoleSetting.ENTRY_LINES,
                template,
                element -> element.setFontText(text)));
    }

    private void followNewestLine() {

        float scrollY = lineList.getScrollY();

        if (scrollY < followedScrollY)
            following = false;

        if (scrollY >= lineList.getMaxScrollY())
            following = true;

        if (following)
            lineList.setScrollY(lineList.getMaxScrollY());

        this.followedScrollY = lineList.getScrollY();
    }

    // Management \\

    public void clear() {

        for (int i = 0; i < lineElements.size(); i++)
            menuManager.eject(consoleMenu, ConsoleSetting.ENTRY_LINES, lineElements.get(i));

        lineElements.clear();
        this.following = true;
    }

    public void setCommandText(String text) {
        commandLabel.setFontText(text);
    }

    // Accessible \\

    public MenuInstance getConsoleMenu() {
        return consoleMenu;
    }
}
