package application.kernel.inputpipeline.inputmanager;

import application.bootstrap.shaderpipeline.sprite.SpriteHandle;
import application.bootstrap.shaderpipeline.sprite.SpriteInstance;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.input.Binding;
import engine.input.Input;
import engine.root.EngineContext;
import engine.root.EngineUtility;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector2;

public class InputManager extends ManagerPackage {

    /*
     * Thin bridge between the engine input backend and bootstrap systems.
     *
     * Every per-window query resolves its own raw Input via getRawInput(window),
     * which always asks the platform for the Input backing that window's own
     * OS window (WindowInstance.getGLWindow()). Nothing here reads
     * EngineContext.input as a source of truth, and nothing but
     * publishActiveInput() ever assigns it. That global exists purely as a
     * convenience for code elsewhere that doesn't carry a WindowInstance
     * around (gameplay bindings); anything that has a WindowInstance in hand
     * — which is everything in the editor — must use getRawInput(window) or
     * one of the window-scoped methods below.
     *
     * syncFocus() and publishActiveInput() run in that order, once per frame.
     * syncFocus() is the single authority for what a click does to focus —
     * nothing else in the engine may reassign focusedWindow or trigger
     * capture. publishActiveInput() then commits EngineContext.input to
     * reflect whatever focus/capture resolved to, exactly once, so every
     * consumer sees a value that's correct for the rest of the frame.
     *
     * resolveInputAuthority() maps a focused window to the window that
     * actually owns lock state, pushed in by the editor layer so the kernel
     * never names an editor type. captureEligible blocks editor chrome from
     * ever capturing the cursor regardless of lock state.
     *
     * onInputLockReleased() is called by the editor layer when a lock_input
     * menu closes on a content window, restoring capture without a re-click.
     *
     * getGlobalMouseX/Y(window) and getRawInput(window) exist for gestures
     * that must track hardware state across multiple frames independent of
     * hover/focus — e.g. tab dragging — so a gesture never has to reason
     * about which window happens to be focused elsewhere while it runs.
     */

    private CursorSystem cursorSystem;
    private WindowManager windowManager;

    @FunctionalInterface
    public interface InputAuthorityResolver {
        WindowInstance resolve(WindowInstance focused);
    }

    private InputAuthorityResolver authorityResolver;

    private Vector2 mouseDelta;
    private static final Vector2 ZERO_DELTA = new Vector2(0, 0);

    private final boolean[] kc = new boolean[512];
    private final boolean[] kh = new boolean[512];
    private final boolean[] kr = new boolean[512];
    private final boolean[] bc = new boolean[8];
    private final boolean[] bh = new boolean[8];
    private final boolean[] br = new boolean[8];

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
    protected void awake() {
        EngineUtility.assignInputManager(this);
    }

    @Override
    protected void update() {
        syncFocus();
        publishActiveInput();
    }

    // Raw Input Access \\

    /*
     * Returns the Input backing the given window's own OS window. The one
     * correct way to poll hardware state for a specific window — never
     * through EngineContext.input.
     */
    public Input getRawInput(WindowInstance window) {
        return internal.windowPlatform.getInputForWindow(window.getGLWindow());
    }

    // Focus \\

    /*
     * The single authority for what a click does to focus. Reads the click
     * state from the hovered window's own OS window directly, not from
     * EngineContext.input — on the frame focus changes to a not-yet-focused
     * OS window, EngineContext.input still reflects last frame's focus and
     * would never see the click meant to change it.
     */
    private void syncFocus() {

        WindowInstance hovered = windowManager.getHoveredWindow();

        if (hovered == null)
            return;

        if (hovered.isFocusIndependent())
            return;

        Input rawInput = getRawInput(hovered);

        if (!rawInput.isMouseClicked(0) && !rawInput.isMouseClicked(1))
            return;

        if (hovered != windowManager.getFocusedWindow()) {
            windowManager.setFocusedWindow(hovered);
            onWindowFocused(hovered);
            return;
        }

        if (windowManager.getCapturedWindow() == null)
            onWindowFocused(hovered);
    }

    /*
     * Commits EngineContext.input for the rest of the frame. Runs after
     * syncFocus() so any focus change made this frame is already reflected.
     * Captured window wins outright; otherwise the focused window;
     * otherwise the main window. The ONLY assignment to EngineContext.input
     * anywhere in the engine.
     */
    private void publishActiveInput() {

        WindowInstance active = windowManager.getCapturedWindow();

        if (active == null)
            active = windowManager.getFocusedWindow();

        if (active == null)
            active = windowManager.getMainWindow();

        EngineContext.input = getRawInput(active);
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

    public void setAuthorityResolver(InputAuthorityResolver resolver) {
        this.authorityResolver = resolver;
    }

    private boolean isHovered(WindowInstance window) {

        WindowInstance captured = windowManager.getCapturedWindow();

        if (captured != null)
            return window == captured;

        if (window.isFocusIndependent())
            return window == windowManager.getHoveredWindow();

        return window == windowManager.getFocusedWindow();
    }

    private float localX(WindowInstance window, float screenX) {
        return window.hasCompositeRect() ? screenX - window.getCompositeX() : screenX;
    }

    private float localY(WindowInstance window, float screenY) {
        return window.hasCompositeRect() ? screenY - window.getCompositeY() : screenY;
    }

    public void writeRawInput(RawInputHandle handle, WindowInstance window) {

        if (!isHovered(window)) {
            handle.clear();
            return;
        }

        Input raw = getRawInput(window);

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
                raw.getDeltaY() * internal.settings.mouseSensitivity,
                raw.getScrollX(),
                raw.getScrollY());
    }

    public boolean bindingHeld(Binding binding, WindowInstance window) {
        return isHovered(window) && binding.isDown(getRawInput(window));
    }

    public boolean bindingClicked(Binding binding, WindowInstance window) {
        return isHovered(window) && binding.isClicked(getRawInput(window));
    }

    public boolean bindingReleased(Binding binding, WindowInstance window) {
        return isHovered(window) && binding.isReleased(getRawInput(window));
    }

    public float getMouseX(WindowInstance window) {
        return isHovered(window) ? localX(window, getRawInput(window).getMouseX()) : 0f;
    }

    public float getMouseY(WindowInstance window) {
        return isHovered(window) ? localY(window, getRawInput(window).getMouseY()) : 0f;
    }

    public Vector2 getMouseDelta(WindowInstance window) {
        if (!isHovered(window))
            return ZERO_DELTA;
        Input raw = getRawInput(window);
        mouseDelta.set(
                raw.getDeltaX() * internal.settings.mouseSensitivity,
                raw.getDeltaY() * internal.settings.mouseSensitivity);
        return mouseDelta;
    }

    /*
     * Raw cursor X/Y for the OS window backing the given window. No
     * composite-rect subtraction, no hover/focus gating. Use this when a
     * coordinate must be compared against OS window screen positions (e.g.
     * TabDragManager resolving a drop target).
     */
    public float getGlobalMouseX(WindowInstance window) {
        return getRawInput(window).getMouseX();
    }

    public float getGlobalMouseY(WindowInstance window) {
        return getRawInput(window).getMouseY();
    }

    public float getCursorXForWindow(WindowInstance osWindow) {
        return internal.windowPlatform.getCursorX(osWindow);
    }

    public float getCursorYForWindow(WindowInstance osWindow) {
        return internal.windowPlatform.getCursorY(osWindow);
    }

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

    public float getHoverMouseX(WindowInstance window) {
        return localX(window, getRawInput(window).getMouseX());
    }

    public float getHoverMouseY(WindowInstance window) {
        return localY(window, getRawInput(window).getMouseY());
    }
}