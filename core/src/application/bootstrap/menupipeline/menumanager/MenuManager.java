package application.bootstrap.menupipeline.menumanager;

import java.util.function.Consumer;

import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuHandle;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menu.MenuNodeStruct;
import application.bootstrap.menupipeline.menulist.MenuListHandle;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuManager extends ManagerPackage {

    /*
     * Owns the menu palette and the menu lifecycle. Open menus live per window
     * in its MenuListHandle. Input routes through ElementHitSystem; opening and
     * closing lock_input menus releases and reclaims cursor capture on the
     * focused window. openMenuWindow() and closeMenuWindow() are the single
     * pair for menus that live in their own logical window.
     */

    // Internal
    private ElementSystem elementSystem;
    private MenuRenderSystem renderSystem;
    private ElementHitSystem hitSystem;
    private WindowManager windowManager;
    private InputManager inputManager;
    private FBOManager fboManager;

    // Palette
    private Object2IntOpenHashMap<String> menuName2MenuID;
    private Int2ObjectOpenHashMap<MenuHandle> menuID2MenuHandle;

    // Deferred close — scratch buffer only, not the source of truth for open state
    private ObjectArrayList<MenuInstance> pendingCloseMenus;

    // FBO routing
    private Object2ObjectOpenHashMap<WindowInstance, FBOInstance> window2MenuTargetFbo;

    // Internal \\

    @Override
    protected void create() {
        this.menuName2MenuID = new Object2IntOpenHashMap<>();
        this.menuID2MenuHandle = new Int2ObjectOpenHashMap<>();
        this.menuName2MenuID.defaultReturnValue(-1);
        this.pendingCloseMenus = new ObjectArrayList<>();
        this.window2MenuTargetFbo = new Object2ObjectOpenHashMap<>();
        create(MenuLoader.class);
    }

    @Override
    protected void get() {
        this.elementSystem = get(ElementSystem.class);
        this.renderSystem = get(MenuRenderSystem.class);
        this.hitSystem = get(ElementHitSystem.class);
        this.windowManager = get(WindowManager.class);
        this.inputManager = get(InputManager.class);
        this.fboManager = get(FBOManager.class);
    }

    @Override
    protected void update() {

        flushPendingClosedMenus();

        // Render — all windows, regardless of hover state
        ObjectArrayList<WindowInstance> windows = windowManager.getWindows();
        int windowCount = windows.size();

        for (int i = 0; i < windowCount; i++) {
            WindowInstance window = windows.get(i);
            MenuListHandle menuList = window.getMenuListHandle();
            if (menuList.isEmpty())
                continue;
            FBOInstance menuTargetFbo = window2MenuTargetFbo.get(window);
            if (menuTargetFbo == null)
                continue;
            ObjectArrayList<MenuInstance> menus = menuList.getMenus();
            int menuCount = menus.size();
            for (int j = 0; j < menuCount; j++) {
                MenuInstance menu = menus.get(j);
                menu.advance(internal.getDeltaTime());
                renderSystem.renderMenu(menu, menuTargetFbo, RuntimeSetting.LAYER_UI);
            }
        }

        // Input — ordered list of all hovered windows, highest priority first.
        // ElementHitSystem iterates the list and dispatches to the first hit.
        ObjectArrayList<WindowInstance> hoveredWindows = windowManager.getHoveredWindows();

        hitSystem.clearHoverIfWindowChanged(hoveredWindows);
        hitSystem.updateRaycast(hoveredWindows);
    }

    // Deferred Menu Close \\

    private void flushPendingClosedMenus() {

        if (pendingCloseMenus.isEmpty())
            return;

        for (int i = 0; i < pendingCloseMenus.size(); i++) {
            MenuInstance instance = pendingCloseMenus.get(i);
            WindowInstance window = instance.getWindow();
            renderSystem.releaseFontModels(instance.getElements());
            window.getMenuListHandle().remove(instance);

            if (!window.getMenuListHandle().isInputLocked()
                    && window == windowManager.getFocusedWindow())
                inputManager.captureCursor(true, window);
        }

        pendingCloseMenus.clear();
    }

    // Runtime Injection \\

    public ElementInstance inject(
            MenuInstance menu,
            int entryPoint,
            String masterKey,
            Consumer<ElementInstance> customizer) {

        ElementInstance instance = createInjected(masterKey, customizer);

        menu.addToEntryPoint(entryPoint, instance);
        return instance;
    }

    public ElementInstance inject(MenuInstance menu, int entryPoint, String masterKey) {
        return inject(menu, entryPoint, masterKey, null);
    }

    public ElementInstance inject(
            ElementInstance container,
            String masterKey,
            Consumer<ElementInstance> customizer) {

        ElementInstance instance = createInjected(masterKey, customizer);

        container.addChild(instance);
        return instance;
    }

    private ElementInstance createInjected(String masterKey, Consumer<ElementInstance> customizer) {

        ElementHandle master = elementSystem.getMaster(masterKey);

        if (master == null)
            throwException("inject failed — master not found: '" + masterKey + "'");

        ElementInstance instance = elementSystem.createDetachedInstance(
                new MenuNodeStruct(master, master.getChildren()));

        if (customizer != null)
            customizer.accept(instance);

        return instance;
    }

    public void eject(MenuInstance menu, int entryPoint, ElementInstance instance) {
        ObjectArrayList<ElementInstance> single = new ObjectArrayList<>(1);
        single.add(instance);
        renderSystem.releaseFontModels(single);
        menu.removeFromEntryPoint(entryPoint, instance);
    }

    public void ejectAll(MenuInstance menu, int entryPoint) {

        ElementInstance container = menu.getEntryPoint(entryPoint);

        if (container == null)
            return;

        while (!container.getChildren().isEmpty())
            eject(menu, entryPoint, container.getChildren().get(0));
    }

    // Management \\

    void addMenu(String menuName, MenuHandle menuHandle) {
        int id = RegistryUtility.toIntID(menuName);
        menuName2MenuID.put(menuName, id);
        menuID2MenuHandle.put(id, menuHandle);
    }

    // Accessible \\

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
                handle.getNodes(), () -> holder[0]);

        MenuInstance instance = create(MenuInstance.class);
        instance.constructor(handle.getMenuData(), liveElements, window);
        holder[0] = instance;

        window.getMenuListHandle().add(instance);

        if (window.getMenuListHandle().isInputLocked()
                && window == windowManager.getFocusedWindow())
            inputManager.captureCursor(false, window);

        return instance;
    }

    public MenuInstance closeMenu(MenuInstance instance) {

        if (instance == null)
            return null;

        instance.hide();

        if (!pendingCloseMenus.contains(instance))
            pendingCloseMenus.add(instance);

        if (!instance.getWindow().getMenuListHandle().isRaycastLocked())
            hitSystem.resetPressed();

        return null;
    }

    // Menu Windows \\

    public MenuInstance openMenuWindow(String menuName, WindowInstance osWindow) {

        WindowInstance window = windowManager.createLogicalWindow(menuName, osWindow);
        window.setCaptureEligible(false);
        window.setFocusIndependent(true);
        windowManager.bringToFront(window);

        setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        window.place(0, 0, osWindow.getWidth(), osWindow.getHeight());

        return openMenu(menuName, window);
    }

    public void closeMenuWindow(MenuInstance instance) {

        if (instance == null)
            return;

        WindowInstance window = instance.getWindow();
        closeMenu(instance);
        setMenuTargetFbo(window, null);
        window.dispose();
    }

    public ObjectArrayList<MenuInstance> getActiveMenus(WindowInstance window) {
        return window.getMenuListHandle().getMenus();
    }

    public boolean isMenuOpen(WindowInstance window) {
        return window.getMenuListHandle().isOpen();
    }

    public void request(String menuName) {
        ((MenuLoader) internalLoader).request(menuName);
    }

    public void setMenuTargetFbo(WindowInstance window, FBOInstance menuTargetFbo) {
        if (window == null)
            return;
        if (menuTargetFbo == null) {
            window2MenuTargetFbo.remove(window);
            return;
        }
        window2MenuTargetFbo.put(window, menuTargetFbo);
    }
}