package com.internal.bootstrap.menupipeline.menumanager;

import java.util.function.Consumer;

import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.menupipeline.element.ElementInstance;
import com.internal.bootstrap.menupipeline.element.ElementPlacementHandle;
import com.internal.bootstrap.menupipeline.element.LayoutStruct;
import com.internal.bootstrap.menupipeline.element.StackDirection;
import com.internal.bootstrap.menupipeline.elementsystem.ElementSystem;
import com.internal.bootstrap.menupipeline.fonts.FontInstance;
import com.internal.bootstrap.menupipeline.menu.MenuHandle;
import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.bootstrap.menupipeline.raycastsystem.RaycastSystem;
import com.internal.bootstrap.renderpipeline.rendercall.MaskStruct;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.WindowInstance;
import com.internal.core.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuManager extends ManagerPackage {

    // Internal
    private ElementSystem elementSystem;
    private RenderSystem renderSystem;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private WindowInstance windowInstance;
    private InputSystem inputSystem;
    private RaycastSystem raycastSystem;

    // Menu registry
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

    // Cached font transform — translation only
    private final Matrix4 fontTransform = new Matrix4();

    // Mask stack — pure data, no GL calls here
    private final ObjectArrayList<MaskStruct> maskStack = new ObjectArrayList<>();

    // Base \\

    @Override
    protected void create() {
        create(InternalLoader.class);
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
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.inputSystem = get(InputSystem.class);
        this.windowInstance = internal.getWindowInstance();
    }

    @Override
    protected void awake() {
        cacheScreenSize();
    }

    // Update \\

    @Override
    protected void update() {
        if (activeMenus.isEmpty() || screenW == 0 || screenH == 0)
            return;
        for (int i = 0; i < activeMenus.size(); i++) {
            MenuInstance instance = activeMenus.get(i);
            if (!instance.isVisible())
                continue;
            ObjectArrayList<ElementInstance> elements = instance.getElements();
            for (int j = 0; j < elements.size(); j++)
                renderElement(elements.get(j), 0f, 0f, screenW, screenH);
        }
        raycastSystem.update(activeMenus, screenW, screenH);
    }

    // Render Traversal \\

    private void renderElement(
            ElementInstance element,
            float parentLeft, float parentTop,
            float parentW, float parentH) {
        element.computeTransform(parentLeft, parentTop, parentW, parentH);
        renderElementContent(element);
    }

    private void renderStackedElement(
            ElementInstance element,
            float left, float top,
            float parentW, float parentH) {
        element.computeStackedTransform(left, top, parentW, parentH);
        renderElementContent(element);
    }

    private void renderElementContent(ElementInstance element) {
        if (element.hasSprite())
            pushSpriteRenderCall(element);
        if (element.hasFont())
            pushFontRenderCall(element);

        if (!element.hasChildren())
            return;

        if (element.getHandle().isMask())
            pushMask(element);

        StackDirection stack = element.getHandle().getStackDirection();
        if (stack != StackDirection.NONE)
            renderStacked(element, stack);
        else {
            ObjectArrayList<ElementInstance> children = element.getChildren();
            for (int i = 0; i < children.size(); i++)
                renderElement(children.get(i),
                        element.getComputedLeft(), element.getComputedTop(),
                        element.getComputedW(), element.getComputedH());
        }

        if (element.getHandle().isMask())
            popMask();
    }

    private void renderStacked(ElementInstance parent, StackDirection dir) {
        boolean vertical = dir == StackDirection.VERTICAL;
        float parentW = parent.getComputedW();
        float parentH = parent.getComputedH();

        float spacing = parent.getHandle().getSpacing() != null
                ? parent.getHandle().getSpacing().resolve(vertical ? parentH : parentW)
                : 0f;

        float cursor = vertical
                ? parent.getComputedTop() + parent.getComputedH() - parent.getScrollY()
                : parent.getComputedLeft() + parent.getScrollX();

        float contentSize = 0f;
        ObjectArrayList<ElementInstance> children = parent.getChildren();

        for (int i = 0; i < children.size(); i++) {
            ElementInstance child = children.get(i);

            if (vertical) {
                LayoutStruct layout = child.getHandle().getLayout();
                float childH = layout.size.y.resolve(parentH);
                if (layout.minSize != null)
                    childH = Math.max(childH, layout.minSize.y.resolve(parentH));
                if (layout.maxSize != null)
                    childH = Math.min(childH, layout.maxSize.y.resolve(parentH));
                cursor -= childH;
                renderStackedElement(child, parent.getComputedLeft(), cursor, parentW, parentH);
                contentSize += childH + (i < children.size() - 1 ? spacing : 0f);
                cursor -= spacing;
            } else {
                renderStackedElement(child, cursor, parent.getComputedTop(), parentW, parentH);
                float childW = child.getComputedW();
                contentSize += childW + (i < children.size() - 1 ? spacing : 0f);
                cursor += childW + spacing;
            }
        }

        if (vertical)
            parent.setContentH(contentSize);
        else
            parent.setContentW(contentSize);
    }

    // Sprite Render Call \\

    private void pushSpriteRenderCall(ElementInstance element) {
        element.getSpriteInstance()
                .getModelHandle()
                .getMaterial()
                .setUniform("u_transform", element.getTransform());
        renderSystem.pushRenderCall(
                element.getSpriteInstance().getModelHandle(), 1, currentMask());
    }

    // Font Render Call \\

    private void pushFontRenderCall(ElementInstance element) {
        FontInstance font = element.getFontInstance();
        if (!font.hasModel() || font.getMergedModel().isEmpty())
            return;

        float x = element.getComputedLeft() + (element.getComputedW() - font.getTextWidth()) * 0.5f;
        float y = element.getComputedTop() + (element.getComputedH() - font.getTextHeight()) * 0.5f;

        fontTransform.set(
                1, 0, 0, x,
                0, 1, 0, y,
                0, 0, 1, 0,
                0, 0, 0, 1);

        font.getModelInstance()
                .getMaterial()
                .setUniform("u_transform", fontTransform);

        renderSystem.pushRenderCall(font.getModelInstance(), 2, currentMask());
    }

    // Mask Stack — pure data, MaskStruct travels with render calls \\

    private void pushMask(ElementInstance element) {
        int x = (int) element.getComputedLeft();
        int y = (int) element.getComputedTop();
        int w = (int) element.getComputedW();
        int h = (int) element.getComputedH();

        if (!maskStack.isEmpty()) {
            MaskStruct prev = maskStack.get(maskStack.size() - 1);
            int ix = Math.max(x, prev.x);
            int iy = Math.max(y, prev.y);
            int ix2 = Math.min(x + w, prev.x + prev.w);
            int iy2 = Math.min(y + h, prev.y + prev.h);
            x = ix;
            y = iy;
            w = Math.max(0, ix2 - ix);
            h = Math.max(0, iy2 - iy);
        }

        maskStack.add(new MaskStruct(x, y, w, h));
    }

    private void popMask() {
        maskStack.remove(maskStack.size() - 1);
    }

    private MaskStruct currentMask() {
        return maskStack.isEmpty() ? null : maskStack.get(maskStack.size() - 1);
    }

    // Font GPU Upload \\

    private void uploadFontModels(ObjectArrayList<ElementInstance> elements) {
        for (int i = 0; i < elements.size(); i++) {
            ElementInstance el = elements.get(i);
            if (el.hasFont())
                el.getFontInstance().upload(modelManager, materialManager);
            if (el.hasChildren())
                uploadFontModels(el.getChildren());
        }
    }

    // Font GPU Release \\

    private void releaseFontModels(ObjectArrayList<ElementInstance> elements) {
        for (int i = 0; i < elements.size(); i++) {
            ElementInstance el = elements.get(i);
            if (el.hasFont())
                el.getFontInstance().release(modelManager);
            if (el.hasChildren())
                releaseFontModels(el.getChildren());
        }
    }

    // Runtime Injection \\

    public ElementInstance inject(
            MenuInstance menu, int entryPoint,
            String masterKey, Consumer<ElementInstance> customizer) {

        var master = elementSystem.getMaster(masterKey);
        if (master == null)
            throwException("inject failed — master not found: '" + masterKey + "'");

        ElementPlacementHandle placement = create(ElementPlacementHandle.class);
        placement.constructor(master, null);
        ElementInstance instance = elementSystem.createDetachedInstance(placement);

        if (customizer != null)
            customizer.accept(instance);

        ObjectArrayList<ElementInstance> single = new ObjectArrayList<>();
        single.add(instance);
        uploadFontModels(single);

        menu.addToEntryPoint(entryPoint, instance);
        return instance;
    }

    public ElementInstance inject(MenuInstance menu, int entryPoint, String masterKey) {
        return inject(menu, entryPoint, masterKey, null);
    }

    public void eject(MenuInstance menu, int entryPoint, ElementInstance instance) {
        ObjectArrayList<ElementInstance> single = new ObjectArrayList<>();
        single.add(instance);
        releaseFontModels(single);
        menu.removeFromEntryPoint(entryPoint, instance);
    }

    public void refreshText(ElementInstance element) {
        if (element.hasFont())
            element.getFontInstance().upload(modelManager, materialManager);
        if (element.hasChildren())
            for (int i = 0; i < element.getChildren().size(); i++)
                refreshText(element.getChildren().get(i));
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

    // On-Demand Loading \\

    public void request(String menuName) {
        ((InternalLoader) internalLoader).request(menuName);
    }

    // Menu Management \\

    void addMenu(String menuName, MenuHandle menuHandle) {
        int id = nextMenuID++;
        menuName2MenuID.put(menuName, id);
        menuID2Handle.put(id, menuHandle);
    }

    // Accessible \\

    public MenuInstance openMenu(String menuName) {
        if (!menuName2MenuID.containsKey(menuName))
            request(menuName);
        int id = menuName2MenuID.getInt(menuName);
        if (id == -1)
            throwException("Menu not found after load: '" + menuName + "'");
        MenuHandle handle = menuID2Handle.get(id);
        MenuInstance[] holder = { null };
        ObjectArrayList<ElementInstance> liveElements = elementSystem.createInstances(
                handle.getPlacements(), () -> holder[0]);
        MenuInstance instance = create(MenuInstance.class);
        instance.constructor(handle, liveElements);
        holder[0] = instance;
        uploadFontModels(liveElements);
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
        releaseFontModels(instance.getElements());
        activeMenus.remove(instance);
        return null;
    }

    public boolean hasMenu(String menuName) {
        return menuName2MenuID.containsKey(menuName);
    }

    public MenuHandle getMenuHandle(String menuName) {
        if (!menuName2MenuID.containsKey(menuName))
            request(menuName);
        int id = menuName2MenuID.getInt(menuName);
        return id == -1 ? null : menuID2Handle.get(id);
    }

    public ObjectArrayList<MenuInstance> getActiveMenus() {
        return activeMenus;
    }
}