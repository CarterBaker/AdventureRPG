package application.bootstrap.menupipeline.elementhitsystem;

import java.lang.reflect.Method;

import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.util.MenuAwareAction;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.ScreenRayStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementHitSystem extends SystemPackage {

    /*
     * Handles all interactive element behavior per frame.
     *
     * Hover state is updated every raycast. An element is hovered when the mouse
     * is within its own bounds OR within any of its hover state children's bounds.
     * This prevents flicker when the hover state visually expands the element (e.g.
     * a toolbar button whose hover state shows a dropdown below it — moving the
     * mouse into the dropdown must keep the button hovered).
     *
     * Two tracking fields manage this:
     * hoveredElement — the innermost element directly under the cursor; this
     * element has isHovered() == true for its own rendering.
     * hoverAnchor — set when hoveredElement is inside the hover state children
     * of another element; that parent also has isHovered() == true
     * so it continues to render its hover state (keeps dropdown open).
     *
     * hoverAnchorCapture is a scratch field valid only during a single
     * hoverTestElements traversal. It is set when a hit is found inside hover state
     * children so updateHover can read the parent after traversal completes.
     *
     * Click state expansion and collapse fire on press-release edge.
     * Action resolution is cached on first call.
     */

    private static final String PARENT_ARG = "$parent";

    // Internal
    private RaycastManager raycastManager;
    private InputSystem inputSystem;

    // State
    private boolean wasPressed;
    private ElementInstance hoveredElement;
    private ElementInstance hoverAnchor;
    private ElementInstance hoverAnchorCapture; // scratch, valid only during hoverTestElements
    private ElementInstance openClickState;
    private float collapseTolerance;

    // Action Cache
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
        this.raycastManager = get(RaycastManager.class);
        this.inputSystem = get(InputSystem.class);
    }

    public void updateRaycast(ObjectArrayList<MenuInstance> activeMenus) {

        if (!raycastManager.hasScreenRay())
            return;

        ScreenRayStruct ray = raycastManager.getScreenRay();
        int rayWindowID = ray.getWindowID();

        WindowInstance rayWindow = resolveRayWindow(activeMenus, rayWindowID);

        if (rayWindow == null)
            return;

        float mouseX = ray.getScreenX();
        float mouseY = rayWindow.getHeight() - ray.getScreenY();

        updateHover(activeMenus, mouseX, mouseY, rayWindowID);
        checkClickStateCollapse(mouseX, mouseY);

        boolean pressed = inputSystem.bindingClicked(KeyBindings.PRIMARY);
        boolean clicked = pressed && !wasPressed;
        wasPressed = pressed;

        if (!clicked)
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
        this.wasPressed = false;
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
        hoverAnchorCapture = null;

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

        // Short-circuit if nothing changed (element and anchor are identical)
        if (next == hoveredElement && hoverAnchorCapture == hoverAnchor)
            return;

        clearHover();

        if (next == null)
            return;

        next.setHovered(true);
        hoveredElement = next;

        // If the hit was inside someone else's hover state children, mark that parent
        // as hovered too so it continues to render its hover state
        if (hoverAnchorCapture != null && hoverAnchorCapture != next) {
            hoverAnchorCapture.setHovered(true);
            hoverAnchor = hoverAnchorCapture;
        }
    }

    private ElementInstance hoverTestElements(
            ObjectArrayList<ElementInstance> elements,
            float mouseX,
            float mouseY,
            float clipLeft,
            float clipTop,
            float clipRight,
            float clipBottom) {

        for (int i = elements.size() - 1; i >= 0; i--) {

            ElementInstance element = elements.get(i);
            ElementData data = element.getElementData();

            // Default children
            if (element.hasChildren()) {

                float cl = clipLeft, ct = clipTop, cr = clipRight, cb = clipBottom;

                if (data.isMask()) {
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

            // Click state children — visible and interactive when click-expanded
            if (element.isClickExpanded() && element.hasClickStateChildren()) {

                ElementInstance childHit = hoverTestElements(
                        element.getClickStateChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom);

                if (childHit != null)
                    return childHit;
            }

            // Hover state children — only visible when this element is currently hovered.
            // If the mouse is over any of them, they become the active hovered element
            // while this element remains hovered as the anchor (keeps the hover zone open).
            if (element.isHovered() && element.hasHoverStateChildren()) {

                ElementInstance prevCapture = hoverAnchorCapture;
                hoverAnchorCapture = element;

                ElementInstance childHit = hoverTestElements(
                        element.getHoverStateChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom);

                if (childHit != null) {
                    // Preserve element as the anchor at this level — if there was deeper
                    // nesting the inner call may have overwritten capture, so restore it
                    hoverAnchorCapture = element;
                    return childHit;
                }

                hoverAnchorCapture = prevCapture;
            }

            // Element itself
            boolean hoverable = data.getType() == ElementType.BUTTON
                    || element.hasHoverState();

            if (!hoverable)
                continue;

            if (mouseX < clipLeft || mouseX > clipRight || mouseY < clipTop || mouseY > clipBottom)
                continue;

            if (isHit(element, mouseX, mouseY))
                return element;
        }

        return null;
    }

    private void clearHover() {

        if (hoveredElement != null) {
            hoveredElement.setHovered(false);
            hoveredElement = null;
        }

        if (hoverAnchor != null) {
            hoverAnchor.setHovered(false);
            hoverAnchor = null;
        }
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

    private void collapseClickState() {
        openClickState.setClickExpanded(false);
        openClickState = null;
    }

    // Hit Testing \\

    private boolean hitTestElements(
            ObjectArrayList<ElementInstance> elements,
            float mouseX,
            float mouseY,
            float clipLeft,
            float clipTop,
            float clipRight,
            float clipBottom,
            MenuInstance menu) {

        for (int i = elements.size() - 1; i >= 0; i--) {

            ElementInstance element = elements.get(i);
            ElementData data = element.getElementData();

            // Default children
            if (element.hasChildren()) {

                float cl = clipLeft, ct = clipTop, cr = clipRight, cb = clipBottom;

                if (data.isMask()) {
                    cl = Math.max(cl, element.getComputedLeft());
                    ct = Math.max(ct, element.getComputedTop());
                    cr = Math.min(cr, element.getComputedLeft() + element.getComputedW());
                    cb = Math.min(cb, element.getComputedTop() + element.getComputedH());
                }

                if (hitTestElements(element.getChildren(), mouseX, mouseY,
                        cl, ct, cr, cb, menu))
                    return true;
            }

            // Click state children — interactive when click-expanded
            if (element.isClickExpanded() && element.hasClickStateChildren()) {

                if (hitTestElements(element.getClickStateChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom, menu))
                    return true;
            }

            // Hover state children — interactive when this element is hovered (visible)
            if (element.isHovered() && element.hasHoverStateChildren()) {

                if (hitTestElements(element.getHoverStateChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom, menu))
                    return true;
            }

            // Element itself
            boolean interactive = data.getType() == ElementType.BUTTON
                    || element.hasClickState();

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

            executeAction(data, menu);
            return true;
        }

        return false;
    }

    // Toggle \\

    private void toggleClickState(ElementInstance element) {

        if (openClickState != null && openClickState != element)
            collapseClickState();

        boolean expanding = !element.isClickExpanded();
        element.setClickExpanded(expanding);
        openClickState = expanding ? element : null;
    }

    // Actions \\

    private void executeAction(ElementData data, MenuInstance parent) {

        if (!data.hasAction())
            return;

        String key = data.getActionClass() + "#" + data.getActionMethod() + "#" + data.getActionArg();

        if (PARENT_ARG.equals(data.getActionArg())) {

            MenuAwareAction action = resolvedMenuAwareActions.get(key);

            if (action == null) {
                action = resolveMenuAwareAction(data);
                resolvedMenuAwareActions.put(key, action);
            }

            action.execute(parent);
            return;
        }

        Runnable action = resolvedActions.get(key);

        if (action == null) {
            action = resolveRunnableAction(data);
            resolvedActions.put(key, action);
        }

        action.run();
    }

    private Runnable resolveRunnableAction(ElementData data) {

        try {
            Class<?> clazz = Class.forName(data.getActionClass());
            Object target = internal.getUnchecked(clazz);

            if (target == null)
                throwException("on_click class not registered: '" + data.getActionClass() + "'");

            Method method = data.getActionArg() != null
                    ? target.getClass().getMethod(data.getActionMethod(), String.class)
                    : target.getClass().getMethod(data.getActionMethod());
            String arg = data.getActionArg();

            if (arg != null)
                return () -> invoke(method, target, arg);

            return () -> invoke(method, target);
        } catch (Exception e) {
            throwException("Failed to resolve button action: "
                    + data.getActionClass() + "#" + data.getActionMethod(), e);
            return null;
        }
    }

    private MenuAwareAction resolveMenuAwareAction(ElementData data) {

        try {
            Class<?> clazz = Class.forName(data.getActionClass());
            Object target = internal.getUnchecked(clazz);

            if (target == null)
                throwException("on_click class not registered: '" + data.getActionClass() + "'");

            Method method = target.getClass().getMethod(data.getActionMethod(), MenuInstance.class);

            return p -> invoke(method, target, p);
        } catch (Exception e) {
            throwException("Failed to resolve menu-aware action: "
                    + data.getActionClass() + "#" + data.getActionMethod(), e);
            return null;
        }
    }

    private void invoke(Method method, Object target, Object... args) {

        try {
            method.invoke(target, args);
        } catch (Exception e) {
            throwException("Button action failed: " + method.getName(), e);
        }
    }

    // Util \\

    private WindowInstance resolveRayWindow(
            ObjectArrayList<MenuInstance> activeMenus,
            int rayWindowID) {

        for (int i = 0; i < activeMenus.size(); i++) {
            WindowInstance candidate = activeMenus.get(i).getWindow();
            if (candidate.getWindowID() == rayWindowID)
                return candidate;
        }

        return null;
    }

    private boolean isHit(ElementInstance element, float mouseX, float mouseY) {
        float left = element.getComputedLeft();
        float top = element.getComputedTop();
        float right = left + element.getComputedW();
        float bottom = top + element.getComputedH();
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }
}