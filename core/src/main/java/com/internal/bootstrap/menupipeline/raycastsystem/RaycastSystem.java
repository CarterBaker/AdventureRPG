package com.internal.bootstrap.menupipeline.raycastsystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.internal.bootstrap.menupipeline.element.ElementType;
import com.internal.bootstrap.menupipeline.element.MenuElementHandle;
import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.core.engine.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RaycastSystem extends SystemPackage {

    private boolean active = false;
    private boolean wasPressed = false;

    // Update \\

    public void update(ObjectArrayList<MenuInstance> activeMenus, float screenW, float screenH) {

        if (!active)
            return;

        boolean pressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean clicked = pressed && !wasPressed;
        wasPressed = pressed;

        if (!clicked)
            return;

        // Screen coords — LibGDX y is top-down from top-left
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();

        // Walk menus top-most first (last opened = last in list = highest priority)
        for (int i = activeMenus.size() - 1; i >= 0; i--) {
            MenuInstance instance = activeMenus.get(i);
            if (!instance.isVisible())
                continue;

            if (hitTestElements(instance.getHandle().getElements(), mouseX, mouseY))
                return; // consumed
        }
    }

    // Hit Testing \\

    private boolean hitTestElements(
            ObjectArrayList<MenuElementHandle> elements,
            float mouseX, float mouseY) {

        for (MenuElementHandle element : elements) {

            // Children first — they are drawn on top
            if (element.hasChildren())
                if (hitTestElements(element.getChildren(), mouseX, mouseY))
                    return true;

            if (element.getType() != ElementType.BUTTON)
                continue;

            if (!isHit(element, mouseX, mouseY))
                continue;

            if (element.getClickAction() != null)
                element.getClickAction().run();

            return true;
        }

        return false;
    }

    private boolean isHit(MenuElementHandle element, float mouseX, float mouseY) {
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