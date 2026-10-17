package editor.charactereditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.charactereditor.CharacterEditorSetting;
import editor.characterview.panel.CharacterViewPanelSystem;
import editor.characterview.panel.CharacterViewRowStruct;
import editor.characterview.panel.CharacterViewTemplateStruct;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Character Editor toolbar, its two panels and its status bar
     * over the viewport. When the editor's revision moves it rewrites the
     * status line and refills both panels from the rows the editor lists.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private CharacterEditorManager characterEditorManager;
    private CharacterViewPanelSystem characterViewPanelSystem;

    // Menus
    private MenuInstance toolbarMenu;
    private CharacterViewTemplateStruct templates;

    // Rows
    private ObjectArrayList<CharacterViewRowStruct> rows;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Menus
        this.templates = new CharacterViewTemplateStruct(
                CharacterEditorSetting.TEMPLATE_HEADER_ROW,
                CharacterEditorSetting.TEMPLATE_ITEM_ROW,
                CharacterEditorSetting.TEMPLATE_ITEM_ROW_SELECTED,
                CharacterEditorSetting.TEMPLATE_NUMBER_ROW,
                CharacterEditorSetting.TEMPLATE_CHOICE_ROW,
                CharacterEditorSetting.TEMPLATE_ACTION_ROW,
                CharacterEditorSetting.ELEMENT_ROW_LABEL,
                CharacterEditorSetting.ELEMENT_ROW_VALUE,
                CharacterEditorSetting.ELEMENT_ROW_VALUE_BUTTON,
                CharacterEditorSetting.ELEMENT_ROW_DECREASE,
                CharacterEditorSetting.ELEMENT_ROW_INCREASE);

        // Rows
        this.rows = new ObjectArrayList<>();

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterViewPanelSystem = get(CharacterViewPanelSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(CharacterEditorSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (characterEditorManager.getRevision() == shownRevision)
            return;

        shownRevision = characterEditorManager.getRevision();

        ElementInstance statusLabel = toolbarMenu.getEntryPoint(CharacterEditorSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(characterEditorManager.getStatusText());

        rows.clear();
        characterEditorManager.buildListRows(rows);
        characterViewPanelSystem.fill(toolbarMenu, CharacterEditorSetting.ENTRY_LIST, rows, templates);

        rows.clear();
        characterEditorManager.buildPropertyRows(rows);
        characterViewPanelSystem.fill(toolbarMenu, CharacterEditorSetting.ENTRY_PROPERTIES, rows, templates);
    }
}
