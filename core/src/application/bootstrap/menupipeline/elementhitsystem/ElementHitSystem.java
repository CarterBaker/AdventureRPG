package application.bootstrap.menupipeline.elementhitsystem;

import java.lang.reflect.Method;

import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementInstance;
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
     * is within its own bounds OR within its hover state children's bounds (inline)
     * OR within its hoverStateRoot's children's bounds (master-based overlay).
     *
     * For master-based hover overlays (hasHoverStateRoot), hoverTestElements always
     * tests the root's children unconditionally — this bootstraps hover on first
     * entry without a chicken-and-egg dependency on isHovered(). hitTestElements
     * gates on isHovered() so buttons inside the panel are only clickable when the
     * panel is actually visible.
     *
     * For inline hover children (hasHoverStateChildren, no root), the owning
     * element must already be hovered before its children are tested in both
     * systems. The anchor mechanism keeps the parent marked hovered while the
     * cursor is over a child.
     *
     * Two tracking fields manage anchor state:
     * hoveredElement — the innermost element directly under the cursor.
     * hoverAnchor — the parent whose hover region contains the cursor; remains
     * hovered so it continues to render its hover state.
     *
     * hoverAnchorCapture is a scratch field valid only during a single
     * hoverTestElements traversal.
     *
     * Click state expansion and collapse fire on press-release edge.
     * Action resolution is cached on first call.
     *
     * Interactivity is capability-driven — any element with a hover state, click
     * state, or action participates in hit testing regardless of element type.
     */

    private static final String PARENT_ARG = "$parent";

    // Internal
    private RaycastManager raycastManager;
    private InputSystem inputSystem;

    // State
    private boolean wasPressed;
    private ElementInstance hoveredElement;
    private ElementInstance hoverAnchor;
    private ElementInstance hoverAnchorCapture;
    private ElementInstance openClickState;
    private float collapseTolerance;

    // Action Cache
    private Object2ObjectOpenHashMap<String, Runnable> resolvedActions;
    private Object2ObjectOpenHashMap<String, MenuAwareAction> resolvedMenuAwareActions;

    // Internal \\

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

    // Entry Point \\

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

        if (next == hoveredElement && hoverAnchorCapture == hoverAnchor)
            return;

        clearHover();

        if (next == null)
            return;

        next.setHovered(true);
        hoveredElement = next;

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

            // Master-based hover overlay — always test unconditionally to bootstrap
            // hover on first entry; isHovered() gate lives in hitTestElements only
            if (element.hasHoverStateRoot()) {

                ElementInstance prevCapture = hoverAnchorCapture;
                hoverAnchorCapture = element;

                ElementInstance childHit = hoverTestElements(
                        element.getHoverStateRoot().getChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom);

                if (childHit != null) {
                    hoverAnchorCapture = element;
                    return childHit;
                }

                hoverAnchorCapture = prevCapture;

                // Inline hover children — owning element must already be hovered
            } else if (element.isHovered() && element.hasHoverStateChildren()) {

                ElementInstance prevCapture = hoverAnchorCapture;
                hoverAnchorCapture = element;

                ElementInstance childHit = hoverTestElements(
                        element.getHoverStateChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom);

                if (childHit != null) {
                    hoverAnchorCapture = element;
                    return childHit;
                }

                hoverAnchorCapture = prevCapture;
            }

            // Element itself — capability-driven, type is irrelevant
            boolean hoverable = element.hasHoverState() || element.hasClickState() || data.hasAction();

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

            // Master-based hover overlay — gated on isHovered() so buttons are only
            // clickable when the panel is actually visible
            if (element.isHovered() && element.hasHoverStateRoot()) {

                if (hitTestElements(element.getHoverStateRoot().getChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom, menu))
                    return true;

                // Inline hover children — same gate, consistent with master-based path
            } else if (element.isHovered() && element.hasHoverStateChildren()) {

                if (hitTestElements(element.getHoverStateChildren(), mouseX, mouseY,
                        clipLeft, clipTop, clipRight, clipBottom, menu))
                    return true;
            }

            // Element itself — capability-driven, type is irrelevant
            boolean interactive = element.hasClickState() || data.hasAction();

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