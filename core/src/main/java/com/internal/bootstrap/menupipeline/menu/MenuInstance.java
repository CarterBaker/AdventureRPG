package com.internal.bootstrap.menupipeline.menu;

import com.internal.bootstrap.menupipeline.element.ElementInstance;
import com.internal.core.engine.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuInstance extends InstancePackage {

    private MenuHandle handle;

    // Live element instances — only allocated while this menu is open
    private ObjectArrayList<ElementInstance> elements;

    private boolean visible;

    public void constructor(MenuHandle handle, ObjectArrayList<ElementInstance> elements) {
        this.handle = handle;
        this.elements = elements;
        this.visible = true;
    }

    public void show() {
        this.visible = true;
    }

    public void hide() {
        this.visible = false;
    }

    public MenuHandle getHandle() {
        return handle;
    }

    public ObjectArrayList<ElementInstance> getElements() {
        return elements;
    }

    public boolean isVisible() {
        return visible;
    }
}