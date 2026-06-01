package application.bootstrap.menupipeline.elementhitsystem;

import java.lang.reflect.Method;

import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementStateStruct;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menulist.MenuListHandle;
import application.bootstrap.menupipeline.util.MenuAwareAction;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.EngineContext;
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
     * WindowManager has no lock or cursor-refresh dependency here. All OS
     * windows are synced unconditionally every frame by WindowManager before
     * this system runs, so cursor coordinates are always current on every
     * window. hoveredElementWindow is tracked locally to detect cross-window
     * hover changes.
     *
     * Hover lifecycle:
     * Enter — if hoverEnterState defined: set as activeHoverState, fire callback.
     * Immediately transitions to hoverState if also defined.
     * Per frame — if hoverState defined: set as activeHoverState, fire callback.
     * Exit — if hoverExitState defined: set as activeHoverState, fire callback.
     * If NOT defined: activeHoverState is left as-is. Nothing reverts implicitly.
     * resetPressed — hard reset, clears activeHoverState unconditionally.
     *
     * Hover root/children belong to the parent element for hover purposes.
     * When the mouse is anywhere within the active hover state root or its
     * children, the parent element remains hoveredElement.
     *
     * on_drag latches on the frame primary is first pressed over an element that
     * has on_drag defined. While latched the callback fires every frame the primary
     * button is physically held — queried directly from EngineContext.input so
     * window
     * focus, hover state, and window boundaries cannot interrupt the gesture. All
     * hover and click machinery is bypassed for the duration. The latch releases
     * the frame the button is no longer held. on_click fires once on primary press
     * when no drag is latching.
     *
     * openClickState — the currently expanded dropdown element, paired with
     * openClickStateWindow to track which window owns it. The collapse check
     * fires per frame using that window's cursor coordinates, and also collapses
     * immediately if that window is no longer in hoveredWindows.
     */

    private static final String PARENT_ARG = "$parent";

    private InputManager inputManager;

    private ElementInstance hoveredElement;
    private ElementInstance draggedElement;
    private ElementInstance openClickState;
    private WindowInstance hoveredElementWindow;
    private WindowInstance openClickStateWindow;
    private float collapseTolerance;

    private Object2ObjectOpenHashMap<String, Runnable> resolvedActions;
    private Object2ObjectOpenHashMap<String, MenuAwareAction> resolvedMenuAwareActions;

    @Override
    protected void create() {
        this.collapseTolerance = EngineSetting.DROPDOWN_COLLAPSE_TOLERANCE;
        this.resolvedActions = new Object2ObjectOpenHashMap<>();
        this.resolvedMenuAwareActions = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
    }

    // Entry Point \\

    public void updateRaycast(ObjectArrayList<WindowInstance> hoveredWindows) {

        // Drag is fully independent of hover and window focus. Query the raw
        // button state directly so window boundaries cannot interrupt the gesture.
        if (draggedElement != null) {
            if (EngineContext.input.isMouseDown(0)) {
                executeCallback(
                        draggedElement.getEffectiveOnDragClass(),
                        draggedElement.getEffectiveOnDragMethod(),
                        draggedElement.getEffectiveOnDragArg(),
                        null);
                return;
            }
            draggedElement = null;
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

        // Latch drag on the frame primary is first pressed over a draggable element.
        if (hoveredElement != null && hoveredElement.hasOnDrag()) {
            draggedElement = hoveredElement;
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
                        enterState.getActionArg(), null);
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
                        exitState.getActionArg(), null);
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
                    hoverState.getActionArg(), null);
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
                    menu);
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

    private void executeCallback(String actionClass, String actionMethod,
            String actionArg, MenuInstance menu) {

        if (actionClass == null || actionMethod == null)
            return;

        String key = actionClass + "#" + actionMethod + "#" + actionArg;

        if (PARENT_ARG.equals(actionArg)) {
            MenuAwareAction action = resolvedMenuAwareActions.get(key);
            if (action == null) {
                action = resolveMenuAwareAction(actionClass, actionMethod);
                resolvedMenuAwareActions.put(key, action);
            }
            action.execute(menu);
            return;
        }

        Runnable action = resolvedActions.get(key);
        if (action == null) {
            action = resolveRunnableAction(actionClass, actionMethod, actionArg);
            resolvedActions.put(key, action);
        }
        action.run();
    }

    private Runnable resolveRunnableAction(String actionClass, String actionMethod,
            String actionArg) {
        try {
            Class<?> clazz = Class.forName(actionClass);
            Object target = internal.getUnchecked(clazz);
            if (target == null)
                throwException("Callback class not registered: '" + actionClass + "'");
            Method method = actionArg != null
                    ? target.getClass().getMethod(actionMethod, String.class)
                    : target.getClass().getMethod(actionMethod);
            if (actionArg != null)
                return () -> invoke(method, target, actionArg);
            return () -> invoke(method, target);
        } catch (Exception e) {
            throwException("Failed to resolve callback: " + actionClass + "#" + actionMethod, e);
            return null;
        }
    }

    private MenuAwareAction resolveMenuAwareAction(String actionClass, String actionMethod) {
        try {
            Class<?> clazz = Class.forName(actionClass);
            Object target = internal.getUnchecked(clazz);
            if (target == null)
                throwException("Callback class not registered: '" + actionClass + "'");
            Method method = target.getClass().getMethod(actionMethod, MenuInstance.class);
            return p -> invoke(method, target, p);
        } catch (Exception e) {
            throwException("Failed to resolve menu-aware callback: "
                    + actionClass + "#" + actionMethod, e);
            return null;
        }
    }

    private void invoke(Method method, Object target, Object... args) {
        try {
            method.invoke(target, args);
        } catch (Exception e) {
            throwException("Callback failed: " + method.getName(), e);
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