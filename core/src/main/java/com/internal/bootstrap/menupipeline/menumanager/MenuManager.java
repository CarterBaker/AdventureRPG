package com.internal.bootstrap.menupipeline.menumanager;

import java.util.function.Consumer;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.menupipeline.element.ElementData;
import com.internal.bootstrap.menupipeline.element.ElementHandle;
import com.internal.bootstrap.menupipeline.element.ElementInstance;
import com.internal.bootstrap.menupipeline.element.ElementPlacementStruct;
import com.internal.bootstrap.menupipeline.fonts.FontInstance;
import com.internal.bootstrap.menupipeline.menu.MenuData;
import com.internal.bootstrap.menupipeline.menu.MenuHandle;
import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.bootstrap.menupipeline.raycastsystem.RaycastSystem;
import com.internal.bootstrap.menupipeline.util.LayoutStruct;
import com.internal.bootstrap.menupipeline.util.StackDirection;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.bootstrap.renderpipeline.util.MaskStruct;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.WindowInstance;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.RegistryUtility;
import com.internal.core.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuManager extends ManagerPackage {

    /*
     * Owns the menu palette and drives the menu lifecycle. Handles opening and
     * closing MenuInstances, element tree rendering, font GPU upload and release,
     * runtime element injection, input and raycast lock reference counting, and
     * mask stack management. Screen size is cached on awake and updated on resize.
     */

    // Internal
    private ElementSystem elementSystem;
    private RenderSystem renderSystem;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private WindowInstance windowInstance;
    private InputSystem inputSystem;
    private RaycastSystem raycastSystem;

    // Palette
    private Object2IntOpenHashMap<String> menuName2MenuID;
    private Int2ObjectOpenHashMap<MenuHandle> menuID2MenuHandle;

    // Active
    private ObjectArrayList<MenuInstance> activeMenus;

    // Screen
    private float screenW;
    private float screenH;

    // Lock Reference Counts
    private int inputLockCount;
    private int raycastLockCount;

    // Cached Transforms
    private Matrix4 fontTransform;

    // Mask Pool
    private MaskStruct[] maskPool;
    private int maskDepth;

    // Scratch
    private ObjectArrayList<ElementInstance> singletonScratch;

    // Internal \\

    @Override
    protected void create() {

        this.menuName2MenuID = new Object2IntOpenHashMap<>();
        this.menuID2MenuHandle = new Int2ObjectOpenHashMap<>();
        this.menuName2MenuID.defaultReturnValue(-1);

        this.activeMenus = new ObjectArrayList<>();
        this.fontTransform = new Matrix4();

        this.maskPool = new MaskStruct[EngineSetting.MAX_MASK_DEPTH];
        for (int i = 0; i < maskPool.length; i++)
            maskPool[i] = new MaskStruct();

        this.singletonScratch = new ObjectArrayList<>(1);

        this.elementSystem = create(ElementSystem.class);

        create(InternalLoader.class);
    }

    @Override
    protected void get() {
        this.renderSystem = get(RenderSystem.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.inputSystem = get(InputSystem.class);
        this.raycastSystem = get(RaycastSystem.class);
        this.windowInstance = internal.getWindowInstance();
    }

    @Override
    protected void awake() {
        cacheScreenSize();
    }

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

        ElementData data = element.getElementData();

        if (element.hasSprite())
            pushSpriteRenderCall(element);

        if (element.hasFont())
            pushFontRenderCall(element);

        if (!element.hasChildren())
            return;

        if (data.isMask())
            pushMask(element);

        StackDirection stack = data.getStackDirection();

        if (stack != StackDirection.NONE)
            renderStacked(element, stack);
        else {
            ObjectArrayList<ElementInstance> children = element.getChildren();
            for (int i = 0; i < children.size(); i++)
                renderElement(
                        children.get(i),
                        element.getComputedLeft(), element.getComputedTop(),
                        element.getComputedW(), element.getComputedH());
        }

        if (data.isMask())
            popMask();
    }

    private void renderStacked(ElementInstance parent, StackDirection dir) {

        boolean vertical = dir == StackDirection.VERTICAL;
        float parentW = parent.getComputedW();
        float parentH = parent.getComputedH();

        ElementData parentData = parent.getElementData();

        float spacing = parentData.getSpacing() != null
                ? parentData.getSpacing().resolve(vertical ? parentH : parentW)
                : 0f;

        float cursor = vertical
                ? parent.getComputedTop() + parent.getComputedH() - parent.getScrollY()
                : parent.getComputedLeft() + parent.getScrollX();

        float contentSize = 0f;
        ObjectArrayList<ElementInstance> children = parent.getChildren();

        for (int i = 0; i < children.size(); i++) {

            ElementInstance child = children.get(i);

            if (vertical) {

                LayoutStruct layout = child.getElementData().getLayout();
                float childH = layout.getSize().getY().resolve(parentH);

                if (layout.hasMinSize())
                    childH = Math.max(childH, layout.getMinSize().getY().resolve(parentH));

                if (layout.hasMaxSize())
                    childH = Math.min(childH, layout.getMaxSize().getY().resolve(parentH));

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
                .getModelInstance()
                .getMaterial()
                .setUniform("u_transform", element.getTransform());
        renderSystem.pushRenderCall(
                element.getSpriteInstance().getModelInstance(), 1, currentMask());
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

    // Mask Stack \\

    private void pushMask(ElementInstance element) {

        int x = (int) element.getComputedLeft();
        int y = (int) element.getComputedTop();
        int w = (int) element.getComputedW();
        int h = (int) element.getComputedH();

        if (maskDepth > 0) {
            MaskStruct prev = maskPool[maskDepth - 1];
            int ix = Math.max(x, prev.getX());
            int iy = Math.max(y, prev.getY());
            int ix2 = Math.min(x + w, prev.getX() + prev.getW());
            int iy2 = Math.min(y + h, prev.getY() + prev.getH());
            x = ix;
            y = iy;
            w = Math.max(0, ix2 - ix);
            h = Math.max(0, iy2 - iy);
        }

        maskPool[maskDepth].set(x, y, w, h);
        maskDepth++;
    }

    private void popMask() {
        maskDepth--;
    }

    private MaskStruct currentMask() {
        return maskDepth == 0 ? null : maskPool[maskDepth - 1];
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
        uploadFontModels(singletonScratch);

        menu.addToEntryPoint(entryPoint, instance);

        return instance;
    }

    public ElementInstance inject(MenuInstance menu, int entryPoint, String masterKey) {
        return inject(menu, entryPoint, masterKey, null);
    }

    public void eject(MenuInstance menu, int entryPoint, ElementInstance instance) {

        singletonScratch.clear();
        singletonScratch.add(instance);
        releaseFontModels(singletonScratch);

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

    public MenuInstance openMenu(String menuName) {

        MenuHandle handle = getMenuHandleFromMenuName(menuName);
        MenuInstance[] holder = { null };

        ObjectArrayList<ElementInstance> liveElements = elementSystem.createInstances(
                handle.getPlacements(), () -> holder[0]);

        MenuInstance instance = create(MenuInstance.class);
        instance.constructor(handle.getMenuData(), liveElements);
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

        MenuData data = instance.getMenuData();

        if (data.isLockInput())
            applyInputLock(-1);

        if (data.isRaycastInput())
            applyRaycastLock(-1);

        releaseFontModels(instance.getElements());
        activeMenus.remove(instance);

        return null;
    }

    public ObjectArrayList<MenuInstance> getActiveMenus() {
        return activeMenus;
    }

    public void request(String menuName) {
        ((InternalLoader) internalLoader).request(menuName);
    }
}