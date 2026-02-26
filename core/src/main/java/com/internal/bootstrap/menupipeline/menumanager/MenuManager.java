package com.internal.bootstrap.menupipeline.menumanager;

import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.menupipeline.element.MenuElementHandle;
import com.internal.bootstrap.menupipeline.menu.MenuHandle;
import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.bootstrap.menupipeline.raycastsystem.RaycastSystem;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.WindowInstance;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;
    private RenderSystem renderSystem;
    private WindowInstance windowInstance;
    private InputSystem inputSystem;
    private RaycastSystem raycastSystem;

    // Data
    private Object2ObjectOpenHashMap<String, MenuHandle> menuName2MenuHandle;

    // Active — ordered by open time
    private ObjectArrayList<MenuInstance> activeMenus;

    // Cached — updated on resize, used to resolve percentage-based dimensions
    private float screenW;
    private float screenH;

    // Input lock reference count — unlocks only when all locking menus are closed
    private int inputLockCount;

    // Raycast lock reference count — active only when at least one raycast menu is
    // open
    private int raycastLockCount;

    // Base \\

    @Override
    protected void create() {
        this.internalLoadManager = create(InternalLoadManager.class);
        this.raycastSystem = create(RaycastSystem.class);
        this.menuName2MenuHandle = new Object2ObjectOpenHashMap<>();
        this.activeMenus = new ObjectArrayList<>();
        this.inputLockCount = 0;
        this.raycastLockCount = 0;
    }

    @Override
    protected void get() {
        this.renderSystem = get(RenderSystem.class);
        this.inputSystem = get(InputSystem.class);
        this.windowInstance = internal.getWindowInstance();
    }

    @Override
    protected void awake() {
        internalLoadManager.loadMenuData();
        cacheScreenSize();
    }

    @Override
    protected void release() {
        this.internalLoadManager = release(InternalLoadManager.class);
    }

    // Update \\

    @Override
    public void update() {

        if (activeMenus.isEmpty() || screenW == 0 || screenH == 0)
            return;

        raycastSystem.update(activeMenus, screenW, screenH);

        for (int i = 0; i < activeMenus.size(); i++) {
            MenuInstance instance = activeMenus.get(i);
            if (!instance.isVisible())
                continue;

            for (MenuElementHandle element : instance.getHandle().getElements())
                renderElement(element, 0f, 0f, screenW, screenH);
        }
    }

    // Render Traversal \\

    private void renderElement(
            MenuElementHandle element,
            float parentLeft, float parentTop,
            float parentW, float parentH) {

        element.computeTransform(parentLeft, parentTop, parentW, parentH);

        if (element.hasSprite())
            pushSpriteRenderCall(element);

        if (element.hasChildren())
            for (MenuElementHandle child : element.getChildren())
                renderElement(child,
                        element.getComputedLeft(), element.getComputedTop(),
                        element.getComputedW(), element.getComputedH());
    }

    private void pushSpriteRenderCall(MenuElementHandle element) {
        element.getSpriteHandle()
                .getModelHandle()
                .getMaterial()
                .setUniform("u_transform", element.getTransform());

        renderSystem.pushRenderCall(
                element.getSpriteHandle().getModelHandle(), 1);
    }

    // Screen Size \\

    public void resize(int width, int height) {
        this.screenW = width;
        this.screenH = height;
    }

    private void cacheScreenSize() {
        this.screenW = windowInstance.getWidth();
        this.screenH = windowInstance.getHeight();
    }

    // Input Lock \\

    private void applyInputLock(int delta) {
        inputLockCount = Math.max(0, inputLockCount + delta);
        inputSystem.lockInput(inputLockCount > 0);
    }

    // Raycast Lock \\

    private void applyRaycastLock(int delta) {
        raycastLockCount = Math.max(0, raycastLockCount + delta);
        raycastSystem.setActive(raycastLockCount > 0);
    }

    // Menu Management \\

    void addMenu(String menuName, MenuHandle menuHandle) {
        menuName2MenuHandle.put(menuName, menuHandle);
    }

    // Accessible \\

    public MenuInstance openMenu(String menuName) {

        MenuHandle handle = menuName2MenuHandle.get(menuName);
        if (handle == null)
            throwException("Menu not found: '" + menuName + "'");

        MenuInstance instance = create(MenuInstance.class);
        instance.constructor(handle);
        activeMenus.add(instance);

        if (handle.isLockInput())
            applyInputLock(1);

        if (handle.isRaycastInput())
            applyRaycastLock(1);

        return instance;
    }

    public void closeMenu(MenuInstance instance) {

        if (instance.getHandle().isLockInput())
            applyInputLock(-1);

        if (instance.getHandle().isRaycastInput())
            applyRaycastLock(-1);

        activeMenus.remove(instance);
    }

    public boolean hasMenu(String menuName) {
        return menuName2MenuHandle.containsKey(menuName);
    }

    public MenuHandle getMenuHandle(String menuName) {
        return menuName2MenuHandle.get(menuName);
    }

    public ObjectArrayList<MenuInstance> getActiveMenus() {
        return activeMenus;
    }
}