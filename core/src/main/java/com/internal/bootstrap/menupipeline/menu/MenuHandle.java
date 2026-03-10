package com.internal.bootstrap.menupipeline.menu;

import com.internal.bootstrap.menupipeline.element.ElementPlacementHandle;
import com.internal.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuHandle extends HandlePackage {

    private String name;
    private ObjectArrayList<ElementPlacementHandle> placements;
    private boolean lockInput;
    private boolean raycastInput;
    private ObjectArrayList<String> entryPoints;

    public void constructor(
            String name,
            ObjectArrayList<ElementPlacementHandle> placements,
            boolean lockInput,
            boolean raycastInput,
            ObjectArrayList<String> entryPoints) {
        this.name = name;
        this.placements = placements;
        this.lockInput = lockInput;
        this.raycastInput = raycastInput;
        this.entryPoints = entryPoints;
    }

    public String getName() {
        return name;
    }

    public ObjectArrayList<ElementPlacementHandle> getPlacements() {
        return placements;
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