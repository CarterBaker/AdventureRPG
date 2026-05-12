package editor.bootstrap.tab;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.ContextPackage;
import engine.root.EngineSetting;

public class TabContext extends ContextPackage {

    /*
     * Thin context shell for a single editor tab. Owns the docker chrome menu
     * and one content window. When the tab window is resized by the compositor,
     * onResize fires once and pushes the corrected canvas bounds down to the
     * content window — no per-frame polling.
     */

    // Internal
    private MenuManager menuManager;
    private FboManager fboManager;

    // Chrome
    private MenuInstance chromeMenu;

    // Content
    private WindowInstance contentWindow;

    // Internal \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void awake() {
        menuManager.setMenuTargetFbo(getWindow(), fboManager.getFbo(RuntimeSetting.FBO_UI));
        this.chromeMenu = menuManager.openMenu(EngineSetting.MENU_TAB_SHELL, getWindow());
    }

    // Management \\

    public void mountContent(WindowInstance contentWindow) {
        this.contentWindow = contentWindow;
    }

    public void unmountContent() {
        this.contentWindow = null;
    }

    // Resize \\

    @Override
    public void onResize(int width, int height) {
        if (contentWindow == null)
            return;

        int[] canvas = menuManager.getCanvas(chromeMenu);

        if (canvas == null)
            return;

        if (canvas.length != EngineSetting.TAB_CANVAS_BOUNDS_LENGTH)
            return;

        float tabX = getWindow().getCompositeX();
        float tabY = getWindow().getCompositeY();

        float x = tabX + canvas[EngineSetting.TAB_CANVAS_X_INDEX];
        float y = tabY + canvas[EngineSetting.TAB_CANVAS_Y_INDEX];
        float w = canvas[EngineSetting.TAB_CANVAS_W_INDEX];
        float h = canvas[EngineSetting.TAB_CANVAS_H_INDEX];

        contentWindow.setCompositeRect(x, y, w, h);
        contentWindow.resize((int) w, (int) h);
    }

    // Accessible \\

    public MenuInstance getChromeMenu() {
        return chromeMenu;
    }
}