package editor.runtime.animationtreeeditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.runtime.animationtreeeditor.AnimationTreeEditorSetting;
import editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager.AnimationTreeEditorManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class AnimationTreeEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Animation Tree Editor toolbar and its status bar over the
     * viewport, and rewrites the status line when the editor's revision
     * moves. The tree's layers and nodes are picked in the Palette tab and
     * their values set in the Properties tab, never in panels of its own.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private AnimationTreeEditorManager animationTreeEditorManager;

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
        this.animationTreeEditorManager = get(AnimationTreeEditorManager.class);
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
    }
}
