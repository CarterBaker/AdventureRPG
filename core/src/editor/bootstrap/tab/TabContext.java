package editor.bootstrap.tab;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.ContextPackage;
import engine.root.EngineSetting;

public class TabContext extends ContextPackage {

    /*
     * Thin context shell for a single editor tab. Owns the docker chrome menu
     * and a reference to a peer content window assigned at mount time. When the
     * tab window is resized by the compositor, onResize fires once and pushes
     * the corrected canvas bounds down to the content window — no per-frame
     * polling. The content context is a peer in the engine context array, not
     * a child — TabContext only holds the window reference, not the lifecycle.
     *
     * Canvas: chromeMenu.hasCanvas() gates the resize path. The menu writes
     * its canvas bounds directly onto the MenuInstance each frame via
     * CanvasAreaSystem — no manager call, no index constants.
     *
     * FBO: each tab window clones FBO_UI so every tab has its own isolated
     * render target. Using getFbo() (the shared singleton) causes all tab
     * chrome menus to write to the same texture — rendering corrupts as soon
     * as a second tab opens.
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
    private FboRenderSystem fboRenderSystem;

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
        this.fboRenderSystem = get(FboRenderSystem.class);
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

    @Override
    protected void update() {

        if (contentWindow == null)
            return;

        if (contentWindow.hasCompositeRect())
            return;

        if (!getWindow().hasCompositeRect())
            return;

        if (!chromeMenu.hasCanvas())
            return;

        // Canvas became valid after the first onResize() fired.
        // Propagate now — this runs exactly once.
        onResize((int) getWindow().getCompositeW(), (int) getWindow().getCompositeH());
    }

    // Render \\

    @Override
    protected void render() {

        if (!getWindow().hasCompositeRect())
            return;

        fboRenderSystem.pushFbo(uiFbo, RuntimeSetting.LAYER_UI, getWindow());
    }

    // Management \\

    public void mountContent(WindowInstance contentWindow) {

        this.contentWindow = contentWindow;

        // The compositor may have already fired resize before mount was called.
        // If we have a valid rect right now, push it immediately — otherwise
        // contentWindow sits at zero forever because the rect won't change again.
        int w = (int) getWindow().getCompositeW();
        int h = (int) getWindow().getCompositeH();

        if (w > 0 && h > 0)
            onResize(w, h);
    }

    public void unmountContent() {
        this.contentWindow = null;
    }

    public void deactivate() {

        // Clear both logical window rects so hasCompositeRect() returns false
        // on both. FboRenderSystem skips pushFbo for any logical window without
        // a composite rect, so neither the chrome FBO nor the content FBO will
        // blit while this tab is hidden.
        // Do not call resize — zero-size resizes cascade into content FBOs and
        // corrupt them. The layout system will fire a real resize when this tab
        // becomes active again.
        getWindow().clearCompositeRect();

        if (contentWindow != null)
            contentWindow.clearCompositeRect();
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

        // Guard: chrome menu canvas hasn't laid out yet. The next resize
        // triggered by a real rect will correct it.
        if (!chromeMenu.hasCanvas())
            return;

        float canvasX = chromeMenu.getCanvasX();
        float canvasY = chromeMenu.getCanvasY();
        float canvasW = chromeMenu.getCanvasW();
        float canvasH = chromeMenu.getCanvasH();

        // Guard: canvas element has zero size on the frame it first appears.
        if (canvasW <= 0 || canvasH <= 0)
            return;

        float tabX = getWindow().getCompositeX();
        float tabY = getWindow().getCompositeY();

        contentWindow.setCompositeRect(tabX + canvasX, tabY + canvasY, canvasW, canvasH);
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