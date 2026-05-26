package editor.runtime.editor;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class SecondaryMenuSystem extends SystemPackage {

    /*
     * Opens the Base menu on a secondary OS window — background sprite and dock
     * canvas area. No toolbar window. Mirrors EditorMenuSystem exactly minus
     * the toolbar logical window and toolbar FBO.
     *
     * The OS window dimensions are the source of truth here — there is no
     * compositeRect on an OS window. The base menu fills the full window so no
     * rect mirroring is needed in update(); the menu layout system handles it.
     *
     * SecondaryTabCompositorSystem reads getBaseMenu() for dock canvas bounds
     * exactly as EditorTabCompositorSystem reads EditorMenuSystem.getBaseMenu().
     */

    private MenuManager menuManager;
    private FboManager fboManager;

    private MenuInstance baseMenu;

    // Internal \\

    @Override
    protected void get() {
        menuManager = get(MenuManager.class);
        fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance osWindow = context.getWindow();

        FboInstance baseFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, osWindow);
        menuManager.setMenuTargetFbo(osWindow, baseFbo);
        baseMenu = menuManager.openMenu(EditorWindowSetting.MENU_EDITOR_SECONDARY, osWindow);
    }

    // Accessible \\

    public MenuInstance getBaseMenu() {
        return baseMenu;
    }
}