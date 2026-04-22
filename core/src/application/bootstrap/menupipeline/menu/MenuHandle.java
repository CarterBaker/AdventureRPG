package application.bootstrap.menupipeline.menu;

import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded menu definition. Registered and owned by
     * MenuManager. Wraps MenuData and holds the root node list of the fully
     * resolved element tree needed to instantiate a MenuInstance at runtime.
     */

    // Internal
    private MenuData data;
    private ObjectArrayList<MenuNodeStruct> nodes;

    // Constructor \\

    public void constructor(MenuData data, ObjectArrayList<MenuNodeStruct> nodes) {
        this.data = data;
        this.nodes = nodes;
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

    public ObjectArrayList<MenuNodeStruct> getNodes() {
        return nodes;
    }
}