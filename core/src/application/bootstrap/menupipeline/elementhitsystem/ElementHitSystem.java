package application.bootstrap.menupipeline.elementhitsystem;

import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.ScreenRayStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementHitSystem extends SystemPackage {

    /*
     * Handles per-frame raycast hit testing against the active menu stack.
     * Resolves the target window from the screen ray, performs click detection,
     * and walks the element tree to find and execute the first hit button or
     * toggle the first hit expandable container.
     *
     * Expandable collapse contract:
     * - Only one expandable may be open at a time. Opening a second collapses
     * the first immediately.
     * - Each frame the mouse position is checked against the open expandable's
     * combined header and dropdown bounds, expanded by DROPDOWN_COLLAPSE_TOLERANCE
     * on all sides. If the mouse leaves that region the expandable collapses.
     */

    // Internal
    private RaycastManager raycastManager;
    private InputSystem inputSystem;

    // Raycast State
    private boolean wasPressed;

    // Expandable State
    private ElementInstance openExpandable;
    private float collapseTolerance;

    // Internal \\

    @Override
    protected void create() {
        this.collapseTolerance = EngineSetting.DROPDOWN_COLLAPSE_TOLERANCE;
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

        WindowInstance rayWindow = null;

        for (int i = 0; i < activeMenus.size(); i++) {
            WindowInstance candidate = activeMenus.get(i).getWindow();

            if (candidate.getWindowID() != rayWindowID)
                continue;

            rayWindow = candidate;
            break;
        }

        if (rayWindow == null)
            return;

        float mouseX = ray.getScreenX();
        float adjustedMouseY = rayWindow.getHeight() - ray.getScreenY();

        checkDropdownCollapse(mouseX, adjustedMouseY);

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

            float screenW = window.getWidth();
            float screenH = window.getHeight();

            if (hitTestElements(
                    instance.getElements(),
                    mouseX, adjustedMouseY,
                    0, 0, screenW, screenH))
                return;
        }
    }

    public void resetPressed() {
        this.wasPressed = false;
        this.openExpandable = null;
    }

    // Dropdown Collapse \\

    private void checkDropdownCollapse(float mouseX, float mouseY) {

        if (openExpandable == null)
            return;

        float left = openExpandable.getComputedLeft() - collapseTolerance;
        float right = openExpandable.getComputedLeft() + openExpandable.getComputedW() + collapseTolerance;
        float bottom = openExpandable.getComputedTop() + openExpandable.getComputedH() + collapseTolerance;
        float top = openExpandable.getComputedTop() - openExpandable.getContentH() - collapseTolerance;

        if (mouseX < left || mouseX > right || mouseY < top || mouseY > bottom)
            collapseOpen();
    }

    private void collapseOpen() {
        openExpandable.toggleExpanded();
        openExpandable = null;
    }

    // Hit Testing \\

    private boolean hitTestElements(
            ObjectArrayList<ElementInstance> elements,
            float mouseX, float mouseY,
            float clipLeft, float clipTop,
            float clipRight, float clipBottom) {

        for (int i = elements.size() - 1; i >= 0; i--) {

            ElementInstance element = elements.get(i);
            ElementData data = element.getElementData();
            ElementType type = data.getType();

            if (element.hasChildren()) {

                boolean traverseChildren = type != ElementType.EXPANDABLE_CONTAINER
                        || element.isExpanded();

                if (traverseChildren) {

                    float cl = clipLeft;
                    float ct = clipTop;
                    float cr = clipRight;
                    float cb = clipBottom;

                    if (data.isMask()) {
                        cl = Math.max(cl, element.getComputedLeft());
                        ct = Math.max(ct, element.getComputedTop());
                        cr = Math.min(cr, element.getComputedLeft() + element.getComputedW());
                        cb = Math.min(cb, element.getComputedTop() + element.getComputedH());
                    }

                    if (hitTestElements(element.getChildren(), mouseX, mouseY, cl, ct, cr, cb))
                        return true;
                }
            }

            if (type != ElementType.BUTTON && type != ElementType.EXPANDABLE_CONTAINER)
                continue;

            if (mouseX < clipLeft || mouseX > clipRight
                    || mouseY < clipTop || mouseY > clipBottom)
                continue;

            if (!isHit(element, mouseX, mouseY))
                continue;

            if (type == ElementType.EXPANDABLE_CONTAINER) {
                toggleExpandable(element);
                return true;
            }

            element.execute();
            return true;
        }

        return false;
    }

    private void toggleExpandable(ElementInstance element) {

        if (openExpandable != null && openExpandable != element)
            collapseOpen();

        element.toggleExpanded();

        openExpandable = element.isExpanded() ? element : null;
    }

    private boolean isHit(ElementInstance element, float mouseX, float mouseY) {

        float left = element.getComputedLeft();
        float top = element.getComputedTop();
        float right = left + element.getComputedW();
        float bottom = top + element.getComputedH();

        return mouseX >= left && mouseX <= right
                && mouseY >= top && mouseY <= bottom;
    }
}