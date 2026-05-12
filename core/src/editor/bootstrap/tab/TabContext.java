package editor.bootstrap.tab;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
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
     *
     * FBO: each tab window clones FBO_UI so every tab has its own isolated
     * render target. Using getFbo() (the shared singleton) causes all tab
     * chrome menus to write to the same texture — rendering corrupts as soon
     * as a second tab opens. Pattern matches MenuTargetFboSystem.
     *
     * Zero-size guard: if the BSP assigns a (0,0,0,0) rect during a split
     * frame, or the compositor zeroes an inactive tab, onResize returns early
     * so the content window never receives a zero-size resize. The next valid
     * computeRects frame pushes the real rect through via the normal resize
     * event.
     */

    // Internal
    private MenuManager menuManager;
    private FboManager fboManager;

    // Render Target
    private FboInstance uiFbo;

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

        // Clone — not get — so this tab window owns its own FBO.
        // Sharing the singleton FBO across multiple tab windows means every
        // chrome menu overwrites the same texture each frame.
        this.uiFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, getWindow());
        menuManager.setMenuTargetFbo(getWindow(), uiFbo);

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

        // Guard: tab window has no meaningful size yet. Fires when the BSP
        // emits a (0,0,0,0) rect on the split frame, or when the compositor
        // zeroes an inactive tab window. Do not forward to content FBO.
        if (width <= 0 || height <= 0)
            return;

        int[] canvas = menuManager.getCanvas(chromeMenu);

        if (canvas == null)
            return;

        if (canvas.length != EngineSetting.TAB_CANVAS_BOUNDS_LENGTH)
            return;

        float canvasW = canvas[EngineSetting.TAB_CANVAS_W_INDEX];
        float canvasH = canvas[EngineSetting.TAB_CANVAS_H_INDEX];

        // Guard: chrome menu canvas hasn't laid out yet. The next resize
        // triggered by a real rect will correct it.
        if (canvasW <= 0 || canvasH <= 0)
            return;

        float tabX = getWindow().getCompositeX();
        float tabY = getWindow().getCompositeY();
        float x = tabX + canvas[EngineSetting.TAB_CANVAS_X_INDEX];
        float y = tabY + canvas[EngineSetting.TAB_CANVAS_Y_INDEX];

        contentWindow.setCompositeRect(x, y, canvasW, canvasH);
        contentWindow.resize((int) canvasW, (int) canvasH);
    }

    // Accessible \\

    public MenuInstance getChromeMenu() {
        return chromeMenu;
    }

    public FboInstance getUiFbo() {
        return uiFbo;
    }
}