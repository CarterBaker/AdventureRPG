package editor.runtime.characterview.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterViewDockSystem extends SystemPackage {

    /*
     * Fills a docker window with the rows of the character tool its context
     * supplies: the dock menu's title from the source, and its one scrolling
     * panel refilled through CharacterViewPanelSystem whenever the source's
     * revision moves, never per frame. Every docker window of every
     * character tool creates one; its context is the row source.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private CharacterViewPanelSystem characterViewPanelSystem;

    // Source
    private CharacterViewRowSource rowSource;

    // Menus
    private MenuInstance dockMenu;

    // Rows
    private ObjectArrayList<CharacterViewRowStruct> rows;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.rows = new ObjectArrayList<>();

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.characterViewPanelSystem = get(CharacterViewPanelSystem.class);
    }

    @Override
    protected void awake() {

        if (!(context instanceof CharacterViewRowSource))
            throwException("A character docker window's context must supply its rows.");

        this.rowSource = (CharacterViewRowSource) context;

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.dockMenu = menuManager.openMenu(EditorSetting.CHARACTER_VIEW_MENU_DOCK, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (rowSource.getRowRevision() == shownRevision)
            return;

        this.shownRevision = rowSource.getRowRevision();

        ElementInstance title = dockMenu.getEntryPoint(EditorSetting.CHARACTER_VIEW_DOCK_ENTRY_TITLE);

        if (title != null)
            title.setFontText(rowSource.getRowTitle());

        rows.clear();
        rowSource.buildRows(rows);
        characterViewPanelSystem.fill(
                dockMenu,
                EditorSetting.CHARACTER_VIEW_DOCK_ENTRY_ROWS,
                rows,
                rowSource.getRowTemplates());
    }
}
