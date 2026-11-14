package application.bootstrap.menupipeline.menulist;

import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuListHandle extends HandlePackage {

    /*
     * Per-window list of open menus, owned by WindowInstance. Lock state is
     * derived live from what is open, and the lock release listener fires once
     * when the last lock_input menu closes so capture can be restored without a
     * click. Answers whether a point lies on any menu the window shows. A
     * withheld window keeps its menus open but neither draws them nor lets
     * them take a hover, click, drag or scroll, until it is withheld no more.
     */

    private ObjectArrayList<MenuInstance> openMenus;

    // Withheld
    private boolean withheld;

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

    // Withheld \\

    public void setWithheld(boolean withheld) {
        this.withheld = withheld;
    }

    public boolean isWithheld() {
        return withheld;
    }

    // Lock State — derived, never counted \\

    public boolean isInputLocked() {
        for (int i = 0; i < openMenus.size(); i++)
            if (openMenus.get(i).getMenuData().isLockInput())
                return true;
        return false;
    }

    public boolean isRaycastLocked() {
        if (withheld)
            return false;
        for (int i = 0; i < openMenus.size(); i++)
            if (openMenus.get(i).getMenuData().isRaycastInput())
                return true;
        return false;
    }

    // Hit \\

    public boolean containsPoint(float x, float y) {
        if (withheld)
            return false;
        for (int i = 0; i < openMenus.size(); i++)
            if (openMenus.get(i).containsPoint(x, y))
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