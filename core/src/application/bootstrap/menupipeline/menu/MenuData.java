package application.bootstrap.menupipeline.menu;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuData extends DataPackage {

    /*
     * Persistent menu definition. Holds identity, input lock flags, and named
     * entry point IDs. Owned by MenuHandle, created with new during bootstrap.
     */

    // Identity
    private final String name;
    private final boolean lockInput;
    private final boolean raycastInput;

    // Entry Points
    private final ObjectArrayList<String> entryPoints;

    // Constructor \\

    public MenuData(
            String name,
            boolean lockInput,
            boolean raycastInput,
            ObjectArrayList<String> entryPoints) {

        // Identity
        this.name = name;
        this.lockInput = lockInput;
        this.raycastInput = raycastInput;

        // Entry Points
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

    public ObjectArrayList<String> getEntryPoints() {
        return entryPoints;
    }
}