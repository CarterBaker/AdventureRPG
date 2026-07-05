package application.bootstrap.menupipeline.elementhitsystem;

import java.lang.reflect.Method;

import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementStateStruct;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menulist.MenuListHandle;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.input.Input;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementHitSystem extends SystemPackage {

    /*
     * Hover and click dispatch across all hovered windows.
     *
     * Both updateRaycast and clearHoverIfWindowChanged receive the full
     * hoveredWindows list from WindowManager, sorted by priority (index 0 is
     * the highest-priority window). All iteration goes window-by-window in
     * that order. First hit wins, for both hover and click.
     *
     * Callback dispatch — on_click, on_drag, and all three hover states —
     * goes through the exact same executeCallback()/resolveCallback() path.
     * The window and element the interaction happened on are always known
     * at the call site (this system just finished determining them) and are
     * always passed along; a target method opts into whichever pieces of
     * that context it actually needs simply by declaring a parameter of
     * that type — String for a literal/injected arg, MenuInstance,
     * WindowInstance, or ElementInstance, in any combination, resolved once
     * per (class, method) via reflection and cached. There is no separate
     * "menu-aware" dispatch path and no sentinel argument value that means
     * something different from a literal string — a method that wants the
     * menu declares a MenuInstance parameter, full stop. This is what lets
     * a handler like a tab-drag callback receive the exact window the
     * gesture started on directly, instead of re-deriving "which window is
     * this" a second time via a separately computed global lookup that
     * isn't guaranteed to agree with the one that triggered the callback.
     *
     * on_drag latches on the frame primary is first pressed over an element
     * that has on_drag defined, remembering both the element and the window
     * it was hovered on at that moment. While latched the callback fires
     * every frame the primary button is physically held on that same
     * window — queried directly via InputManager.getRawInput(), never
     * through the engine-wide EngineContext.input global, so a drag
     * gesture's own continuation can't be corrupted by focus changing
     * elsewhere in the meantime. The latch releases the frame the button is
     * no longer held. on_click fires once on primary press when no drag is
     * latching.
     *
     * openClickState — the currently expanded dropdown element, paired with
     * openClickStateWindow to track which window owns it. The collapse check
     * fires per frame using that window's cursor coordinates, and also collapses
     * immediately if that window is no longer in hoveredWindows.
     */

    private InputManager inputManager;

    private ElementInstance hoveredElement;
    private ElementInstance draggedElement;
    private ElementInstance openClickState;
    private WindowInstance hoveredElementWindow;
    private WindowInstance draggedElementWindow;
    private WindowInstance openClickStateWindow;
    private float collapseTolerance;

    private Object2ObjectOpenHashMap<String, ResolvedCallback> resolvedCallbacks;

    @Override
    protected void create() {
        this.collapseTolerance = EngineSetting.DROPDOWN_COLLAPSE_TOLERANCE;
        this.resolvedCallbacks = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
    }

    // Entry Point \\

    public void updateRaycast(ObjectArrayList<WindowInstance> hoveredWindows) {

        // Drag is fully independent of hover and window focus once latched.
        // Polls the window it started on directly — never the ambient
        // engine-wide input — so window boundaries or a focus change
        // elsewhere cannot interrupt the gesture.
        if (draggedElement != null) {

            Input rawInput = inputManager.getRawInput(draggedElementWindow);

            if (rawInput.isMouseDown(0)) {
                executeCallback(
                        draggedElement.getEffectiveOnDragClass(),
                        draggedElement.getEffectiveOnDragMethod(),
                        draggedElement.getEffectiveOnDragArg(),
                        null, draggedElementWindow, draggedElement);
                return;
            }

            draggedElement = null;
            draggedElementWindow = null;
        }

        if (hoveredWindows.isEmpty())
            return;

        updateHover(hoveredWindows);
        fireOnHoverPerFrame();
        checkClickStateCollapse(hoveredWindows);

        // Check for a primary click — use the highest-priority hovered window
        // as the authority. If nothing is clicked, nothing to dispatch.
        if (!inputManager.bindingClicked(KeyBindings.PRIMARY, hoveredWindows.get(0)))
            return;

        // Latch drag on the frame primary is first pressed over a draggable
        // element. The window it was hovered on at this exact moment is
        // remembered for the gesture's whole duration.
        if (hoveredElement != null && hoveredElement.hasOnDrag()) {
            draggedElement = hoveredElement;
            draggedElementWindow = hoveredElementWindow;
            return;
        }

        // Click dispatch — iterate windows in priority order. First element
        // hit across any window's menus consumes the click.
        for (int i = 0; i < hoveredWindows.size(); i++) {

            WindowInstance window = hoveredWindows.get(i);
            MenuListHandle menuList = window.getMenuListHandle();

            if (!menuList.isRaycastLocked())
                continue;

            float mx = inputManager.getHoverMouseX(window);
            float my = inputManager.getHoverMouseY(window);

            ObjectArrayList<MenuInstance> menus = menuList.getMenus();

            for (int j = menus.size() - 1; j >= 0; j--) {
                MenuInstance menu = menus.get(j);
                if (!menu.isVisible())
                    continue;
                if (hitTestElements(menu.getElements(), mx, my,
                        0, 0, window.getWidth(), window.getHeight(), menu))
                    return;
            }
        }
    }

    public void resetPressed() {
        this.openClickState = null;
        this.openClickStateWindow = null;
        this.draggedElement = null;
        this.draggedElementWindow = null;
        clearHover();
    }

    // Hover \\

    private void updateHover(ObjectArrayList<WindowInstance> hoveredWindows) {

        // Walk windows in priority order — first element hit across any window wins.
        ElementInstance nextElement = null;
        WindowInstance nextWindow = null;

        outer: for (int i = 0; i < hoveredWindows.size(); i++) {

            WindowInstance window = hoveredWindows.get(i);
            MenuListHandle menuList = window.getMenuListHandle();

            if (!menuList.isRaycastLocked())
                continue;

            float mx = inputManager.getHoverMouseX(window);
            float my = inputManager.getHoverMouseY(window);

            ObjectArrayList<MenuInstance> menus = menuList.getMenus();

            for (int j = menus.size() - 1; j >= 0; j--) {
                MenuInstance menu = menus.get(j);
                if (!menu.isVisible())
                    continue;
                ElementInstance hit = hoverTestElements(
                        menu.getElements(), mx, my, 0, 0, window.getWidth(), window.getHeight());
                if (hit != null) {
                    nextElement = hit;
                    nextWindow = window;
                    break outer;
                }
            }
        }

        if (nextElement == hoveredElement)
            return;

        if (hoveredElement != null)
            fireHoverExit();

        if (nextElement == null)
            return;

        hoveredElement = nextElement;
        hoveredElementWindow = nextWindow;
        hoveredElement.setHovered(true);

        ElementStateStruct enterState = hoveredElement.getHandle().getHoverEnterState();
        if (enterState != null) {
            hoveredElement.setActiveHoverState(enterState);
            if (enterState.hasAction())
                executeCallback(enterState.getActionClass(), enterState.getActionMethod(),
                        enterState.getActionArg(), null, hoveredElementWindow, hoveredElement);
        }

        // Immediately transition to hoverState if defined
        ElementStateStruct hoverState = hoveredElement.getHandle().getHoverState();
        if (hoverState != null)
            hoveredElement.setActiveHoverState(hoverState);
    }

    private void fireHoverExit() {
        ElementStateStruct exitState = hoveredElement.getHandle().getHoverExitState();
        if (exitState != null) {
            hoveredElement.setActiveHoverState(exitState);
            if (exitState.hasAction())
                executeCallback(exitState.getActionClass(), exitState.getActionMethod(),
                        exitState.getActionArg(), null, hoveredElementWindow, hoveredElement);
        }
        hoveredElement.setHovered(false);
        hoveredElement = null;
        hoveredElementWindow = null;
    }

    private void fireOnHoverPerFrame() {
        if (hoveredElement == null)
            return;
        ElementStateStruct hoverState = hoveredElement.getHandle().getHoverState();
        if (hoverState == null)
            return;
        hoveredElement.setActiveHoverState(hoverState);
        if (hoverState.hasAction())
            executeCallback(hoverState.getActionClass(), hoverState.getActionMethod(),
                    hoverState.getActionArg(), null, hoveredElementWindow, hoveredElement);
    }

    private void clearHover() {
        if (hoveredElement == null)
            return;
        hoveredElement.clearActiveHoverState();
        hoveredElement.setHovered(false);
        hoveredElement = null;
        hoveredElementWindow = null;
    }

    public void clearHoverIfWindowChanged(ObjectArrayList<WindowInstance> hoveredWindows) {
        if (hoveredElement == null)
            return;
        if (draggedElement != null)
            return;
        if (hoveredWindows.contains(hoveredElementWindow))
            return;
        fireHoverExit();
    }

    // Hover Test \\

    private ElementInstance hoverTestElements(
            ObjectArrayList<ElementInstance> elements,
            float mouseX, float mouseY,
            float clipLeft, float clipTop,
            float clipRight, float clipBottom) {

        for (int i = elements.size() - 1; i >= 0; i--) {

            ElementInstance element = elements.get(i);

            if (element.hasChildren()) {
                float cl = clipLeft, ct = clipTop, cr = clipRight, cb = clipBottom;
                if (element.getElementData().isMask()) {
                    cl = Math.max(cl, element.getComputedLeft());
                    ct = Math.max(ct, element.getComputedTop());
                    cr = Math.min(cr, element.getComputedLeft() + element.getComputedW());
                    cb = Math.min(cb, element.getComputedTop() + element.getComputedH());
                }
                ElementInstance childHit = hoverTestElements(
                        element.getChildren(), mouseX, mouseY, cl, ct, cr, cb);
                if (childHit != null)
                    return childHit;
            }

            if (element.isClickExpanded() && element.hasClickStateChildren()) {
                ElementInstance childHit = hoverTestElements(
                        element.getClickStateChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom);
                if (childHit != null)
                    return childHit;
            }

            if (element.hasActiveHoverState()) {
                ElementInstance activeRoot = resolveActiveHoverRoot(element);
                if (activeRoot != null) {
                    if (isHit(activeRoot, mouseX, mouseY)
                            && mouseX >= clipLeft && mouseX <= clipRight
                            && mouseY >= clipTop && mouseY <= clipBottom)
                        return element;
                    ElementInstance childHit = hoverTestElements(
                            activeRoot.getChildren(), mouseX, mouseY,
                            clipLeft, clipTop, clipRight, clipBottom);
                    if (childHit != null)
                        return element;
                }
                ObjectArrayList<ElementInstance> activeChildren = resolveActiveHoverChildren(element);
                if (activeChildren != null && !activeChildren.isEmpty()) {
                    ElementInstance childHit = hoverTestElements(
                            activeChildren, mouseX, mouseY,
                            clipLeft, clipTop, clipRight, clipBottom);
                    if (childHit != null)
                        return element;
                }
            }

            if (!element.isHoverable())
                continue;
            if (mouseX < clipLeft || mouseX > clipRight || mouseY < clipTop || mouseY > clipBottom)
                continue;
            if (isHit(element, mouseX, mouseY))
                return element;
        }

        return null;
    }

    // Hit Test (click) \\

    private boolean hitTestElements(
            ObjectArrayList<ElementInstance> elements,
            float mouseX, float mouseY,
            float clipLeft, float clipTop,
            float clipRight, float clipBottom,
            MenuInstance menu) {

        for (int i = elements.size() - 1; i >= 0; i--) {

            ElementInstance element = elements.get(i);

            if (element.hasChildren()) {
                float cl = clipLeft, ct = clipTop, cr = clipRight, cb = clipBottom;
                if (element.getElementData().isMask()) {
                    cl = Math.max(cl, element.getComputedLeft());
                    ct = Math.max(ct, element.getComputedTop());
                    cr = Math.min(cr, element.getComputedLeft() + element.getComputedW());
                    cb = Math.min(cb, element.getComputedTop() + element.getComputedH());
                }
                if (hitTestElements(element.getChildren(), mouseX, mouseY,
                        cl, ct, cr, cb, menu))
                    return true;
            }

            if (element.isClickExpanded() && element.hasClickStateChildren()) {
                if (hitTestElements(element.getClickStateChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom, menu))
                    return true;
            }

            if (element.hasActiveHoverState()) {
                ElementInstance activeRoot = resolveActiveHoverRoot(element);
                if (activeRoot != null) {
                    if (hitTestElements(activeRoot.getChildren(), mouseX, mouseY,
                            clipLeft, clipTop, clipRight, clipBottom, menu))
                        return true;
                }
                ObjectArrayList<ElementInstance> activeChildren = resolveActiveHoverChildren(element);
                if (activeChildren != null && !activeChildren.isEmpty()) {
                    if (hitTestElements(activeChildren, mouseX, mouseY,
                            clipLeft, clipTop, clipRight, clipBottom, menu))
                        return true;
                }
            }

            if (!element.hasClickState() && !element.hasAction())
                continue;
            if (mouseX < clipLeft || mouseX > clipRight || mouseY < clipTop || mouseY > clipBottom)
                continue;
            if (!isHit(element, mouseX, mouseY))
                continue;

            if (element.hasClickState() && element.hasClickStateChildren()) {
                toggleClickState(element, menu.getWindow());
                return true;
            }

            executeCallback(
                    element.getEffectiveActionClass(),
                    element.getEffectiveActionMethod(),
                    element.getEffectiveActionArg(),
                    menu, menu.getWindow(), element);
            return true;
        }

        return false;
    }

    // Active Hover State Helpers \\

    private ElementInstance resolveActiveHoverRoot(ElementInstance element) {
        ElementStateStruct active = element.getActiveHoverState();
        if (active == null || !active.hasMaster())
            return null;
        ElementHandle handle = element.getHandle();
        if (active == handle.getHoverEnterState())
            return element.getHoverEnterStateRoot();
        if (active == handle.getHoverState())
            return element.getHoverStateRoot();
        if (active == handle.getHoverExitState())
            return element.getHoverExitStateRoot();
        return null;
    }

    private ObjectArrayList<ElementInstance> resolveActiveHoverChildren(ElementInstance element) {
        ElementStateStruct active = element.getActiveHoverState();
        if (active == null)
            return null;
        ElementHandle handle = element.getHandle();
        if (active == handle.getHoverEnterState())
            return element.getHoverEnterStateChildren();
        if (active == handle.getHoverState())
            return element.getHoverStateChildren();
        if (active == handle.getHoverExitState())
            return element.getHoverExitStateChildren();
        return null;
    }

    // Toggle \\

    private void toggleClickState(ElementInstance element, WindowInstance window) {
        if (openClickState != null && openClickState != element)
            collapseClickState();
        boolean expanding = !element.isClickExpanded();
        element.setClickExpanded(expanding);
        openClickState = expanding ? element : null;
        openClickStateWindow = expanding ? window : null;
    }

    private void collapseClickState() {
        openClickState.setClickExpanded(false);
        openClickState = null;
        openClickStateWindow = null;
    }

    // Collapse \\

    private void checkClickStateCollapse(ObjectArrayList<WindowInstance> hoveredWindows) {

        if (openClickState == null)
            return;

        // Collapse immediately if the window owning the dropdown is no longer hovered
        if (!hoveredWindows.contains(openClickStateWindow)) {
            collapseClickState();
            return;
        }

        float mouseX = inputManager.getHoverMouseX(openClickStateWindow);
        float mouseY = inputManager.getHoverMouseY(openClickStateWindow);

        float left = openClickState.getComputedLeft() - collapseTolerance;
        float right = openClickState.getComputedLeft() + openClickState.getComputedW() + collapseTolerance;
        float top = openClickState.getComputedTop() - openClickState.getClickStateContentH() - collapseTolerance;
        float bottom = openClickState.getComputedTop() + openClickState.getComputedH() + collapseTolerance;

        if (mouseX < left || mouseX > right || mouseY < top || mouseY > bottom)
            collapseClickState();
    }

    // Callbacks \\

    /*
     * Dispatches a class#method callback with whatever context is available
     * at the call site. Resolution is cached per (class, method) — not per
     * argument value, since which method gets called never depends on the
     * runtime arg — so a dynamically injected arg that takes on many
     * distinct values over a session (e.g. a layout name) never grows the
     * cache; only the fixed, finite set of distinct callback methods does.
     */
    private void executeCallback(
            String actionClass, String actionMethod, String actionArg,
            MenuInstance menu, WindowInstance window, ElementInstance element) {

        if (actionClass == null || actionMethod == null)
            return;

        String key = actionClass + "#" + actionMethod;
        ResolvedCallback callback = resolvedCallbacks.get(key);

        if (callback == null) {
            callback = resolveCallback(actionClass, actionMethod);
            resolvedCallbacks.put(key, callback);
        }

        invoke(callback, actionArg, menu, window, element);
    }

    private ResolvedCallback resolveCallback(String actionClass, String actionMethod) {
        try {
            Class<?> clazz = Class.forName(actionClass);
            Object target = internal.getUnchecked(clazz);

            if (target == null)
                return throwException("Callback class not registered: '" + actionClass + "'");

            Method method = findCallbackMethod(target.getClass(), actionMethod);

            if (method == null)
                return throwException("No compatible method '" + actionMethod + "' on '" + actionClass
                        + "' — every parameter must be one of String, MenuInstance, WindowInstance, ElementInstance.");

            return new ResolvedCallback(target, method);

        } catch (ClassNotFoundException e) {
            return throwException("Callback class not found: " + actionClass, e);
        }
    }

    /*
     * Finds the named public method whose parameters are drawn entirely
     * from the supported context types, in any order and any subset. This
     * is what lets a callback declare exactly the context it needs — a
     * window, an element, a menu, a literal arg, any combination, or
     * nothing at all — without any per-call-site special casing: every
     * on_click, on_drag, and hover-state callback in the engine resolves
     * through this exact same lookup. Overloading a callback method name
     * with two different supported signatures on the same class is not
     * supported — use distinct method names instead, same as every
     * existing callback in this codebase already does.
     */
    private Method findCallbackMethod(Class<?> targetClass, String methodName) {
        for (Method m : targetClass.getMethods()) {
            if (!m.getName().equals(methodName))
                continue;
            if (isFullySupported(m.getParameterTypes()))
                return m;
        }
        return null;
    }

    private boolean isFullySupported(Class<?>[] paramTypes) {
        for (Class<?> type : paramTypes)
            if (type != String.class && type != MenuInstance.class
                    && type != WindowInstance.class && type != ElementInstance.class)
                return false;
        return true;
    }

    private void invoke(ResolvedCallback callback, String arg,
            MenuInstance menu, WindowInstance window, ElementInstance element) {

        Class<?>[] paramTypes = callback.method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> type = paramTypes[i];
            if (type == String.class)
                args[i] = arg;
            else if (type == MenuInstance.class)
                args[i] = menu;
            else if (type == WindowInstance.class)
                args[i] = window;
            else
                args[i] = element;
        }

        try {
            callback.method.invoke(callback.target, args);
        } catch (Exception e) {
            throwException("Callback failed: " + callback.method.getName(), e);
        }
    }

    private static final class ResolvedCallback {

        private final Object target;
        private final Method method;

        ResolvedCallback(Object target, Method method) {
            this.target = target;
            this.method = method;
        }
    }

    // Util \\

    private boolean isHit(ElementInstance element, float mouseX, float mouseY) {
        float left = element.getComputedLeft();
        float top = element.getComputedTop();
        float right = left + element.getComputedW();
        float bottom = top + element.getComputedH();
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }
}