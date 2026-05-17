package editor.bootstrap.tab;

import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.runtime.RuntimeSetting;
import engine.root.ContextPackage;
import engine.root.EngineSetting;

public class TabContext extends ContextPackage {

    /*
     * Thin context shell for a single editor tab. Owns the chrome menu and
     * cascades resize events down to the content context — the same way
     * EditorTabCompositorSystem cascades editor canvas bounds down to this
     * tab window. One level deeper, identical mechanism.
     *
     * linkContent() stores the peer content context reference returned from
     * createContext(). No lifecycle ownership — the engine drives both contexts
     * equally. onResize() is the only bridge: reads this tab's canvas bounds
     * and pushes them to the content window.
     *
     * FBO: cloned per tab so every chrome menu writes to its own render target.
     * Sharing the singleton FBO causes all tab menus to overwrite the same
     * texture as soon as a second tab opens.
     *
     * Zero-size guard: BSP emits (0,0,0,0) on the split frame. Canvas guard:
     * menu layout runs one frame after awake. Both guards live in onResize so
     * the content window never receives an invalid rect.
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
    private ContextPackage contentContext;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        menuManager = get(MenuManager.class);
        fboManager = get(FboManager.class);
        fboRenderSystem = get(FboRenderSystem.class);
    }

    @Override
    protected void awake() {
        uiFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, getWindow());
        menuManager.setMenuTargetFbo(getWindow(), uiFbo);
        chromeMenu = menuManager.openMenu(EngineSetting.MENU_TAB_SHELL, getWindow());
    }

    // Render \\

    @Override
    protected void render() {

        if (!getWindow().hasCompositeRect())
            return;

        fboRenderSystem.pushFbo(uiFbo, RuntimeSetting.LAYER_UI, getWindow());
    }

    // Management \\

    public void linkContent(ContextPackage contentContext) {
        this.contentContext = contentContext;
    }

    // Accessible \\

    public MenuInstance getChromeMenu() {
        return chromeMenu;
    }

    public FboInstance getUiFbo() {
        return uiFbo;
    }
}