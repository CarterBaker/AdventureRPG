package launcher.runtime.window;

import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.input.Buttons;
import engine.input.Input;
import engine.root.SystemPackage;
import launcher.runtime.LauncherSetting;
import launcher.runtime.menueventsmanager.menus.LauncherMenuBranch;

public class LauncherWindowSystem extends SystemPackage {

    /*
     * Gives the launcher's borderless, transparent window its body. Each frame
     * the cursor, which the window manager reads whether or not the window
     * takes it, is tested against every menu the window shows: on a menu the
     * window takes the cursor at once, and once the cursor has stayed off
     * every menu long enough for a menu just opened to be laid out, it passes
     * through to the desktop behind. Pressing on the title grabs the window,
     * which then follows the cursor by the point it was grabbed at until
     * released.
     */

    // Internal
    private InputManager inputManager;
    private LauncherMenuBranch launcherMenuBranch;

    // Drag
    private boolean dragging;
    private float grabX;
    private float grabY;

    // Passthrough
    private int framesOffMenus;
    private boolean passthrough;

    // Scratch
    private float[] cursorScratch;

    // Base \\

    @Override
    protected void create() {
        this.cursorScratch = new float[2];
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
        this.launcherMenuBranch = get(LauncherMenuBranch.class);
    }

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        float hoverX = inputManager.getHoverMouseX(window);
        float hoverY = inputManager.getHoverMouseY(window);

        updateDrag(window, inputManager.getRawInput(window), hoverX, hoverY);
        updatePassthrough(window, dragging || window.getMenuListHandle().containsPoint(hoverX, hoverY));
    }

    // Drag \\

    // The cursor is read in the window's own screen units, so the window moves by exactly what the cursor did
    private void updateDrag(WindowInstance window, Input rawInput, float hoverX, float hoverY) {

        internal.windowPlatform.getCursorPos(window, cursorScratch);

        if (rawInput.isMouseClicked(Buttons.LEFT) && launcherMenuBranch.isOverTitle(hoverX, hoverY)) {
            this.dragging = true;
            this.grabX = cursorScratch[0];
            this.grabY = cursorScratch[1];
        }

        this.dragging &= rawInput.isMouseDown(Buttons.LEFT);

        if (!dragging)
            return;

        internal.windowPlatform.moveWindow(
                window,
                Math.round(internal.windowPlatform.getScreenX(window) + cursorScratch[0] - grabX),
                Math.round(internal.windowPlatform.getScreenY(window) + cursorScratch[1] - grabY));
    }

    // Passthrough \\

    private void updatePassthrough(WindowInstance window, boolean onMenu) {

        this.framesOffMenus = onMenu ? 0 : Math.min(framesOffMenus + 1, LauncherSetting.PASSTHROUGH_SETTLE_FRAMES);
        boolean passthrough = framesOffMenus >= LauncherSetting.PASSTHROUGH_SETTLE_FRAMES;

        if (this.passthrough == passthrough)
            return;

        this.passthrough = passthrough;
        internal.windowPlatform.setMousePassthrough(window, passthrough);
    }
}
