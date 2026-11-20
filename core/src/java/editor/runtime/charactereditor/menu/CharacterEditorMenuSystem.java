package editor.runtime.charactereditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.runtime.charactereditor.CharacterEditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class CharacterEditorMenuSystem extends SystemPackage {

    /*
     * Opens a character tool tab's status bar over the viewport and writes
     * the character editor's status into it, rewritten only when the
     * editor's state moves. The editor's panel is the Properties tab.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private CharacterEditorManager characterEditorManager;

    // Menus
    private MenuInstance toolbarMenu;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
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

        this.shownRevision = characterEditorManager.getRevision();

        ElementInstance statusLabel = toolbarMenu.getEntryPoint(CharacterEditorSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(characterEditorManager.getStatusText());
    }
}
