package com.internal.bootstrap.menupipeline.menumanager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.menupipeline.element.DimensionVector2;
import com.internal.bootstrap.menupipeline.element.ElementOrigin;
import com.internal.bootstrap.menupipeline.element.ElementType;
import com.internal.bootstrap.menupipeline.element.LayoutStruct;
import com.internal.bootstrap.menupipeline.element.MenuElementHandle;
import com.internal.bootstrap.menupipeline.menu.MenuHandle;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.mathematics.vectors.Vector2;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuildSystem extends SystemPackage {

    // Internal
    private SpriteManager spriteManager;

    // Global element registry: "menuName/elementId" → handle
    private Object2ObjectOpenHashMap<String, MenuElementHandle> elementRegistry;

    // Filled during assembly, flushed after all menus are processed
    private List<Runnable> deferredRefs;

    // Base \\

    @Override
    protected void get() {
        this.spriteManager = get(SpriteManager.class);
    }

    // Lifecycle \\

    void init() {
        this.elementRegistry = new Object2ObjectOpenHashMap<>();
        this.deferredRefs = new ArrayList<>();
    }

    // Entry Points \\

    MenuHandle parseAndAssemble(File file, String menuName) {
        MenuData menuData = parseMenu(file, menuName);
        return assembleMenu(menuData);
    }

    void resolveAllDeferredRefs() {
        for (Runnable resolution : deferredRefs)
            resolution.run();
        deferredRefs.clear();
    }

    // Parse Phase \\

    private MenuData parseMenu(File file, String menuName) {

        JsonObject json = JsonUtility.loadJsonObject(file);

        boolean lockInput = json.has("lock_input") && json.get("lock_input").getAsBoolean();
        boolean raycastInput = json.has("raycast_input") && json.get("raycast_input").getAsBoolean();

        ObjectArrayList<MenuElementData> elements = parseElements(json);

        MenuData menuData = create(MenuData.class);
        menuData.constructor(menuName, elements, lockInput, raycastInput);
        return menuData;
    }

    private ObjectArrayList<MenuElementData> parseElements(JsonObject json) {

        ObjectArrayList<MenuElementData> elements = new ObjectArrayList<>();

        if (!json.has("elements"))
            return elements;

        JsonArray array = json.getAsJsonArray("elements");

        for (int i = 0; i < array.size(); i++) {
            MenuElementData element = parseElement(array.get(i).getAsJsonObject());
            if (element != null)
                elements.add(element);
        }

        return elements;
    }

    private MenuElementData parseElement(JsonObject json) {

        String id = JsonUtility.validateString(json, "id");

        // Reference element
        if (json.has("ref")) {
            MenuElementData refData = create(MenuElementData.class);
            refData.constructorRef(id, json.get("ref").getAsString());
            return refData;
        }

        String type = JsonUtility.validateString(json, "type");
        ElementType elementType = parseElementType(type, id);
        String spritePath = json.has("sprite") ? json.get("sprite").getAsString() : null;
        String text = json.has("text") ? json.get("text").getAsString() : null;
        LayoutStruct layout = parseLayout(json);

        String actionClass = null;
        String actionMethod = null;
        String actionArg = null;

        if (json.has("on_click")) {
            JsonObject clickJson = json.getAsJsonObject("on_click");
            actionClass = JsonUtility.validateString(clickJson, "class");
            actionMethod = JsonUtility.validateString(clickJson, "method");
            actionArg = clickJson.has("arg") ? clickJson.get("arg").getAsString() : null;
        }

        // Recursive — containers define their children inline
        ObjectArrayList<MenuElementData> children = parseElements(json);

        MenuElementData elementData = create(MenuElementData.class);
        elementData.constructor(id, elementType, spritePath, text, layout,
                actionClass, actionMethod, actionArg, children);
        return elementData;
    }

    private ElementType parseElementType(String type, String id) {
        return switch (type.toLowerCase()) {
            case "sprite" -> ElementType.SPRITE;
            case "texture" -> ElementType.TEXTURE;
            case "button" -> ElementType.BUTTON;
            case "label" -> ElementType.LABEL;
            case "container" -> ElementType.CONTAINER;
            default -> {
                throwException("Unknown element type '" + type + "' on element '" + id + "'");
                yield null;
            }
        };
    }

    // Layout \\

    private LayoutStruct parseLayout(JsonObject json) {

        Vector2 anchor = parseOriginField(json, "anchor");
        Vector2 pivot = parseOriginField(json, "pivot");
        DimensionVector2 position = DimensionVector2.parse(json, "position", "0%", "0%");
        DimensionVector2 size = DimensionVector2.parse(json, "size", "10%", "10%");
        DimensionVector2 minSize = json.has("min_size") ? DimensionVector2.parse(json, "min_size", "0%", "0%") : null;
        DimensionVector2 maxSize = json.has("max_size") ? DimensionVector2.parse(json, "max_size", "100%", "100%")
                : null;

        return new LayoutStruct(anchor, pivot, position, size, minSize, maxSize);
    }

    private Vector2 parseOriginField(JsonObject json, String key) {

        if (!json.has(key))
            return new Vector2(0f, 0f);

        JsonElement el = json.get(key);

        if (el.isJsonPrimitive()) {
            ElementOrigin o = ElementOrigin.fromString(el.getAsString());
            return new Vector2(o.x, o.y);
        }

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            float x = obj.has("x") ? obj.get("x").getAsFloat() : 0f;
            float y = obj.has("y") ? obj.get("y").getAsFloat() : 0f;
            return new Vector2(x, y);
        }

        return new Vector2(0f, 0f);
    }

    // Assembly Phase \\

    private MenuHandle assembleMenu(MenuData menuData) {

        ObjectArrayList<MenuElementHandle> elements = new ObjectArrayList<>();
        assembleElementList(menuData.getName(), menuData.getElements(), elements);

        MenuHandle menuHandle = create(MenuHandle.class);
        menuHandle.constructor(menuData.getName(), elements, menuData.isLockInput(), menuData.isRaycastInput());
        return menuHandle;
    }

    private void assembleElementList(
            String menuName,
            ObjectArrayList<MenuElementData> dataList,
            ObjectArrayList<MenuElementHandle> targetList) {

        for (MenuElementData data : dataList) {
            if (data.isRef())
                assembleRef(data.getId(), data.getRefPath(), targetList);
            else
                targetList.add(assembleRealElement(menuName, data));
        }
    }

    private MenuElementHandle assembleRealElement(String menuName, MenuElementData data) {

        SpriteHandle spriteHandle = resolveSpriteHandle(data);
        Runnable clickAction = resolveClickAction(data);

        ObjectArrayList<MenuElementHandle> children = new ObjectArrayList<>();
        assembleElementList(menuName, data.getChildren(), children);

        MenuElementHandle handle = create(MenuElementHandle.class);
        handle.constructor(
                data.getId(),
                data.getType(),
                spriteHandle,
                data.getText(),
                data.getLayout(),
                clickAction,
                children);

        elementRegistry.put(menuName + "/" + data.getId(), handle);

        return handle;
    }

    private void assembleRef(String localId, String refPath, ObjectArrayList<MenuElementHandle> targetList) {

        MenuElementHandle resolved = elementRegistry.get(refPath);

        if (resolved != null) {
            targetList.add(cloneHandle(resolved));
            return;
        }

        // Reserve the slot — filled after all menus are assembled
        int slot = targetList.size();
        targetList.add(null);

        deferredRefs.add(() -> {
            MenuElementHandle target = elementRegistry.get(refPath);
            if (target == null)
                throwException("Unresolved element reference: '" + refPath + "' (id: '" + localId + "')");
            targetList.set(slot, cloneHandle(target));
        });
    }

    // Clone — each instance gets its own sprite clone (own material/transform
    // state)
    private MenuElementHandle cloneHandle(MenuElementHandle source) {

        SpriteHandle clonedSprite = source.getSpriteHandle() != null
                ? spriteManager.cloneSprite(source.getSpriteHandle().getName())
                : null;

        MenuElementHandle clone = create(MenuElementHandle.class);
        clone.constructor(
                source.getId(),
                source.getType(),
                clonedSprite,
                source.getText(),
                source.getLayout(),
                source.getClickAction(),
                source.getChildren());
        return clone;
    }

    // Sprite Resolution \\

    private SpriteHandle resolveSpriteHandle(MenuElementData data) {

        if (data.getSpritePath() == null)
            return null;

        if (!spriteManager.hasSprite(data.getSpritePath()))
            throwException("Sprite not found for element '" + data.getId()
                    + "': '" + data.getSpritePath() + "'");

        return spriteManager.cloneSprite(data.getSpritePath());
    }

    // Click Action Resolution \\

    private Runnable resolveClickAction(MenuElementData data) {

        if (data.getActionClass() == null)
            return null;

        Object target = resolveTarget(data.getActionClass(), data.getActionMethod());
        Method method = resolveMethod(target, data.getActionClass(), data.getActionMethod(), data.getActionArg());

        if (data.getActionArg() != null) {
            String capturedArg = data.getActionArg();
            return () -> {
                try {
                    method.invoke(target, capturedArg);
                } catch (Exception e) {
                    throwException("Button action failed: " + data.getActionMethod(), e);
                }
            };
        }

        return () -> {
            try {
                method.invoke(target);
            } catch (Exception e) {
                throwException("Button action failed: " + data.getActionMethod(), e);
            }
        };
    }

    private Object resolveTarget(String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className);
            Object target = internal.getUnchecked(clazz);
            if (target == null)
                throwException("on_click class not registered: '" + className + "' (method: '" + methodName + "')");
            return target;
        } catch (ClassNotFoundException e) {
            throwException("on_click class not found: '" + className + "' (method: '" + methodName + "')", e);
            return null;
        }
    }

    private Method resolveMethod(Object target, String className, String methodName, String arg) {
        try {
            return arg != null
                    ? target.getClass().getMethod(methodName, String.class)
                    : target.getClass().getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throwException("on_click method not found: '" + methodName + "' on '" + className + "'", e);
            return null;
        }
    }
}