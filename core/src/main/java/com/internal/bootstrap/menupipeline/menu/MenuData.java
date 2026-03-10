package com.internal.bootstrap.menupipeline.menu;

import com.internal.bootstrap.menupipeline.element.ElementData;
import com.internal.core.engine.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuData extends DataPackage {

    private String name;
    private ObjectArrayList<ElementData> elements;
    private boolean lockInput;
    private boolean raycastInput;
    private ObjectArrayList<String> entryPoints;

    public void constructor(
            String name,
            ObjectArrayList<ElementData> elements,
            boolean lockInput,
            boolean raycastInput,
            ObjectArrayList<String> entryPoints) {
        this.name = name;
        this.elements = elements;
        this.lockInput = lockInput;
        this.raycastInput = raycastInput;
        this.entryPoints = entryPoints;
    }

    public String getName() {
        return name;
    }

    public ObjectArrayList<ElementData> getElements() {
        return elements;
    }

    public boolean isLockInput() {
        return lockInput;
    }

    public boolean isRaycastInput() {
        return raycastInput;
    }

    public ObjectArrayList<String> getEntryPoints() {
        return entryPoints;
    }
}