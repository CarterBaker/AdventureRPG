package editor.runtime.hintbar;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.dock.EditorDockSystem;
import engine.root.SystemPackage;

public class EditorHintBarSystem extends SystemPackage {

    /*
     * Owns the hint bar along the foot of the editor's main window: the keys
     * of the active window alone, on the canvas it works on and with the tool
     * chosen there, each read out from its shortcut file's hints by
     * ShortcutManager. The bar is read out again only when the active
     * window, its canvas, its tool or a shortcut's keys change, never while
     * they hold.
     */

    // Internal
    private ActiveWindowManager activeWindowManager;
    private ToolManager toolManager;
    private ShortcutManager shortcutManager;
    private EditorDockSystem editorDockSystem;

    // Bar
    private ElementInstance hintLabel;
    private StringBuilder hintBuilder;

    // Shown
    private WindowInstance shownWindow;
    private ToolCanvas shownCanvas;
    private EditorTool shownTool;
    private int shownActiveRevision;
    private int shownShortcutRevision;

    // Base \\

    @Override
    protected void create() {

        // Bar
        this.hintBuilder = new StringBuilder();
    }

    @Override
    protected void get() {
        this.activeWindowManager = get(ActiveWindowManager.class);
        this.toolManager = get(ToolManager.class);
        this.shortcutManager = get(ShortcutManager.class);
        this.editorDockSystem = get(EditorDockSystem.class);
    }

    @Override
    protected void start() {
        this.hintLabel = editorDockSystem.getBaseMenu().getEntryPoint(EditorSetting.HINT_BAR_ENTRY_LABEL);
        showHints();
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = activeWindowManager.getActiveWindow();
        ToolCanvas canvas = toolManager.getWindowCanvas(window);
        EditorTool tool = canvas != null ? toolManager.getTool(canvas) : null;

        if (window == shownWindow
                && canvas == shownCanvas
                && tool == shownTool
                && activeWindowManager.getRevision() == shownActiveRevision
                && shortcutManager.getRevision() == shownShortcutRevision)
            return;

        showHints();
    }

    // The active window's hints read out onto the bar, with what they were read for kept to compare against
    private void showHints() {

        this.shownWindow = activeWindowManager.getActiveWindow();
        this.shownCanvas = toolManager.getWindowCanvas(shownWindow);
        this.shownTool = shownCanvas != null ? toolManager.getTool(shownCanvas) : null;
        this.shownActiveRevision = activeWindowManager.getRevision();
        this.shownShortcutRevision = shortcutManager.getRevision();

        hintBuilder.setLength(0);
        shortcutManager.appendHints(hintBuilder, shownWindow, shownCanvas, shownTool);
        hintLabel.setFontText(hintBuilder.toString());
    }
}
