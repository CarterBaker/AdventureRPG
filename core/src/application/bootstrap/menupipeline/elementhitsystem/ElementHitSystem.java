package application.bootstrap.menupipeline.elementhitsystem;

import java.lang.reflect.Method;

import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementStateStruct;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.util.MenuAwareAction;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementHitSystem extends SystemPackage {

    /*
     * Hover lifecycle:
     * Enter — if hoverEnterState defined: set as activeHoverState, fire callback.
     * Immediately transitions to hoverState if also defined.
     * Per frame — if hoverState defined: set as activeHoverState, fire callback.
     * Exit — if hoverExitState defined: set as activeHoverState, fire callback.
     * If NOT defined: activeHoverState is left as-is. Nothing reverts implicitly.
     * resetPressed — hard reset, clears activeHoverState unconditionally.
     *
     * Hover state root/children belong to the parent element for hover purposes.
     * When the mouse is anywhere within the active hover state root or its
     * children,
     * the parent element remains hoveredElement. The expanded region is part of the
     * parent's hit surface and does not trigger exit.
     *
     * Clicks on state root children are dispatched independently by hitTestElements
     * regardless of hoveredElement.
     *
     * on_drag fires each frame while primary held on hoveredElement.
     * on_click fires once on primary press.
     */

    private static final String PARENT_ARG = "$parent";

    private WindowManager windowManager;
    private InputManager inputManager;

    private ElementInstance hoveredElement;
    private ElementInstance openClickState;
    private WindowInstance hoveredElementWindow;
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
        this.windowManager = get(WindowManager.class);
        this.inputManager = get(InputManager.class);
    }

    // Entry Point \\

    public void updateRaycast(ObjectArrayList<MenuInstance> activeMenus) {

        WindowInstance hoveredWindow = windowManager.getHoveredWindow();

        if (hoveredWindow == null)
            return;

        int rayWindowID = hoveredWindow.getWindowID();
        float mouseX = inputManager.getHoverMouseX(hoveredWindow);
        float mouseY = inputManager.getHoverMouseY(hoveredWindow);

        updateHover(activeMenus, mouseX, mouseY, rayWindowID);
        fireOnHoverPerFrame();
        checkClickStateCollapse(mouseX, mouseY);

        if (hoveredElement != null && hoveredElement.hasOnDrag()
                && inputManager.bindingHeld(KeyBindings.PRIMARY, hoveredWindow))
            executeCallback(
                    hoveredElement.getEffectiveOnDragClass(),
                    hoveredElement.getEffectiveOnDragMethod(),
                    hoveredElement.getEffectiveOnDragArg(),
                    null);

        if (!inputManager.bindingClicked(KeyBindings.PRIMARY, hoveredWindow))
            return;

        for (int i = activeMenus.size() - 1; i >= 0; i--) {

            MenuInstance instance = activeMenus.get(i);

            if (!instance.isVisible())
                continue;

            WindowInstance window = instance.getWindow();

            if (window.getWindowID() != rayWindowID)
                continue;

            if (hitTestElements(instance.getElements(), mouseX, mouseY,
                    0, 0, window.getWidth(), window.getHeight(), instance))
                return;
        }
    }

    public void resetPressed() {
        this.openClickState = null;
        clearHover();
    }

    // Hover \\

    private void updateHover(
            ObjectArrayList<MenuInstance> activeMenus,
            float mouseX,
            float mouseY,
            int rayWindowID) {

        ElementInstance next = null;

        for (int i = activeMenus.size() - 1; i >= 0; i--) {

            MenuInstance instance = activeMenus.get(i);

            if (!instance.isVisible())
                continue;

            if (instance.getWindow().getWindowID() != rayWindowID)
                continue;

            WindowInstance window = instance.getWindow();

            next = hoverTestElements(instance.getElements(), mouseX, mouseY,
                    0, 0, window.getWidth(), window.getHeight());

            if (next != null)
                break;
        }

        if (next == hoveredElement)
            return;

        // Exit previous
        if (hoveredElement != null) {

            ElementStateStruct exitState = hoveredElement.getHandle().getHoverExitState();

            if (exitState != null) {
                hoveredElement.setActiveHoverState(exitState);
                if (exitState.hasAction())
                    executeCallback(exitState.getActionClass(), exitState.getActionMethod(),
                            exitState.getActionArg(), null);
            }

            hoveredElement.setHovered(false);
            hoveredElement = null;
            hoveredElementWindow = null; // ← add
            windowManager.unlockHoveredWindow();
        }

        if (next == null)
            return;

        // Enter next
        hoveredElement = next;
        hoveredElement.setHovered(true);
        hoveredElementWindow = windowManager.getHoveredWindow(); // ← add
        windowManager.lockHoveredWindow();

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
        windowManager.unlockHoveredWindow();
    }

    // Called by MenuManager each frame before the raycast guard. Fires hover
    // exit for the current hoveredElement when the hovered window has changed
    // so exits always fire on window transitions even when the incoming window
    // has no raycast-locked menus.
    public void clearHoverIfWindowChanged(WindowInstance currentHoveredWindow) {

        if (hoveredElement == null)
            return;

        if (hoveredElementWindow == currentHoveredWindow)
            return;

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
        windowManager.unlockHoveredWindow();
    }

    // Hover Test \\

    private ElementInstance hoverTestElements(
            ObjectArrayList<ElementInstance> elements,
            float mouseX, float mouseY,
            float clipLeft, float clipTop,
            float clipRight, float clipBottom) {

        for (int i = elements.size() - 1; i >= 0; i--) {

            ElementInstance element = elements.get(i);

            // Default children — tested before the element itself
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

            // Click state children
            if (element.isClickExpanded() && element.hasClickStateChildren()) {
                ElementInstance childHit = hoverTestElements(
                        element.getClickStateChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom);
                if (childHit != null)
                    return childHit;
            }

            /*
             * Active hover state region — the parent owns this entire region.
             * Any hit within the state root or its children returns the parent,
             * keeping hoveredElement stable across the full expanded visual area.
             */
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

            // State root children are independently clickable regardless of hoveredElement.
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

            boolean interactive = element.hasClickState() || element.hasAction();

            if (!interactive)
                continue;

            if (mouseX < clipLeft || mouseX > clipRight || mouseY < clipTop || mouseY > clipBottom)
                continue;

            if (!isHit(element, mouseX, mouseY))
                continue;

            if (element.hasClickState() && element.hasClickStateChildren()) {
                toggleClickState(element);
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

    private void toggleClickState(ElementInstance element) {
        if (openClickState != null && openClickState != element)
            collapseClickState();
        boolean expanding = !element.isClickExpanded();
        element.setClickExpanded(expanding);
        openClickState = expanding ? element : null;
    }

    private void collapseClickState() {
        openClickState.setClickExpanded(false);
        openClickState = null;
    }

    // Collapse \\

    private void checkClickStateCollapse(float mouseX, float mouseY) {

        if (openClickState == null)
            return;

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