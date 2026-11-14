package editor.runtime.charactereditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.CharacterEditorWindowProvider;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class CharacterEditorMenuSystem extends SystemPackage {

    /*
     * Opens the toolbar its tab names, the one the Character Appearance,
     * Build and Equipment tabs share or the Character Mesh Editor's, and its
     * status bar over the viewport, and rewrites the status line when the
     * editor's revision moves. The editor's panels are the Character
     * Outliner docker window and the Properties tab.
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
        this.toolbarMenu = menuManager.openMenu(((CharacterEditorWindowProvider) context).getToolbarMenu(), window);
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
    }
}
