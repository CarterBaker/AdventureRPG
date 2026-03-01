package com.internal.bootstrap.menupipeline.menumanager;

import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.menupipeline.element.ElementInstance;
import com.internal.bootstrap.menupipeline.elementsystem.ElementSystem;
import com.internal.bootstrap.menupipeline.menu.MenuHandle;
import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.bootstrap.menupipeline.raycastsystem.RaycastSystem;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.WindowInstance;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;
    private ElementSystem elementSystem;
    private RenderSystem renderSystem;
    private WindowInstance windowInstance;
    private InputSystem inputSystem;
    private RaycastSystem raycastSystem;

    // Menu registry — string lookup once on open, int from there
    private Object2IntOpenHashMap<String> menuName2MenuID;
    private Int2ObjectOpenHashMap<MenuHandle> menuID2Handle;
    private int nextMenuID;

    // Active — ordered by open time
    private ObjectArrayList<MenuInstance> activeMenus;

    // Cached screen size
    private float screenW;
    private float screenH;

    // Lock reference counts
    private int inputLockCount;
    private int raycastLockCount;

    // Internal \\

    @Override
    protected void create() {
        this.internalLoadManager = create(InternalLoadManager.class);
        this.raycastSystem = create(RaycastSystem.class);
        this.menuName2MenuID = new Object2IntOpenHashMap<>();
        this.menuID2Handle = new Int2ObjectOpenHashMap<>();
        this.menuName2MenuID.defaultReturnValue(-1);
        this.nextMenuID = 0;
        this.activeMenus = new ObjectArrayList<>();
        this.inputLockCount = 0;
        this.raycastLockCount = 0;
    }

    @Override
    protected void get() {
        this.elementSystem = get(ElementSystem.class);
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
            ObjectArrayList<ElementInstance> elements = instance.getElements();
            for (int j = 0; j < elements.size(); j++)
                renderElement(elements.get(j), 0f, 0f, screenW, screenH);
        }
    }

    // Render Traversal \\

    private void renderElement(
            ElementInstance element,
            float parentLeft, float parentTop,
            float parentW, float parentH) {

        element.computeTransform(parentLeft, parentTop, parentW, parentH);

        if (element.hasSprite())
            pushSpriteRenderCall(element);

        if (element.hasChildren()) {
            ObjectArrayList<ElementInstance> children = element.getChildren();
            for (int i = 0; i < children.size(); i++)
                renderElement(children.get(i),
                        element.getComputedLeft(), element.getComputedTop(),
                        element.getComputedW(), element.getComputedH());
        }
    }

    private void pushSpriteRenderCall(ElementInstance element) {
        element.getSpriteInstance()
                .getModelHandle()
                .getMaterial()
                .setUniform("u_transform", element.getTransform());
        renderSystem.pushRenderCall(element.getSpriteInstance().getModelHandle(), 1);
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
        int id = nextMenuID++;
        menuName2MenuID.put(menuName, id);
        menuID2Handle.put(id, menuHandle);
    }

    // Accessible \\

    public MenuInstance openMenu(String menuName) {

        int id = menuName2MenuID.getInt(menuName);
        if (id == -1)
            throwException("Menu not found: '" + menuName + "'");

        MenuHandle handle = menuID2Handle.get(id);

        MenuInstance[] holder = { null };
        ObjectArrayList<ElementInstance> liveElements = elementSystem.createInstances(
                handle.getPlacements(), () -> holder[0]);

        MenuInstance instance = create(MenuInstance.class);
        instance.constructor(handle, liveElements);
        holder[0] = instance;
        activeMenus.add(instance);

        if (handle.isLockInput())
            applyInputLock(1);
        if (handle.isRaycastInput())
            applyRaycastLock(1);

        return instance;
    }

    public MenuInstance closeMenu(MenuInstance instance) {

        if (instance.getHandle().isLockInput())
            applyInputLock(-1);
        if (instance.getHandle().isRaycastInput())
            applyRaycastLock(-1);

        activeMenus.remove(instance);
        return null;
    }

    public boolean hasMenu(String menuName) {
        return menuName2MenuID.containsKey(menuName);
    }

    public MenuHandle getMenuHandle(String menuName) {
        int id = menuName2MenuID.getInt(menuName);
        return id == -1 ? null : menuID2Handle.get(id);
    }

    public ObjectArrayList<MenuInstance> getActiveMenus() {
        return activeMenus;
    }
}