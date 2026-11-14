package editor.runtime.animationeditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.runtime.animationeditor.AnimationEditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class AnimationEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Animation Editor toolbar and its status bar over the
     * viewport, and rewrites the status line when the editor's revision
     * moves. The editor's panels and timeline are the Animation Outliner and
     * Animation Timeline docker windows, and its values the Properties tab.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private AnimationEditorManager animationEditorManager;

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
        this.animationEditorManager = get(AnimationEditorManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(AnimationEditorSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    @Override
    protected void update() {

        if (animationEditorManager.getRevision() == shownRevision)
            return;

        shownRevision = animationEditorManager.getRevision();

        ElementInstance statusLabel = toolbarMenu.getEntryPoint(AnimationEditorSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(animationEditorManager.getStatusText());
    }
}
