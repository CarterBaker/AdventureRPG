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
     * from the raw screen position before use. Required for correct menu hit
     * testing and game input on any tab that does not sit at the screen origin:
     * element and FBO positions are always relative to the window's own top-left
     * corner, so raw screen coords only happen to match for the first tab when
     * the dock starts at (0, 0).
     *
     * All cursor platform calls are owned by CursorSystem. InputManager drives
     * when capture is granted or released; CursorSystem executes the calls.
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
        return authorityResolver != null ? authorityResolver.resolve(window) : window;
    }

    // Resolver — pushed in by the editor layer once at startup \\

    public void setAuthorityResolver(InputAuthorityResolver resolver) {
        this.authorityResolver = resolver;
    }

    // Guard \\

    private boolean isHovered(WindowInstance window) {
        WindowInstance captured = windowManager.getCapturedWindow();
        return captured != null ? window == captured : window == windowManager.getFocusedWindow();
    }

    // Coordinate utility \\

    // Converts a raw screen coordinate to window-local by subtracting the
    // composite rect origin. Element and FBO positions are always expressed
    // relative to the window's own corner, so any window sitting at a non-zero
    // screen position must have its origin stripped before coordinates are used
    // for hit testing or game input.

    private float localX(WindowInstance window, float screenX) {
        return window.hasCompositeRect() ? screenX - window.getCompositeX() : screenX;
    }

    private float localY(WindowInstance window, float screenY) {
        return window.hasCompositeRect() ? screenY - window.getCompositeY() : screenY;
    }

    // Hover-safe coordinate queries — window-local without focus guard.
    // Used by ElementHitSystem when the raycast target is an alwaysHover window
    // that is hovered but not focused. Geometry-only; no capture or focus check.

    public float getMouseXLocal(WindowInstance window) {
        return localX(window, EngineContext.input.getMouseX());
    }

    public float getMouseYLocal(WindowInstance window) {
        return localY(window, EngineContext.input.getMouseY());
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
}