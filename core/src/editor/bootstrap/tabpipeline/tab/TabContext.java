package editor.bootstrap.tabpipeline.tab;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import engine.root.ContextPackage;
import engine.root.EngineSetting;

public class TabContext extends ContextPackage {

    /*
     * Owns the chrome window, chrome FBO, chrome menu, and the content context
     * that lives inside the tab's canvas area.
     *
     * placeAt() is the single method for positioning this tab. It sets the chrome
     * window rect and derives the content rect from the chrome canvas in one call.
     * Every caller — pushRects, compositor sync, drag tracking — uses placeAt().
     * Neither chrome nor content composite rects are written anywhere else.
     *
     * moveTo() reparents both windows to a new OS window. No destroy, no rebuild,
     * no lifecycle gap.
     *
     * onResize() resizes the FBO when the engine drives a window resize after
     * placeAt() calls tabWindow.resize().
     */

    // Internal
    private MenuManager menuManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private WindowManager windowManager;
    private InputManager inputManager;

    // Render Target
    private FboInstance uiFbo;

    // Chrome
    private MenuInstance chromeMenu;

    // Content — owned by this tab, positioned within this tab's canvas
    private ContextPackage contentContext;

    // Internal \\

    @Override
    protected void get() {
        menuManager = get(MenuManager.class);
        fboManager = get(FboManager.class);
        fboRenderSystem = get(FboRenderSystem.class);
        windowManager = get(WindowManager.class);
        inputManager = get(InputManager.class);
    }

    @Override
    protected void awake() {
        uiFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, getWindow());
        menuManager.setMenuTargetFbo(getWindow(), uiFbo);
        chromeMenu = menuManager.openMenu(EngineSetting.MENU_TAB_SHELL, getWindow());
    }

    @Override
    public void onResize(int width, int height) {

        if (uiFbo == null || width <= 0 || height <= 0)
            return;

        uiFbo.resize(width, height);
    }

    // Render \\

    @Override
    protected void render() {

        if (!getWindow().hasCompositeRect())
            return;

        fboRenderSystem.pushFbo(uiFbo, RuntimeSetting.LAYER_UI, getWindow());
    }

    // Dispose \\

    @Override
    protected void dispose() {

        if (chromeMenu != null) {
            menuManager.closeMenu(chromeMenu);
            chromeMenu = null;
        }

        menuManager.setMenuTargetFbo(getWindow(), null);
    }

    // Management \\

    /*
     * Pairs this tab to its content context. Called once after both contexts are
     * created. Wires the input lock release listener to the content window.
     */
    public void linkContent(ContextPackage contentContext) {

        this.contentContext = contentContext;

        contentContext.getWindow().getMenuListHandle().setLockReleaseListener(
                () -> inputManager.onInputLockReleased(contentContext.getWindow()));
    }

    /*
     * The single method for positioning this tab. Places the chrome window at the
     * given rect, then derives the content window position from the tab_canvas
     * element bounds inside the chrome menu.
     *
     * The canvas stores its position in the chrome window's local pixel space
     * (origin at top-left of the chrome window). Adding the chrome window's
     * composite origin converts those local coords to OS-window composite space,
     * which is what setCompositeRect expects.
     *
     * If the canvas is not yet ready (menu hasn't rendered its first frame),
     * content falls back to the full chrome rect. The compositor will push a
     * corrected placeAt() on the next frame once canvas bounds are written.
     *
     * This is the only place either window's composite rect is written.
     */
    public void placeAt(float x, float y, float w, float h) {

        if (w <= 0 || h <= 0)
            return;

        // Chrome — always gets the full BSP rect
        WindowInstance tabWindow = getWindow();
        tabWindow.setCompositeRect(x, y, w, h);
        tabWindow.resize((int) w, (int) h);

        if (contentContext == null)
            return;

        WindowInstance contentWindow = contentContext.getWindow();

        // Canvas is in chrome-local pixel space — offset by chrome composite origin
        // to get OS-window composite space for the content window.
        if (chromeMenu != null && chromeMenu.getCanvas() != null) {

            float cw = chromeMenu.getCanvas().getW();
            float ch = chromeMenu.getCanvas().getH();

            if (cw > 0 && ch > 0) {
                float cx = x + chromeMenu.getCanvas().getX();
                float cy = y + chromeMenu.getCanvas().getY();
                contentWindow.setCompositeRect(cx, cy, cw, ch);
                contentWindow.resize((int) cw, (int) ch);
                return;
            }
        }

        // Canvas not ready yet — use full chrome rect until first render writes bounds
        contentWindow.setCompositeRect(x, y, w, h);
        contentWindow.resize((int) w, (int) h);
    }

    /*
     * Reparents both the chrome window and the content window to a different OS
     * window. Both contexts stay alive — no rebuild, no lifecycle gap.
     */
    public void moveTo(WindowInstance targetOsWindow) {

        windowManager.reparentWindow(getWindow(), targetOsWindow);
        windowManager.reparentWindow(contentContext.getWindow(), targetOsWindow);
    }

    // Accessible \\

    public MenuInstance getChromeMenu() {
        return chromeMenu;
    }

    public FboInstance getUiFbo() {
        return uiFbo;
    }

    public ContextPackage getContentContext() {
        return contentContext;
    }
}