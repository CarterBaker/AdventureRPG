package application.bootstrap.menupipeline.menulist;

import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuListHandle extends HandlePackage {

    /*
     * Per-window open menu list. Owned by WindowInstance exactly as
     * RenderQueueHandle is — one per window, created in awake().
     * MenuManager routes all open/close/query operations through the
     * window's handle rather than a global active list.
     *
     * Lock state is derived live from the open menu list rather than
     * maintained as a separate counter. No counter means no drift and
     * no off-by-one on close — the answer is always exactly what is
     * actually open in this window right now.
     */

    private ObjectArrayList<MenuInstance> openMenus;

    // Internal \\

    public void constructor() {
        this.openMenus = new ObjectArrayList<>();
    }

    // Management \\

    public void add(MenuInstance menu) {
        openMenus.add(menu);
    }

    public void remove(MenuInstance menu) {
        openMenus.remove(menu);
    }

    public boolean contains(MenuInstance menu) {
        return openMenus.contains(menu);
    }

    // Lock State — derived, never counted \\

    public boolean isInputLocked() {
        for (int i = 0; i < openMenus.size(); i++)
            if (openMenus.get(i).getMenuData().isLockInput())
                return true;
        return false;
    }

    public boolean isRaycastLocked() {
        for (int i = 0; i < openMenus.size(); i++)
            if (openMenus.get(i).getMenuData().isRaycastInput())
                return true;
        return false;
    }

    // Accessible \\

    public boolean isOpen() {
        return !openMenus.isEmpty();
    }

    public boolean isEmpty() {
        return openMenus.isEmpty();
    }

    public ObjectArrayList<MenuInstance> getMenus() {
        return openMenus;
    }
}