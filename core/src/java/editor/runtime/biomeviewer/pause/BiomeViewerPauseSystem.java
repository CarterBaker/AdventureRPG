package editor.runtime.biomeviewer.pause;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import editor.runtime.biomeviewer.BiomeViewerSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;

public class BiomeViewerPauseSystem extends SystemPackage {

    /*
     * Pauses and resumes this viewer's flight. The tab opens paused, so
     * focusing it never takes the cursor; Pause or the Fly button resumes,
     * and Pause again pauses, closing only after the paused banner has shown
     * a full frame. The banner locks the window's input, which frees the
     * cursor and holds the camera where it is, so the viewer streams nothing
     * while the editor is worked in. Only this window pauses; the game's
     * clock and every other tab run on.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;

    // State
    private MenuInstance pausedMenu;
    private boolean resumeArmed;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
    }

    @Override
    protected void awake() {
        pause();
    }

    // Update \\

    @Override
    protected void update() {

        boolean pausePressed = inputManager.bindingClicked(KeyBindings.PAUSE, context.getWindow());

        if (pausedMenu == null) {

            if (pausePressed)
                pause();

            return;
        }

        if (pausePressed && resumeArmed) {
            resume();
            return;
        }

        this.resumeArmed = pausedMenu.isVisible();
    }

    // Pause \\

    private void pause() {
        this.pausedMenu = menuManager.openMenu(BiomeViewerSetting.MENU_PAUSED, context.getWindow());
        this.resumeArmed = false;
    }

    public void resume() {

        if (pausedMenu == null)
            return;

        this.pausedMenu = menuManager.closeMenu(pausedMenu);
    }
}
