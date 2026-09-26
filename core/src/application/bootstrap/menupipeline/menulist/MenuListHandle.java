package application.bootstrap.menupipeline.menulist;

import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuListHandle extends HandlePackage {

    /*
     * Per-window list of open menus, owned by WindowInstance. Lock state is
     * derived live from what is open, and the lock release listener fires once
     * when the last lock_input menu closes so capture can be restored without a
     * click.
     */

    private ObjectArrayList<MenuInstance> openMenus;

    // Notified when the list transitions locked → unlocked on remove
    private Runnable lockReleaseListener;

    // Internal \\

    public void constructor() {
        this.openMenus = new ObjectArrayList<>();
    }

    // Management \\

    public void add(MenuInstance menu) {
        openMenus.add(menu);
    }

    public void remove(MenuInstance menu) {
        boolean wasLocked = isInputLocked();
        openMenus.remove(menu);
        if (wasLocked && !isInputLocked() && lockReleaseListener != null)
            lockReleaseListener.run();
    }

    public boolean contains(MenuInstance menu) {
        return openMenus.contains(menu);
    }

    // Lock Release Listener \\

    public void setLockReleaseListener(Runnable listener) {
        this.lockReleaseListener = listener;
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