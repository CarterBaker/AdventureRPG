package editor.runtime.animationtreeeditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.runtime.animationtreeeditor.AnimationTreeEditorSetting;
import editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager.AnimationTreeEditorManager;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AnimationTreeEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Animation Tree Editor toolbar, its two panels and its status
     * bar over the viewport. When the editor's revision moves it rewrites the
     * status line and refills both panels from the rows the editor lists.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private AnimationTreeEditorManager animationTreeEditorManager;
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
                AnimationTreeEditorSetting.TEMPLATE_HEADER_ROW,
                AnimationTreeEditorSetting.TEMPLATE_ITEM_ROW,
                AnimationTreeEditorSetting.TEMPLATE_ITEM_ROW_SELECTED,
                AnimationTreeEditorSetting.TEMPLATE_NUMBER_ROW,
                AnimationTreeEditorSetting.TEMPLATE_CHOICE_ROW,
                AnimationTreeEditorSetting.TEMPLATE_ACTION_ROW,
                AnimationTreeEditorSetting.ELEMENT_ROW_LABEL,
                AnimationTreeEditorSetting.ELEMENT_ROW_VALUE,
                AnimationTreeEditorSetting.ELEMENT_ROW_VALUE_BUTTON,
                AnimationTreeEditorSetting.ELEMENT_ROW_DECREASE,
                AnimationTreeEditorSetting.ELEMENT_ROW_INCREASE);

        // Rows
        this.rows = new ObjectArrayList<>();

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.animationTreeEditorManager = get(AnimationTreeEditorManager.class);
        this.characterViewPanelSystem = get(CharacterViewPanelSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(AnimationTreeEditorSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (animationTreeEditorManager.getRevision() == shownRevision)
            return;

        shownRevision = animationTreeEditorManager.getRevision();

        ElementInstance statusLabel = toolbarMenu.getEntryPoint(AnimationTreeEditorSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(animationTreeEditorManager.getStatusText());

        rows.clear();
        animationTreeEditorManager.buildListRows(rows);
        characterViewPanelSystem.fill(toolbarMenu, AnimationTreeEditorSetting.ENTRY_LIST, rows, templates);

        rows.clear();
        animationTreeEditorManager.buildPropertyRows(rows);
        characterViewPanelSystem.fill(toolbarMenu, AnimationTreeEditorSetting.ENTRY_PROPERTIES, rows, templates);
    }
}
