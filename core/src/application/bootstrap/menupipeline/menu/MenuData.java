package application.bootstrap.menupipeline.menu;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuData extends DataPackage {

    /*
     * Persistent menu definition. Holds identity, input lock flags, named
     * entry point IDs, and whether this menu defines a canvas_area element.
     * Owned by MenuHandle, created with new during bootstrap.
     */

    // Identity
    private final String name;
    private final boolean lockInput;
    private final boolean raycastInput;
    private final boolean hasCanvasArea;

    // Entry Points
    private final ObjectArrayList<String> entryPoints;

    // Constructor \\

    public MenuData(
            String name,
            boolean lockInput,
            boolean raycastInput,
            boolean hasCanvasArea,
            ObjectArrayList<String> entryPoints) {
        this.name = name;
        this.lockInput = lockInput;
        this.raycastInput = raycastInput;
        this.hasCanvasArea = hasCanvasArea;
        this.entryPoints = entryPoints;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public boolean isLockInput() {
        return lockInput;
    }

    public boolean isRaycastInput() {
        return raycastInput;
    }

    public boolean hasCanvasArea() {
        return hasCanvasArea;
    }

    public ObjectArrayList<String> getEntryPoints() {
        return entryPoints;
    }
}