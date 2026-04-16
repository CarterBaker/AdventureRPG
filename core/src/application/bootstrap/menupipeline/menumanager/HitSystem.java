package application.bootstrap.menupipeline.menumanager;

import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.ScreenRayStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.SystemPackage;
import engine.util.settings.KeyBindings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class HitSystem extends SystemPackage {

    /*
     * Handles per-frame raycast hit testing against the active menu stack.
     * Resolves the target window from the screen ray, performs click detection,
     * and walks the element tree to find and execute the first hit button.
     */

    // Internal
    private RaycastManager raycastManager;
    private InputSystem inputSystem;

    // Raycast State
    private boolean wasPressed;

    @Override
    protected void get() {

        this.raycastManager = get(RaycastManager.class);
        this.inputSystem = get(InputSystem.class);
    }

    void updateRaycast(ObjectArrayList<MenuInstance> activeMenus) {

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

        boolean pressed = inputSystem.bindingClicked(KeyBindings.PRIMARY);
        boolean clicked = pressed && !wasPressed;
        wasPressed = pressed;

        if (!clicked)
            return;

        float mouseX = ray.getScreenX();
        float adjustedMouseY = rayWindow.getHeight() - ray.getScreenY();

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

    void resetPressed() {
        wasPressed = false;
    }

    // Hit Testing \\

    private boolean hitTestElements(
            ObjectArrayList<ElementInstance> elements,
            float mouseX, float mouseY,
            float clipLeft, float clipTop,
            float clipRight, float clipBottom) {

        for (int i = elements.size() - 1; i >= 0; i--) {

            ElementInstance element = elements.get(i);

            if (element.hasChildren()) {

                float cl = clipLeft;
                float ct = clipTop;
                float cr = clipRight;
                float cb = clipBottom;

                if (element.getElementData().isMask()) {
                    cl = Math.max(cl, element.getComputedLeft());
                    ct = Math.max(ct, element.getComputedTop());
                    cr = Math.min(cr, element.getComputedLeft() + element.getComputedW());
                    cb = Math.min(cb, element.getComputedTop() + element.getComputedH());
                }

                if (hitTestElements(element.getChildren(), mouseX, mouseY, cl, ct, cr, cb))
                    return true;
            }

            if (element.getElementData().getType() != ElementType.BUTTON)
                continue;

            if (mouseX < clipLeft || mouseX > clipRight
                    || mouseY < clipTop || mouseY > clipBottom)
                continue;

            if (!isHit(element, mouseX, mouseY))
                continue;

            element.execute();
            return true;
        }

        return false;
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
