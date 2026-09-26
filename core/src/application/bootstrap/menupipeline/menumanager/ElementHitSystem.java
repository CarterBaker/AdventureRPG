package application.bootstrap.menupipeline.menumanager;

import java.lang.reflect.Method;

import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementStateStruct;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menulist.MenuListHandle;
import application.bootstrap.menupipeline.util.StackDirection;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.input.Buttons;
import engine.input.Input;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementHitSystem extends SystemPackage {

    /*
     * Hover, click, drag and scroll dispatch across the hovered windows in
     * priority order; the first hit wins. Callbacks name a class and method in
     * menu JSON and are resolved once, then invoked on the instance registered
     * in the clicked window's context, inside that context's crash boundary. A
     * method receives whichever of String, MenuInstance, WindowInstance and
     * ElementInstance it declares.
     */

    // Internal
    private InputManager inputManager;

    // Interaction
    private ElementInstance hoveredElement;
    private ElementInstance pointedElement;
    private ElementInstance draggedElement;
    private ElementInstance openClickState;
    private MenuInstance hoveredElementMenu;
    private WindowInstance hoveredElementWindow;
    private WindowInstance draggedElementWindow;
    private WindowInstance openClickStateWindow;
    private float collapseTolerance;

    // Callbacks
    private Object2ObjectOpenHashMap<String, Object2ObjectOpenHashMap<String, ResolvedCallback>> actionClass2Callbacks;

    // Base \\

    @Override
    protected void create() {
        this.collapseTolerance = EngineSetting.DROPDOWN_COLLAPSE_TOLERANCE;
        this.actionClass2Callbacks = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
    }

    // Entry Point \\

    public void updateRaycast(ObjectArrayList<WindowInstance> hoveredWindows) {

        // A latched drag polls the window it started on, whatever hover or focus do
        if (draggedElement != null) {

            Input rawInput = inputManager.getRawInput(draggedElementWindow);

            if (rawInput.isMouseDown(Buttons.LEFT)) {
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

        if (hoveredWindows.isEmpty()) {
            pointElement(null);
            return;
        }

        updateHover(hoveredWindows);
        fireOnHoverPerFrame();
        checkClickStateCollapse(hoveredWindows);
        updateScroll(hoveredWindows);

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
        ElementInstance nextPointed = null;
        MenuInstance nextMenu = null;
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
                    nextPointed = pointTestElements(
                            menu.getElements(), mx, my, 0, 0, window.getWidth(), window.getHeight());
                    nextMenu = menu;
                    nextWindow = window;
                    break outer;
                }
            }
        }

        pointElement(nextPointed);

        if (nextElement == hoveredElement)
            return;

        if (hoveredElement != null)
            fireHoverExit();

        if (nextElement == null)
            return;

        hoveredElement = nextElement;
        hoveredElementMenu = nextMenu;
        hoveredElementWindow = nextWindow;
        hoveredElement.setHovered(true);

        ElementStateStruct enterState = hoveredElement.getHandle().getHoverEnterState();
        if (enterState != null) {
            hoveredElement.setActiveHoverState(enterState);
            if (enterState.hasAction())
                executeCallback(enterState.getActionClass(), enterState.getActionMethod(),
                        enterState.getActionArg(), hoveredElementMenu, hoveredElementWindow, hoveredElement);
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
                        exitState.getActionArg(), hoveredElementMenu, hoveredElementWindow, hoveredElement);
        }
        hoveredElement.setHovered(false);
        hoveredElement = null;
        hoveredElementMenu = null;
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
                    hoverState.getActionArg(), hoveredElementMenu, hoveredElementWindow, hoveredElement);
    }

    private void clearHover() {
        pointElement(null);
        if (hoveredElement == null)
            return;
        hoveredElement.clearActiveHoverState();
        hoveredElement.setHovered(false);
        hoveredElement = null;
        hoveredElementMenu = null;
        hoveredElementWindow = null;
    }

    public void clearHoverIfWindowChanged(ObjectArrayList<WindowInstance> hoveredWindows) {
        if (hoveredElement == null)
            return;
        if (draggedElement != null)
            return;
        if (hoveredWindows.contains(hoveredElementWindow))
            return;
        pointElement(null);
        fireHoverExit();
    }

    // Scroll \\

    private void updateScroll(ObjectArrayList<WindowInstance> hoveredWindows) {

        for (int i = 0; i < hoveredWindows.size(); i++) {

            WindowInstance window = hoveredWindows.get(i);
            MenuListHandle menuList = window.getMenuListHandle();

            if (!menuList.isRaycastLocked())
                continue;

            float wheel = inputManager.getRawInput(window).getScrollY();

            if (wheel == 0f)
                continue;

            float mx = inputManager.getHoverMouseX(window);
            float my = inputManager.getHoverMouseY(window);

            ObjectArrayList<MenuInstance> menus = menuList.getMenus();

            for (int j = menus.size() - 1; j >= 0; j--) {

                MenuInstance menu = menus.get(j);

                if (!menu.isVisible())
                    continue;

                ElementInstance scrollable = scrollTestElements(
                        menu.getElements(), mx, my, 0, 0, window.getWidth(), window.getHeight());

                if (scrollable != null) {
                    scrollElement(scrollable, wheel * EngineSetting.MENU_SCROLL_PIXELS);
                    return;
                }
            }
        }
    }

    private ElementInstance scrollTestElements(
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
                ElementInstance childHit = scrollTestElements(
                        element.getChildren(), mouseX, mouseY, cl, ct, cr, cb);
                if (childHit != null)
                    return childHit;
            }

            if (!element.getElementData().isScrollable())
                continue;
            if (mouseX < clipLeft || mouseX > clipRight || mouseY < clipTop || mouseY > clipBottom)
                continue;
            if (isHit(element, mouseX, mouseY))
                return element;
        }

        return null;
    }

    private void scrollElement(ElementInstance element, float distance) {

        if (element.getElementData().getStackDirection() == StackDirection.VERTICAL)
            element.setScrollY(element.getScrollY() - distance);
        else
            element.setScrollX(element.getScrollX() - distance);
    }

    // Point \\

    private void pointElement(ElementInstance element) {

        if (element == pointedElement)
            return;

        if (pointedElement != null)
            pointedElement.setPointed(false);

        pointedElement = element;

        if (pointedElement != null)
            pointedElement.setPointed(true);
    }

    private ElementInstance pointTestElements(
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
                ElementInstance childHit = pointTestElements(
                        element.getChildren(), mouseX, mouseY, cl, ct, cr, cb);
                if (childHit != null)
                    return childHit;
            }

            if (element.isClickExpanded() && element.hasClickStateChildren()) {
                ElementInstance childHit = pointTestElements(
                        element.getClickStateChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom);
                if (childHit != null)
                    return childHit;
            }

            if (element.hasActiveHoverState()) {
                ElementInstance activeRoot = resolveActiveHoverRoot(element);
                if (activeRoot != null) {
                    ElementInstance childHit = pointTestElements(
                            activeRoot.getChildren(), mouseX, mouseY,
                            clipLeft, clipTop, clipRight, clipBottom);
                    if (childHit != null)
                        return childHit;
                }
                ObjectArrayList<ElementInstance> activeChildren = resolveActiveHoverChildren(element);
                if (activeChildren != null && !activeChildren.isEmpty()) {
                    ElementInstance childHit = pointTestElements(
                            activeChildren, mouseX, mouseY,
                            clipLeft, clipTop, clipRight, clipBottom);
                    if (childHit != null)
                        return childHit;
                }
            }

            if (!element.getElementData().hasHoverColor())
                continue;
            if (mouseX < clipLeft || mouseX > clipRight || mouseY < clipTop || mouseY > clipBottom)
                continue;
            if (isHit(element, mouseX, mouseY))
                return element;
        }

        return null;
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

    private void executeCallback(
            String actionClass, String actionMethod, String actionArg,
            MenuInstance menu, WindowInstance window, ElementInstance element) {

        if (actionClass == null || actionMethod == null)
            return;

        ResolvedCallback callback = getResolvedCallback(actionClass, actionMethod);
        ContextPackage context = window != null ? window.getContext() : null;
        Object target = internal.getUnchecked(context, callback.type);

        fillArguments(callback, actionArg, menu, window, element);
        internal.isolate(context, () -> invokeMethod(target, callback));
    }

    private ResolvedCallback getResolvedCallback(String actionClass, String actionMethod) {

        Object2ObjectOpenHashMap<String, ResolvedCallback> method2Callback = actionClass2Callbacks.get(actionClass);

        if (method2Callback == null) {
            method2Callback = new Object2ObjectOpenHashMap<>();
            actionClass2Callbacks.put(actionClass, method2Callback);
        }

        ResolvedCallback callback = method2Callback.get(actionMethod);

        if (callback == null) {
            callback = resolveCallback(actionClass, actionMethod);
            method2Callback.put(actionMethod, callback);
        }

        return callback;
    }

    private ResolvedCallback resolveCallback(String actionClass, String actionMethod) {

        try {
            Class<?> type = Class.forName(actionClass);
            Method method = findCallbackMethod(type, actionMethod);

            if (method == null)
                return throwException("No compatible method '" + actionMethod + "' on '" + actionClass
                        + "' — every parameter must be String, MenuInstance, WindowInstance or ElementInstance.");

            return new ResolvedCallback(type, method);
        } catch (ClassNotFoundException e) {
            return throwException("Callback class not found: " + actionClass, e);
        }
    }

    private Method findCallbackMethod(Class<?> targetClass, String methodName) {

        for (Method method : targetClass.getMethods()) {

            if (!method.getName().equals(methodName))
                continue;

            if (isFullySupported(method.getParameterTypes()))
                return method;
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

    private void fillArguments(
            ResolvedCallback callback, String arg,
            MenuInstance menu, WindowInstance window, ElementInstance element) {

        Class<?>[] paramTypes = callback.paramTypes;
        Object[] args = callback.args;

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
    }

    private void invokeMethod(Object target, ResolvedCallback callback) {

        try {
            callback.method.invoke(target, callback.args);
        } catch (Exception e) {
            throwException("Callback failed: " + callback.method.getName(), e);
        }
    }

    private static final class ResolvedCallback {

        private final Class<?> type;
        private final Method method;
        private final Class<?>[] paramTypes;
        private final Object[] args;

        ResolvedCallback(Class<?> type, Method method) {
            this.type = type;
            this.method = method;
            this.paramTypes = method.getParameterTypes();
            this.args = new Object[paramTypes.length];
        }
    }

    // Accessible \\

    public MenuInstance getHoveredMenu() {
        return hoveredElementMenu;
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