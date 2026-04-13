package application.bootstrap.menupipeline.raycastsystem;

import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RaycastSystem extends SystemPackage {

    /*
     * Performs mouse hit testing against active menu elements each frame when
     * active. Iterates menus back to front and elements back to front so the
     * topmost visible element wins. Mask ancestors clip the testable region.
     */

    // Internal
    private InputSystem inputSystem;

    // State
    private boolean active;
    private boolean wasPressed;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.inputSystem = get(InputSystem.class);
    }

    // Update \\

    public void update(
            ObjectArrayList<MenuInstance> activeMenus,
            float screenW,
            float screenH) {

        if (!active)
            return;

        boolean pressed = inputSystem.isRawLeftClick();
        boolean clicked = pressed && !wasPressed;
        wasPressed = pressed;

        if (!clicked)
            return;

        float mouseX = inputSystem.getMouseX();
        float mouseY = screenH - inputSystem.getMouseY();

        for (int i = activeMenus.size() - 1; i >= 0; i--) {

            MenuInstance instance = activeMenus.get(i);

            if (!instance.isVisible())
                continue;

            if (hitTestElements(
                    instance.getElements(),
                    mouseX, mouseY,
                    0, 0, screenW, screenH))
                return;
        }
    }

    // Hit Testing \\

    /*
     * clipLeft/Top/Right/Bottom — the visible rect inherited from masked
     * ancestors. Mouse must be inside this rect to hit anything within it.
     * Children are tested before parent buttons so nested buttons win.
     */
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

    // Control \\

    public void setActive(boolean active) {

        this.active = active;

        if (!active)
            wasPressed = false;
    }
}