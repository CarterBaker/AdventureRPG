package program.bootstrap.menupipeline.menu;

import program.bootstrap.menupipeline.element.ElementPlacementStruct;
import program.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded menu definition. Registered and owned by
     * MenuManager. Wraps MenuData and holds the placement tree needed to
     * instantiate a MenuInstance at runtime.
     */

    // Internal
    private MenuData data;
    private ObjectArrayList<ElementPlacementStruct> placements;

    // Constructor \\

    public void constructor(
            MenuData data,
            ObjectArrayList<ElementPlacementStruct> placements) {

        // Internal
        this.data = data;
        this.placements = placements;
    }

    // Accessible \\

    public MenuData getMenuData() {
        return data;
    }

    public String getName() {
        return data.getName();
    }

    public boolean isLockInput() {
        return data.isLockInput();
    }

    public boolean isRaycastInput() {
        return data.isRaycastInput();
    }

    public ObjectArrayList<String> getEntryPoints() {
        return data.getEntryPoints();
    }

    public ObjectArrayList<ElementPlacementStruct> getPlacements() {
        return placements;
    }
}