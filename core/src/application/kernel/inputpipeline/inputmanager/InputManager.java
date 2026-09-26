package application.kernel.inputpipeline.inputmanager;

import application.bootstrap.shaderpipeline.sprite.SpriteHandle;
import application.bootstrap.shaderpipeline.sprite.SpriteInstance;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.input.Binding;
import engine.input.Buttons;
import engine.input.Input;
import engine.root.EngineContext;
import engine.root.EngineUtility;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector2;

public class InputManager extends ManagerPackage {

    /*
     * Bridge between the input backend and the engine. Per-window queries
     * always read the Input of that window's own OS window. syncFocus() is the
     * single authority for focus and capture, and publishActiveInput() then
     * commits EngineContext.input once per frame for code that has no window in
     * hand.
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

    public Input getRawInput(WindowInstance window) {
        return internal.windowPlatform.getInputForWindow(window.getGLWindow());
    }

    // Focus \\

    private void syncFocus() {

        WindowInstance hovered = windowManager.getHoveredWindow();

        if (hovered == null)
            return;

        if (hovered.isFocusIndependent())
            return;

        Input rawInput = getRawInput(hovered);

        if (!rawInput.isMouseClicked(Buttons.LEFT) && !rawInput.isMouseClicked(Buttons.RIGHT))
            return;

        if (hovered != windowManager.getFocusedWindow()) {
            windowManager.setFocusedWindow(hovered);
            onWindowFocused(hovered);
            return;
        }

        if (windowManager.getCapturedWindow() == null)
            onWindowFocused(hovered);
    }

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