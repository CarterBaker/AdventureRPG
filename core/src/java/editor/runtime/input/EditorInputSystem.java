package editor.runtime.input;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import engine.root.SystemPackage;

public class EditorInputSystem extends SystemPackage {

    /*
     * Raw input for an editor tool context, written first each frame. The
     * pointer is over the view while it is over this window and none of the
     * window's own menu elements are under it, which is all the wheel needs,
     * so an inactive view still zooms; it is active, and clicks count, only
     * while the window is also the active one. The press that activates a
     * tab makes it active that same frame, so it acts inside it. A tool that acts on a click
     * claims its button for that frame, so the view camera, which reads input
     * late, leaves that press to the tool; claims clear every frame.
     */

    // Internal
    private InputManager inputManager;
    private WindowManager windowManager;
    private ActiveWindowManager activeWindowManager;
    private ElementHitSystem elementHitSystem;

    // Raw Input
    private RawInputHandle rawInputHandle;

    // Claim — one bit per mouse button a tool took this frame
    private int claimedButtons;

    // Base \\

    @Override
    protected void create() {
        this.rawInputHandle = create(RawInputHandle.class);
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
        this.activeWindowManager = get(ActiveWindowManager.class);
        this.elementHitSystem = get(ElementHitSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        inputManager.writeRawInput(rawInputHandle, window);
        claimedButtons = 0;
    }

    // Claim \\

    // A press a tool acts on this frame, kept from the view camera
    public void claim(int button) {
        claimedButtons |= 1 << button;
    }

    public boolean isClaimed(int button) {
        return (claimedButtons & 1 << button) != 0;
    }

    // A click over the view that a tool takes, claimed in the same call
    public boolean takeClick(int button) {

        if (!isClicked(button))
            return false;

        claim(button);

        return true;
    }

    // Accessible \\

    public RawInputHandle getRawInputHandle() {
        return rawInputHandle;
    }

    // The pointer over the view itself, active or not, as hover-only input such as the wheel reads it
    public boolean isPointerOver() {

        WindowInstance window = context.getWindow();

        if (windowManager.getHoveredWindow() != window)
            return false;

        MenuInstance hoveredMenu = elementHitSystem.getHoveredMenu();
        return hoveredMenu == null || hoveredMenu.getWindow() != window;
    }

    public boolean isPointerActive() {
        return activeWindowManager.isActive(context.getWindow()) && isPointerOver();
    }

    public boolean isClicked(int button) {
        return isPointerActive() && rawInputHandle.isButtonClicked(button);
    }
}
