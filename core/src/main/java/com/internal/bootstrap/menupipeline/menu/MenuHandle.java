package com.internal.bootstrap.menupipeline.menu;

import com.internal.bootstrap.menupipeline.element.MenuElementHandle;
import com.internal.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuHandle extends HandlePackage {

    private String name;
    private ObjectArrayList<MenuElementHandle> elements;
    private boolean lockInput;
    private boolean raycastInput;

    public void constructor(
            String name,
            ObjectArrayList<MenuElementHandle> elements,
            boolean lockInput,
            boolean raycastInput) {
        this.name = name;
        this.elements = elements;
        this.lockInput = lockInput;
        this.raycastInput = raycastInput;
    }

    public String getName() {
        return name;
    }

    public ObjectArrayList<MenuElementHandle> getElements() {
        return elements;
    }

    public boolean isLockInput() {
        return lockInput;
    }

    public boolean isRaycastInput() {
        return raycastInput;
    }
}