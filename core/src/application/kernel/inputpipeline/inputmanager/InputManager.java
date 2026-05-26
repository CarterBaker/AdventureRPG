package application.kernel.inputpipeline.inputmanager;

import application.bootstrap.shaderpipeline.sprite.SpriteHandle;
import application.bootstrap.shaderpipeline.sprite.SpriteInstance;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.input.Binding;
import engine.input.Input;
import engine.root.EngineContext;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector2;

public class InputManager extends ManagerPackage {

    /*
     * Thin bridge between the engine input backend and bootstrap systems.
     * Owns sensitivity-scaled mouse delta and binding query convenience.
     * Writes a full hardware snapshot into a RawInputHandle each frame —
     * reuses pre-allocated arrays to avoid per-frame heap allocation.
     *
     * Input routes to the focused window — the window the user last clicked.
     * Cursor capture pins routing to the captured window while in play mode.
     *
     * Focus is detected here each frame: any mouse click on a window that is
     * not already focused shifts focus to it and syncs capture state. Capture
     * follows focus — granted when the resolved content window has no
     * input-locked menus, released when it does.
     *
     * syncFocus reads EngineContext.input which is set exclusively by
     * syncInputForWindow, called from WindowManager.syncHoveredWindow() earlier
     * in the same frame update. bindContext no longer touches EngineContext.input,
     * so GL context switches during rendering cannot corrupt the input context
     * established by syncHoveredWindow. WindowManager must update before
     * InputManager for EngineContext.input to be correct here.
     *
     * resolveInputAuthority() maps a focused window to the window that actually
     * owns lock state. The resolver is pushed in by the editor layer (TabManager)
     * so the kernel never names any editor type — only WindowInstance crosses
     * the boundary. captureEligible on WindowInstance blocks editor chrome
     * windows from ever capturing the cursor regardless of lock state.
     *
     * onInputLockReleased() is called by the editor layer when a lock_input menu
     * closes on a content window. Capture is automatically restored without
     * requiring a re-click. Package-accessible; not intended for game code.
     *
     * Mouse coordinates returned by getMouseX(), getMouseY(), and written by
     * writeRawInput() are window-local — the composite rect origin is subtracted
     * from the raw screen position before use. This is required for correct menu
     * hit testing and game input on any tab that does not sit at the screen
     * origin: element and FBO positions are always relative to the window's own
     * top-left corner, so raw screen coords only happen to match for the first
     * tab when the dock starts at (0, 0). Every subsequent tab has a non-zero
     * offset and would receive mismatched coordinates without this adjustment.
     *
     * All cursor platform calls are owned by CursorSystem. InputManager drives
     * when capture is granted or released; CursorSystem executes the calls.
     * The sprite cursor and clearCursor methods here are pointers to CursorSystem.
     *
     * focusIndependent windows (e.g. the toolbar) bypass the focus check in
     * isHovered() and route against hoveredWindow instead, allowing editor
     * chrome to remain interactive without ever acquiring focus.
     *
     * getGlobalMouseX/Y() returns the raw cursor position as provided by the
     * platform for the currently input-synced window. This is NOT translated
     * into any window's local coordinate space — it is the value before any
     * localX/localY adjustment. Used by TabDragManager exclusively for OS
     * window hit testing and cross-window BSP lookup during drag resolution.
     *
     * getCursorXForWindow/getCursorYForWindow() query the platform directly for
     * the cursor position relative to a specific OS window, bypassing the
     * hoveredWindowLocked / syncInputForWindow mechanism entirely. This is the
     * only correct way to get window-local cursor coords during a drag when the
     * input-synced window differs from the OS window being tested. Callers must
     * pass a window with a native handle; results for logical windows are
     * undefined.
     */

    // Internal
    private CursorSystem cursorSystem;
    private WindowManager windowManager;

    // Authority resolver — pushed in by the editor layer, null in game-only builds
    @FunctionalInterface
    public interface InputAuthorityResolver {
        WindowInstance resolve(WindowInstance focused);
    }

    private InputAuthorityResolver authorityResolver;

    // Mouse delta
    private Vector2 mouseDelta;
    private static final Vector2 ZERO_DELTA = new Vector2(0, 0);

    // Pre-allocated snapshot buffers — reused every frame
    private final boolean[] kc = new boolean[512];
    private final boolean[] kh = new boolean[512];
    private final boolean[] kr = new boolean[512];
    private final boolean[] bc = new boolean[8];
    private final boolean[] bh = new boolean[8];
    private final boolean[] br = new boolean[8];

    // Internal \\

    @Override
    protected void create() {
        this.cursorSystem = create(CursorSystem.class);
        this.mouseDelta = new Vector2();
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void update() {
        syncFocus();
    }

    // Focus \\

    /*
     * Detects a click on a window that does not already own focus and shifts
     * focus to it. EngineContext.input reflects the hovered window at this
     * point because WindowManager.update() has already run syncHoveredWindow(),
     * which calls syncInputForWindow on the resolved OS window. bindContext
     * does not reassign EngineContext.input, so no GL context switch between
     * syncHoveredWindow and here can corrupt it.
     */
    private void syncFocus() {

        if (!EngineContext.input.isMouseClicked(0) && !EngineContext.input.isMouseClicked(1))
            return;

        WindowInstance hovered = windowManager.getHoveredWindow();
        if (hovered == null || hovered == windowManager.getFocusedWindow())
            return;

        windowManager.setFocusedWindow(hovered);
        onWindowFocused(hovered);
    }

    private void onWindowFocused(WindowInstance window) {

        if (!window.isCaptureEligible())
            return;

        if (windowManager.getCapturedWindow() != null)
            cursorSystem.releaseCapture();

        WindowInstance authority = resolveInputAuthority(window);
        if (authority == null || authority.getMenuListHandle().isInputLocked())
            return;

        cursorSystem.capture(authority);
        authority.getMenuListHandle().setLockReleaseListener(() -> onInputLockReleased(authority));
    }

    // Called by the editor layer when a lock_input menu closes on a content window.
    public void onInputLockReleased(WindowInstance authority) {

        WindowInstance focused = windowManager.getFocusedWindow();

        if (focused == null)
            return;

        if (resolveInputAuthority(focused) != authority)
            return;

        cursorSystem.capture(authority);
        authority.getMenuListHandle().setLockReleaseListener(() -> onInputLockReleased(authority));
    }

    private WindowInstance resolveInputAuthority(WindowInstance window) {

        if (authorityResolver != null)
            return authorityResolver.resolve(window);

        return window;
    }

    // Resolver — pushed in by the editor layer once at startup \\

    public void setAuthorityResolver(InputAuthorityResolver resolver) {
        this.authorityResolver = resolver;
    }

    // Guard \\

    private boolean isHovered(WindowInstance window) {

        WindowInstance captured = windowManager.getCapturedWindow();

        if (captured != null)
            return window == captured;

        if (window.isFocusIndependent())
            return window == windowManager.getHoveredWindow();

        return window == windowManager.getFocusedWindow();
    }

    // Coordinate utility — converts a raw screen coordinate to window-local
    // by subtracting the composite rect origin. Element and FBO positions are
    // always expressed relative to the window's own corner, so any window
    // sitting at a non-zero screen position must have its origin stripped
    // before the coordinate is used for hit testing or game input. \\

    private float localX(WindowInstance window, float screenX) {
        return window.hasCompositeRect() ? screenX - window.getCompositeX() : screenX;
    }

    private float localY(WindowInstance window, float screenY) {
        return window.hasCompositeRect() ? screenY - window.getCompositeY() : screenY;
    }

    // Frame write \\

    public void writeRawInput(RawInputHandle handle, WindowInstance window) {

        if (!isHovered(window)) {
            handle.clear();
            return;
        }

        Input raw = EngineContext.input;

        for (int i = 0; i < 512; i++) {
            kc[i] = raw.isKeyClicked(i);
            kh[i] = raw.isKeyDown(i);
            kr[i] = raw.isKeyReleased(i);
        }

        for (int i = 0; i < 8; i++) {
            bc[i] = raw.isMouseClicked(i);
            bh[i] = raw.isMouseDown(i);
            br[i] = raw.isMouseReleased(i);
        }

        handle.write(
                kc, kh, kr,
                bc, bh, br,
                localX(window, raw.getMouseX()),
                localY(window, raw.getMouseY()),
                raw.getDeltaX() * internal.settings.mouseSensitivity,
                raw.getDeltaY() * internal.settings.mouseSensitivity);
    }

    // Convenience — bindings \\

    public boolean bindingHeld(Binding binding, WindowInstance window) {
        return isHovered(window) && binding.isDown(EngineContext.input);
    }

    public boolean bindingClicked(Binding binding, WindowInstance window) {
        return isHovered(window) && binding.isClicked(EngineContext.input);
    }

    public boolean bindingReleased(Binding binding, WindowInstance window) {
        return isHovered(window) && binding.isReleased(EngineContext.input);
    }

    // Convenience — direct queries \\

    public float getMouseX(WindowInstance window) {
        return isHovered(window) ? localX(window, EngineContext.input.getMouseX()) : 0f;
    }

    public float getMouseY(WindowInstance window) {
        return isHovered(window) ? localY(window, EngineContext.input.getMouseY()) : 0f;
    }

    public Vector2 getMouseDelta(WindowInstance window) {
        if (!isHovered(window))
            return ZERO_DELTA;
        mouseDelta.set(
                EngineContext.input.getDeltaX() * internal.settings.mouseSensitivity,
                EngineContext.input.getDeltaY() * internal.settings.mouseSensitivity);
        return mouseDelta;
    }

    // Global cursor position \\

    /*
     * Returns the raw cursor X as provided by the platform for the currently
     * input-synced window. No composite-rect subtraction is applied. Use this
     * when you need a coordinate that can be compared against OS window screen
     * positions (e.g. TabDragManager.resolveDropTarget) rather than against
     * element/FBO layout coordinates.
     *
     * This is distinct from getMouseX(window), which (a) gates on focus/hover
     * and (b) subtracts the compositeRect origin for logical windows.
     */
    public float getGlobalMouseX() {
        return EngineContext.input.getMouseX();
    }

    public float getGlobalMouseY() {
        return EngineContext.input.getMouseY();
    }

    /*
     * Returns the cursor position relative to a specific OS window, bypassing
     * hoveredWindowLocked and the syncInputForWindow / focused-window mechanism
     * entirely. Backed by a direct platform query (e.g. glfwGetCursorPos).
     *
     * This is the only correct way to get window-local coords during a drag
     * when the input-synced window (the drag source) is different from the OS
     * window being tested as a drop target. The caller is responsible for
     * passing a window with a native handle — results for logical windows are
     * undefined.
     */
    public float getCursorXForWindow(WindowInstance osWindow) {
        return internal.windowPlatform.getCursorX(osWindow);
    }

    public float getCursorYForWindow(WindowInstance osWindow) {
        return internal.windowPlatform.getCursorY(osWindow);
    }

    // Platform — delegates to CursorSystem \\

    public void captureCursor(boolean captured, WindowInstance window) {
        cursorSystem.captureCursor(captured, window);
    }

    public void setCursorSprite(SpriteHandle handle) {
        cursorSystem.setCursorSprite(handle);
    }

    public void setCursorSprite(SpriteInstance instance) {
        cursorSystem.setCursorSprite(instance);
    }

    public void clearCursor() {
        cursorSystem.clearCursor();
    }

    // Hover coordinates — no focus gate. Position testing does not require
    // input authority; only binding queries do.
    public float getHoverMouseX(WindowInstance window) {
        return localX(window, EngineContext.input.getMouseX());
    }

    public float getHoverMouseY(WindowInstance window) {
        return localY(window, EngineContext.input.getMouseY());
    }
}