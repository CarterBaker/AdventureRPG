package application.bootstrap.menupipeline.menumanager;

import java.util.function.Consumer;

import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementPlacementStruct;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.fonts.FontInstance;
import application.bootstrap.menupipeline.menu.MenuData;
import application.bootstrap.menupipeline.menu.MenuHandle;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.StackDirection;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.ScreenRayStruct;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.renderpipeline.util.MaskStruct;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ManagerPackage;
import engine.settings.EngineSetting;
import engine.util.RegistryUtility;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuManager extends ManagerPackage {

    /*
     * Owns the menu palette and drives the menu lifecycle. Handles opening and
     * closing MenuInstances, element tree rendering, font GPU upload and release,
     * runtime element injection, and mask stack management. Screen size is read
     * from the menu's own window each frame — no resize() method and no active
     * window dependency. Render calls are routed to each menu's window explicitly.
     * Hit testing reads the frame's ScreenRayStruct from RaycastManager —
     * zero allocation.
     */

    // Internal
    private ElementSystem elementSystem;
    private RenderManager renderManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private InputSystem inputSystem;
    private RaycastManager raycastManager;

    // Palette
    private Object2IntOpenHashMap<String> menuName2MenuID;
    private Int2ObjectOpenHashMap<MenuHandle> menuID2MenuHandle;

    // Active
    private ObjectArrayList<MenuInstance> activeMenus;
    private ObjectArrayList<MenuInstance> pendingCloseMenus;

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

    // Raycast State
    private boolean wasPressed;

    // Current render target — set per menu before traversal
    private WindowInstance currentWindow;

    // Internal \\

    @Override
    protected void create() {

        this.menuName2MenuID = new Object2IntOpenHashMap<>();
        this.menuID2MenuHandle = new Int2ObjectOpenHashMap<>();
        this.menuName2MenuID.defaultReturnValue(-1);

        this.activeMenus = new ObjectArrayList<>();
        this.pendingCloseMenus = new ObjectArrayList<>();
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

        // Internal
        this.renderManager = get(RenderManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.inputSystem = get(InputSystem.class);
        this.raycastManager = get(RaycastManager.class);
    }

    @Override
    protected void update() {

        flushPendingClosedMenus();

        if (activeMenus.isEmpty())
            return;

        for (int i = 0; i < activeMenus.size(); i++) {

            MenuInstance instance = activeMenus.get(i);

            if (!instance.isVisible())
                continue;

            currentWindow = instance.getWindow();

            float screenW = currentWindow.getWidth();
            float screenH = currentWindow.getHeight();

            if (screenW == 0 || screenH == 0)
                continue;

            ObjectArrayList<ElementInstance> elements = instance.getElements();

            for (int j = 0; j < elements.size(); j++)
                renderElement(elements.get(j), 0f, 0f, screenW, screenH);
        }

        if (raycastLockCount > 0)
            updateRaycast();
    }

    // Raycast \\

    private void updateRaycast() {

        if (!raycastManager.hasScreenRay())
            return;

        ScreenRayStruct ray = raycastManager.getScreenRay();

        boolean pressed = inputSystem.isRightDown();
        boolean clicked = pressed && !wasPressed;
        wasPressed = pressed;

        if (!clicked)
            return;

        float mouseX = ray.getScreenX();
        float mouseY = ray.getScreenY();

        for (int i = activeMenus.size() - 1; i >= 0; i--) {

            MenuInstance instance = activeMenus.get(i);

            if (!instance.isVisible())
                continue;

            WindowInstance window = instance.getWindow();
            float screenW = window.getWidth();
            float screenH = window.getHeight();
            float adjustedMouseY = screenH - mouseY;

            if (hitTestElements(
                    instance.getElements(),
                    mouseX, adjustedMouseY,
                    0, 0, screenW, screenH))
                return;
        }
    }

    // Hit Testing \\

    private boolean hitTestElements(
            ObjectArrayList<ElementInstance> elements,
            float mouseX, float mouseY,
            float clipLeft, float clipTop,
            float clipRight, float clipBottom) {

        for (int i = elements.size() - 1; i >= 0; i--) {

            ElementInstance element = elements.get(i);

            if (element.hasChildren()) {

                float cl = clipLeft;
                float ct = clipTop;
                float cr = clipRight;
                float cb = clipBottom;

                if (element.getElementData().isMask()) {
                    cl = Math.max(cl, element.getComputedLeft());
                    ct = Math.max(ct, element.getComputedTop());
                    cr = Math.min(cr, element.getComputedLeft() + element.getComputedW());
                    cb = Math.min(cb, element.getComputedTop() + element.getComputedH());
                }

                if (hitTestElements(element.getChildren(), mouseX, mouseY, cl, ct, cr, cb))
                    return true;
            }

            if (element.getElementData().getType() != ElementType.BUTTON)
                continue;

            if (mouseX < clipLeft || mouseX > clipRight
                    || mouseY < clipTop || mouseY > clipBottom)
                continue;

            if (!isHit(element, mouseX, mouseY))
                continue;

            element.execute();
            return true;
        }

        return false;
    }

    private boolean isHit(ElementInstance element, float mouseX, float mouseY) {

        float left = element.getComputedLeft();
        float top = element.getComputedTop();
        float right = left + element.getComputedW();
        float bottom = top + element.getComputedH();

        return mouseX >= left && mouseX <= right
                && mouseY >= top && mouseY <= bottom;
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
        renderManager.pushRenderCall(
                element.getSpriteInstance().getModelInstance(), 1, currentMask(), currentWindow);
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

        renderManager.pushRenderCall(font.getModelInstance(), 2, currentMask(), currentWindow);
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

    // Deferred Menu Close \\

    private void flushPendingClosedMenus() {

        if (pendingCloseMenus.isEmpty())
            return;

        for (int i = 0; i < pendingCloseMenus.size(); i++) {

            MenuInstance instance = pendingCloseMenus.get(i);

            releaseFontModels(instance.getElements());
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

    // Input Lock \\

    private void applyInputLock(int delta) {

        int prev = inputLockCount;
        inputLockCount = Math.max(0, inputLockCount + delta);

        if (prev == 0 && inputLockCount > 0)
            inputSystem.captureCursor(false);
        else if (prev > 0 && inputLockCount == 0)
            inputSystem.captureCursor(true);
    }

    // Raycast Lock \\

    private void applyRaycastLock(int delta) {
        raycastLockCount = Math.max(0, raycastLockCount + delta);

        if (raycastLockCount == 0)
            wasPressed = false;
    }

    // Management \\

    void addMenu(String menuName, MenuHandle menuHandle) {
        int id = RegistryUtility.toIntID(menuName);
        menuName2MenuID.put(menuName, id);
        menuID2MenuHandle.put(id, menuHandle);
    }

    // Accessible \\

    public boolean isInputLocked() {
        return inputLockCount > 0;
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

        uploadFontModels(liveElements);
        activeMenus.add(instance);

        if (handle.isLockInput())
            applyInputLock(1);

        if (handle.isRaycastInput())
            applyRaycastLock(1);

        return instance;
    }

    public MenuInstance closeMenu(MenuInstance instance) {

        if (instance == null)
            return null;

        MenuData data = instance.getMenuData();

        if (data.isLockInput())
            applyInputLock(-1);

        if (data.isRaycastInput())
            applyRaycastLock(-1);

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
