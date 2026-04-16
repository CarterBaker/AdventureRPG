package application.bootstrap.menupipeline.menumanager;

import java.util.function.Consumer;

import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementPlacementStruct;
import application.bootstrap.menupipeline.menu.MenuData;
import application.bootstrap.menupipeline.menu.MenuHandle;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuManager extends ManagerPackage {

    /*
     * Owns the menu palette and drives the menu lifecycle. Handles opening and
     * closing MenuInstances, runtime element injection, and deferred menu closing.
     * Rendering, hit testing, mask stack, lock management, and font upload/release
     * are delegated to dedicated systems.
     */

    // Internal
    private ElementSystem elementSystem;
    private RenderSystem renderSystem;
    private HitSystem hitSystem;
    private LockSystem lockSystem;

    // Palette
    private Object2IntOpenHashMap<String> menuName2MenuID;
    private Int2ObjectOpenHashMap<MenuHandle> menuID2MenuHandle;

    // Active
    private ObjectArrayList<MenuInstance> activeMenus;
    private ObjectArrayList<MenuInstance> pendingCloseMenus;

    // Scratch
    private ObjectArrayList<ElementInstance> singletonScratch;

    // Internal \\

    @Override
    protected void create() {

        this.menuName2MenuID = new Object2IntOpenHashMap<>();
        this.menuID2MenuHandle = new Int2ObjectOpenHashMap<>();
        this.menuName2MenuID.defaultReturnValue(-1);

        this.activeMenus = new ObjectArrayList<>();
        this.pendingCloseMenus = new ObjectArrayList<>();

        this.singletonScratch = new ObjectArrayList<>(1);

        this.elementSystem = create(ElementSystem.class);
        this.renderSystem = create(RenderSystem.class);
        this.hitSystem = create(HitSystem.class);
        this.lockSystem = create(LockSystem.class);

        create(InternalLoader.class);
    }

    @Override
    protected void get() {
    }

    @Override
    protected void update() {

        flushPendingClosedMenus();

        if (activeMenus.isEmpty())
            return;

        for (int i = 0; i < activeMenus.size(); i++)
            renderSystem.renderMenu(activeMenus.get(i));

        if (lockSystem.isRaycastLocked())
            hitSystem.updateRaycast(activeMenus);
    }

    // Deferred Menu Close \\

    private void flushPendingClosedMenus() {

        if (pendingCloseMenus.isEmpty())
            return;

        for (int i = 0; i < pendingCloseMenus.size(); i++) {

            MenuInstance instance = pendingCloseMenus.get(i);

            renderSystem.releaseFontModels(instance.getElements());
            activeMenus.remove(instance);
        }

        pendingCloseMenus.clear();
    }

    // Runtime Injection \\

    public ElementInstance inject(
            MenuInstance menu,
            int entryPoint,
            String masterKey,
            Consumer<ElementInstance> customizer) {

        ElementHandle master = elementSystem.getMaster(masterKey);

        if (master == null)
            throwException("inject failed — master not found: '" + masterKey + "'");

        ElementInstance instance = elementSystem.createDetachedInstance(
                new ElementPlacementStruct(master));

        if (customizer != null)
            customizer.accept(instance);

        singletonScratch.clear();
        singletonScratch.add(instance);
        renderSystem.uploadFontModels(singletonScratch);

        menu.addToEntryPoint(entryPoint, instance);

        return instance;
    }

    public ElementInstance inject(MenuInstance menu, int entryPoint, String masterKey) {
        return inject(menu, entryPoint, masterKey, null);
    }

    public void eject(MenuInstance menu, int entryPoint, ElementInstance instance) {

        singletonScratch.clear();
        singletonScratch.add(instance);
        renderSystem.releaseFontModels(singletonScratch);

        menu.removeFromEntryPoint(entryPoint, instance);
    }

    public void refreshText(ElementInstance element) {

        singletonScratch.clear();
        singletonScratch.add(element);
        renderSystem.uploadFontModels(singletonScratch);
    }

    // Management \\

    void addMenu(String menuName, MenuHandle menuHandle) {
        int id = RegistryUtility.toIntID(menuName);
        menuName2MenuID.put(menuName, id);
        menuID2MenuHandle.put(id, menuHandle);
    }

    // Accessible \\

    public boolean isInputLocked() {
        return lockSystem.isInputLocked();
    }

    public boolean hasMenu(String menuName) {
        return menuName2MenuID.containsKey(menuName);
    }

    public int getMenuIDFromMenuName(String menuName) {

        if (!menuName2MenuID.containsKey(menuName))
            request(menuName);

        return menuName2MenuID.getInt(menuName);
    }

    public MenuHandle getMenuHandleFromMenuID(int menuID) {

        MenuHandle handle = menuID2MenuHandle.get(menuID);

        if (handle == null)
            throwException("Menu ID not found: " + menuID);

        return handle;
    }

    public MenuHandle getMenuHandleFromMenuName(String menuName) {
        return getMenuHandleFromMenuID(getMenuIDFromMenuName(menuName));
    }

    public MenuInstance openMenu(String menuName, WindowInstance window) {

        MenuHandle handle = getMenuHandleFromMenuName(menuName);
        MenuInstance[] holder = { null };

        ObjectArrayList<ElementInstance> liveElements = elementSystem.createInstances(
                handle.getPlacements(), () -> holder[0]);

        MenuInstance instance = create(MenuInstance.class);
        instance.constructor(handle.getMenuData(), liveElements, window);
        holder[0] = instance;

        renderSystem.uploadFontModels(liveElements);
        activeMenus.add(instance);

        if (handle.isLockInput())
            lockSystem.applyInputLock(1);

        if (handle.isRaycastInput())
            lockSystem.applyRaycastLock(1);

        return instance;
    }

    public MenuInstance closeMenu(MenuInstance instance) {

        if (instance == null)
            return null;

        MenuData data = instance.getMenuData();

        if (data.isLockInput())
            lockSystem.applyInputLock(-1);

        if (data.isRaycastInput()) {
            lockSystem.applyRaycastLock(-1);
            if (!lockSystem.isRaycastLocked())
                hitSystem.resetPressed();
        }

        instance.hide();

        if (!pendingCloseMenus.contains(instance))
            pendingCloseMenus.add(instance);

        return null;
    }

    public ObjectArrayList<MenuInstance> getActiveMenus() {
        return activeMenus;
    }

    public void request(String menuName) {
        ((InternalLoader) internalLoader).request(menuName);
    }
}
