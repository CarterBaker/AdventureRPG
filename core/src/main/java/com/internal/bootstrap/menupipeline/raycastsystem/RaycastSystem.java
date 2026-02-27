package com.internal.bootstrap.menupipeline.raycastsystem;

import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.menupipeline.element.ElementInstance;
import com.internal.bootstrap.menupipeline.element.ElementType;
import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.core.engine.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RaycastSystem extends SystemPackage {

    private InputSystem inputSystem;

    private boolean active = false;
    private boolean wasPressed = false;

    // Base \\

    @Override
    protected void get() {
        this.inputSystem = get(InputSystem.class);
    }

    // Update \\

    public void update(ObjectArrayList<MenuInstance> activeMenus, float screenW, float screenH) {

        if (!active)
            return;

        boolean pressed = inputSystem.isRawLeftClick();
        boolean clicked = pressed && !wasPressed;
        wasPressed = pressed;

        if (!clicked)
            return;

        float mouseX = inputSystem.getMouseX();
        float mouseY = screenH - inputSystem.getMouseY(); // input Y is top-down, layout Y is up

        for (int i = activeMenus.size() - 1; i >= 0; i--) {
            MenuInstance instance = activeMenus.get(i);
            if (!instance.isVisible())
                continue;
            if (hitTestElements(instance.getElements(), mouseX, mouseY))
                return;
        }
    }

    // Hit Testing \\

    private boolean hitTestElements(
            ObjectArrayList<ElementInstance> elements,
            float mouseX, float mouseY) {

        for (ElementInstance element : elements) {
            if (element.hasChildren())
                if (hitTestElements(element.getChildren(), mouseX, mouseY))
                    return true;

            if (element.getHandle().getType() != ElementType.BUTTON)
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
        float bottom = element.getComputedTop();
        float right = left + element.getComputedW();
        float top = bottom + element.getComputedH();
        return mouseX >= left && mouseX <= right
                && mouseY >= bottom && mouseY <= top;
    }

    // Control \\

    public void setActive(boolean active) {
        this.active = active;
        if (!active)
            wasPressed = false;
    }
}