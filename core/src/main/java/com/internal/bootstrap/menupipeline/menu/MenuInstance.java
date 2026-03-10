package com.internal.bootstrap.menupipeline.menu;

import com.internal.bootstrap.menupipeline.element.ElementInstance;
import com.internal.core.engine.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuInstance extends InstancePackage {

    private MenuHandle handle;
    private ObjectArrayList<ElementInstance> elements;
    private boolean visible;

    public void constructor(MenuHandle handle, ObjectArrayList<ElementInstance> elements) {
        this.handle = handle;
        this.elements = elements;
        this.visible = true;
    }

    // Entry Points \\

    public ElementInstance getEntryPoint(int index) {
        ObjectArrayList<String> eps = handle.getEntryPoints();
        if (eps == null || index >= eps.size())
            return null;
        return findById(elements, eps.get(index));
    }

    public void addToEntryPoint(int index, ElementInstance element) {
        ElementInstance container = getEntryPoint(index);
        if (container != null)
            container.addChild(element);
    }

    public void removeFromEntryPoint(int index, ElementInstance element) {
        ElementInstance container = getEntryPoint(index);
        if (container != null)
            container.removeChild(element);
    }

    private ElementInstance findById(ObjectArrayList<ElementInstance> list, String id) {
        for (int i = 0; i < list.size(); i++) {
            ElementInstance el = list.get(i);
            if (el.getHandle().getId().equals(id))
                return el;
            if (el.hasChildren()) {
                ElementInstance found = findById(el.getChildren(), id);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    // Visibility \\

    public void show() {
        this.visible = true;
    }

    public void hide() {
        this.visible = false;
    }

    // Accessible \\

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