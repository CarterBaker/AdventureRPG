package application.bootstrap.menupipeline.elementhitsystem;

import java.lang.reflect.Method;

import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.util.MenuAwareAction;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.ScreenRayStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ElementHitSystem extends SystemPackage {

    private static final String PARENT_ARG = "$parent";

    private RaycastManager raycastManager;
    private InputSystem inputSystem;
    private boolean wasPressed;
    private ElementInstance openExpandable;
    private float collapseTolerance;
    private Object2ObjectOpenHashMap<String, Runnable> resolvedActions;
    private Object2ObjectOpenHashMap<String, MenuAwareAction> resolvedMenuAwareActions;

    @Override
    protected void create() {
        this.collapseTolerance = EngineSetting.DROPDOWN_COLLAPSE_TOLERANCE;
    }

    @Override
    protected void get() {
        this.raycastManager = get(RaycastManager.class);
        this.inputSystem = get(InputSystem.class);
        this.resolvedActions = new Object2ObjectOpenHashMap<>();
        this.resolvedMenuAwareActions = new Object2ObjectOpenHashMap<>();
    }

    public void updateRaycast(ObjectArrayList<MenuInstance> activeMenus) {
        if (!raycastManager.hasScreenRay()) return;
        ScreenRayStruct ray = raycastManager.getScreenRay();
        int rayWindowID = ray.getWindowID();
        WindowInstance rayWindow = null;
        for (int i = 0; i < activeMenus.size(); i++) {
            WindowInstance candidate = activeMenus.get(i).getWindow();
            if (candidate.getWindowID() != rayWindowID) continue;
            rayWindow = candidate;
            break;
        }
        if (rayWindow == null) return;

        float mouseX = ray.getScreenX();
        float adjustedMouseY = rayWindow.getHeight() - ray.getScreenY();
        checkDropdownCollapse(mouseX, adjustedMouseY);
        boolean pressed = inputSystem.bindingClicked(KeyBindings.PRIMARY);
        boolean clicked = pressed && !wasPressed;
        wasPressed = pressed;
        if (!clicked) return;

        for (int i = activeMenus.size() - 1; i >= 0; i--) {
            MenuInstance instance = activeMenus.get(i);
            if (!instance.isVisible()) continue;
            WindowInstance window = instance.getWindow();
            if (window.getWindowID() != rayWindowID) continue;
            if (hitTestElements(instance.getElements(), mouseX, adjustedMouseY, 0, 0, window.getWidth(), window.getHeight(), instance)) return;
        }
    }

    public void resetPressed() { this.wasPressed = false; this.openExpandable = null; }

    private void checkDropdownCollapse(float mouseX, float mouseY) {
        if (openExpandable == null) return;
        float left = openExpandable.getComputedLeft() - collapseTolerance;
        float right = openExpandable.getComputedLeft() + openExpandable.getComputedW() + collapseTolerance;
        float bottom = openExpandable.getComputedTop() + openExpandable.getComputedH() + collapseTolerance;
        float top = openExpandable.getComputedTop() - openExpandable.getContentH() - collapseTolerance;
        if (mouseX < left || mouseX > right || mouseY < top || mouseY > bottom) collapseOpen();
    }

    private void collapseOpen() { openExpandable.toggleExpanded(); openExpandable = null; }

    private boolean hitTestElements(ObjectArrayList<ElementInstance> elements, float mouseX, float mouseY, float clipLeft, float clipTop, float clipRight, float clipBottom, MenuInstance menu) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            ElementInstance element = elements.get(i);
            ElementData data = element.getElementData();
            ElementType type = data.getType();
            if (element.hasChildren()) {
                boolean traverseChildren = type != ElementType.EXPANDABLE_CONTAINER || element.isExpanded();
                if (traverseChildren) {
                    float cl = clipLeft, ct = clipTop, cr = clipRight, cb = clipBottom;
                    if (data.isMask()) {
                        cl = Math.max(cl, element.getComputedLeft());
                        ct = Math.max(ct, element.getComputedTop());
                        cr = Math.min(cr, element.getComputedLeft() + element.getComputedW());
                        cb = Math.min(cb, element.getComputedTop() + element.getComputedH());
                    }
                    if (hitTestElements(element.getChildren(), mouseX, mouseY, cl, ct, cr, cb, menu)) return true;
                }
            }
            if (type != ElementType.BUTTON && type != ElementType.EXPANDABLE_CONTAINER) continue;
            if (mouseX < clipLeft || mouseX > clipRight || mouseY < clipTop || mouseY > clipBottom) continue;
            if (!isHit(element, mouseX, mouseY)) continue;
            if (type == ElementType.EXPANDABLE_CONTAINER) { toggleExpandable(element); return true; }
            executeAction(element, menu);
            return true;
        }
        return false;
    }

    private void executeAction(ElementInstance element, MenuInstance parent) {
        if (!element.hasAction()) return;
        String actionClass = element.getEffectiveActionClass();
        String actionMethod = element.getEffectiveActionMethod();
        String actionArg = element.getEffectiveActionArg();
        String key = actionClass + "#" + actionMethod + "#" + actionArg;
        if (PARENT_ARG.equals(actionArg)) {
            MenuAwareAction action = resolvedMenuAwareActions.get(key);
            if (action == null) {
                action = resolveMenuAwareAction(actionClass, actionMethod);
                resolvedMenuAwareActions.put(key, action);
            }
            action.execute(parent);
            return;
        }
        Runnable action = resolvedActions.get(key);
        if (action == null) {
            action = resolveRunnableAction(actionClass, actionMethod, actionArg);
            resolvedActions.put(key, action);
        }
        action.run();
    }

    private Runnable resolveRunnableAction(String actionClass, String actionMethod, String actionArg) {
        try {
            Class<?> clazz = Class.forName(data.getActionClass());
            Object target = internal.getUnchecked(clazz);
            if (target == null) throwException("on_click class not registered: '" + data.getActionClass() + "'");
            Method method = data.getActionArg() != null ? target.getClass().getMethod(data.getActionMethod(), String.class) : target.getClass().getMethod(data.getActionMethod());
            String arg = data.getActionArg();
            if (arg != null) return () -> invoke(method, target, arg);
            return () -> invoke(method, target);
        } catch (Exception e) {
            throwException("Failed to resolve button action: " + actionClass + "#" + actionMethod, e);
            return null;
        }
    }

    private MenuAwareAction resolveMenuAwareAction(String actionClass, String actionMethod) {
        try {
            Class<?> clazz = Class.forName(data.getActionClass());
            Object target = internal.getUnchecked(clazz);
            if (target == null) throwException("on_click class not registered: '" + data.getActionClass() + "'");
            Method method = target.getClass().getMethod(data.getActionMethod(), MenuInstance.class);
            return parent -> invoke(method, target, parent);
        } catch (Exception e) {
            throwException("Failed to resolve menu-aware action: " + actionClass + "#" + actionMethod, e);
            return null;
        }
    }

    private void invoke(Method method, Object target, Object... args) {
        try { method.invoke(target, args); } catch (Exception e) { throwException("Button action failed: " + method.getName(), e); }
    }

    private void toggleExpandable(ElementInstance element) { if (openExpandable != null && openExpandable != element) collapseOpen(); element.toggleExpanded(); openExpandable = element.isExpanded() ? element : null; }
    private boolean isHit(ElementInstance element, float mouseX, float mouseY) { float left = element.getComputedLeft(); float top = element.getComputedTop(); float right = left + element.getComputedW(); float bottom = top + element.getComputedH(); return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom; }
}
