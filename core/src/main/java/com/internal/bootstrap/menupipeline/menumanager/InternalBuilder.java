package com.internal.bootstrap.menupipeline.menumanager;

import java.io.File;
import java.lang.reflect.Method;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.menupipeline.element.ElementData;
import com.internal.bootstrap.menupipeline.element.ElementHandle;
import com.internal.bootstrap.menupipeline.element.ElementOverrideStruct;
import com.internal.bootstrap.menupipeline.element.ElementPlacementHandle;
import com.internal.bootstrap.menupipeline.element.ElementType;
import com.internal.bootstrap.menupipeline.element.LayoutStruct;
import com.internal.bootstrap.menupipeline.element.MenuAwareAction;
import com.internal.bootstrap.menupipeline.elementsystem.ElementSystem;
import com.internal.bootstrap.menupipeline.menu.MenuData;
import com.internal.bootstrap.menupipeline.menu.MenuHandle;
import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.JsonUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/*
 * Parses menu JSON files into MenuHandles and ElementHandles during bootstrap.
 *
 * Color JSON field: "color": [r, g, b, a] — floats 0.0–1.0 on any LABEL element.
 * If omitted the FontInstance defaults to white (1, 1, 1, 1).
 */
class InternalBuilder extends BuilderPackage {

    private static final String PARENT_ARG = "$parent";

    // Internal
    private SpriteManager spriteManager;
    private ElementSystem elementSystem;
    private File root;
    private ObjectOpenHashSet<String> registeredFiles;
    private ObjectArrayList<Runnable> deferredRefs;

    // Base \\

    @Override
    protected void get() {
        this.spriteManager = get(SpriteManager.class);
        this.elementSystem = get(ElementSystem.class);
    }

    void init(File root) {
        this.root = root;
        this.registeredFiles = new ObjectOpenHashSet<>();
        this.deferredRefs = new ObjectArrayList<>();
    }

    // Entry Point \\

    ObjectArrayList<MenuHandle> processFile(File file, String filePath) {

        JsonObject json = JsonUtility.loadJsonObject(file);

        if (!registeredFiles.contains(filePath)) {
            if (elementSystem.isFileLoading(filePath))
                throwException("Circular file dependency at: '" + filePath + "'");
            elementSystem.beginFileLoad(filePath);
            registeredFiles.add(filePath);
            try {
                registerTopLevelMasters(filePath, parseElements(json));
            } finally {
                elementSystem.endFileLoad(filePath);
            }
        }

        String fileName = filePath.contains("/")
                ? filePath.substring(filePath.lastIndexOf('/') + 1)
                : filePath;

        ObjectArrayList<MenuHandle> handles = new ObjectArrayList<>();
        for (MenuData decl : parseMenus(json)) {
            ObjectArrayList<ElementPlacementHandle> placements = new ObjectArrayList<>();
            for (ElementData data : decl.getElements())
                placements.add(buildPlacement(filePath, data));
            MenuHandle handle = create(MenuHandle.class);
            handle.constructor(fileName + "/" + decl.getName(),
                    placements,
                    decl.isLockInput(),
                    decl.isRaycastInput());
            handles.add(handle);
        }

        return handles;
    }

    void resolveAllDeferredRefs() {
        for (Runnable r : deferredRefs)
            r.run();
        deferredRefs.clear();
    }

    // Parsing — Menus \\

    private ObjectArrayList<MenuData> parseMenus(JsonObject json) {

        ObjectArrayList<MenuData> menus = new ObjectArrayList<>();

        if (!json.has("menus"))
            return menus;

        JsonArray array = json.getAsJsonArray("menus");
        for (int i = 0; i < array.size(); i++) {
            JsonObject menuJson = array.get(i).getAsJsonObject();
            String id = JsonUtility.validateString(menuJson, "id");
            boolean lockInput = JsonUtility.getBoolean(menuJson, "lock_input", false);
            boolean raycastInput = JsonUtility.getBoolean(menuJson, "raycast_input", false);
            ObjectArrayList<ElementData> elements = parseElements(menuJson);
            MenuData data = create(MenuData.class);
            data.constructor(id, elements, raycastInput, lockInput);
            menus.add(data);
        }

        return menus;
    }

    // Parsing — Elements \\

    private ObjectArrayList<ElementData> parseElements(JsonObject json) {

        ObjectArrayList<ElementData> elements = new ObjectArrayList<>();

        if (!json.has("elements"))
            return elements;

        JsonArray array = json.getAsJsonArray("elements");
        for (int i = 0; i < array.size(); i++) {
            ElementData element = parseElement(array.get(i).getAsJsonObject());
            if (element != null)
                elements.add(element);
        }

        return elements;
    }

    private ElementData parseElement(JsonObject json) {
        String id = JsonUtility.validateString(json, "id");
        if (json.has("ref"))
            return parseRefElement(id, json);
        if (json.has("use"))
            return parseUseElement(id, json);
        return parseInlineElement(id, json);
    }

    private ElementData parseRefElement(String id, JsonObject json) {
        LayoutStruct layout = FileParserUtility.parseLayoutOverride(json);
        ElementData data = create(ElementData.class);
        data.constructorRef(id, json.get("ref").getAsString(), layout);
        return data;
    }

    private ElementData parseUseElement(String id, JsonObject json) {
        String usePath = json.get("use").getAsString();
        String spritePath = JsonUtility.getString(json, "sprite", null);
        String text = JsonUtility.getString(json, "text", null);
        String fontName = JsonUtility.getString(json, "font", null);
        float[] color = parseColor(json);
        LayoutStruct layout = FileParserUtility.parseLayoutOverride(json);
        String[] onClick = FileParserUtility.parseOnClick(json);
        ObjectArrayList<ElementData> children = parseElements(json);
        ElementData data = create(ElementData.class);
        data.constructorUse(id, usePath, spritePath, text, fontName, color, layout,
                onClick != null ? onClick[0] : null,
                onClick != null ? onClick[1] : null,
                onClick != null ? onClick[2] : null,
                children);
        return data;
    }

    private ElementData parseInlineElement(String id, JsonObject json) {
        ElementType elementType = FileParserUtility.parseElementType(
                JsonUtility.validateString(json, "type"), id);
        String spritePath = JsonUtility.getString(json, "sprite", null);
        String text = JsonUtility.getString(json, "text", null);
        String fontName = parseFontName(json, elementType, id);
        float[] color = elementType == ElementType.LABEL ? parseColor(json) : null;
        LayoutStruct layout = FileParserUtility.parseLayout(json);
        String[] onClick = FileParserUtility.parseOnClick(json);
        ObjectArrayList<ElementData> children = parseElements(json);
        ElementData data = create(ElementData.class);
        data.constructor(id, elementType, spritePath, text, fontName, color, layout,
                onClick != null ? onClick[0] : null,
                onClick != null ? onClick[1] : null,
                onClick != null ? onClick[2] : null,
                children);
        return data;
    }

    // Font / Color Parsing \\

    private String parseFontName(JsonObject json, ElementType type, String elementId) {
        String fontName = JsonUtility.getString(json, "font", null);
        if (fontName == null)
            return null;
        if (type != ElementType.LABEL)
            throwException("Element '" + elementId + "' has 'font' field but is not type LABEL");
        return fontName;
    }

    /*
     * Parses "color": [r, g, b, a] from JSON. Returns null if field absent —
     * FontInstance will default to white. Throws if the array is not exactly
     * 4 elements.
     */
    private float[] parseColor(JsonObject json) {
        if (!json.has("color"))
            return null;
        JsonArray arr = json.getAsJsonArray("color");
        if (arr.size() != 4)
            throwException("'color' field must be an array of exactly 4 floats [r, g, b, a]");
        return new float[] {
                arr.get(0).getAsFloat(),
                arr.get(1).getAsFloat(),
                arr.get(2).getAsFloat(),
                arr.get(3).getAsFloat()
        };
    }

    // Master Registration \\

    private void registerTopLevelMasters(
            String filePath, ObjectArrayList<ElementData> elements) {
        for (ElementData data : elements)
            if (!data.isRef() && !data.isUse())
                elementSystem.registerMaster(filePath + "/" + data.getId(),
                        buildMaster(filePath, data));
    }

    private ElementHandle buildMaster(String filePath, ElementData data) {
        Object action = resolveClickActionRaw(data);
        ElementHandle master = create(ElementHandle.class);
        master.constructor(
                data.getId(), data.getType(),
                resolveSpriteName(data),
                data.getText(),
                data.getFontName(),
                data.getColor(),
                data.getLayout(),
                action instanceof Runnable r ? r : null,
                action instanceof MenuAwareAction m ? m : null,
                buildChildPlacements(filePath, data.getChildren()));
        return master;
    }

    // Placement \\

    private ElementPlacementHandle buildPlacement(String filePath, ElementData data) {
        if (data.isRef())
            return buildRefPlacement(filePath, data);
        if (data.isUse())
            return buildUsePlacement(filePath, data);
        return buildInlinePlacement(filePath, data);
    }

    private ObjectArrayList<ElementPlacementHandle> buildChildPlacements(
            String filePath, ObjectArrayList<ElementData> children) {
        ObjectArrayList<ElementPlacementHandle> placements = new ObjectArrayList<>(children.size());
        for (ElementData child : children)
            placements.add(buildPlacement(filePath, child));
        return placements;
    }

    private ElementPlacementHandle buildInlinePlacement(String filePath, ElementData data) {
        String key = filePath + "/" + data.getId();
        ElementHandle master = elementSystem.getMaster(key);
        if (master == null) {
            master = buildMaster(filePath, data);
            elementSystem.registerMaster(key, master);
        }
        ElementPlacementHandle placement = create(ElementPlacementHandle.class);
        placement.constructor(master, null);
        return placement;
    }

    private ElementPlacementHandle buildUsePlacement(String filePath, ElementData data) {

        ElementHandle template = resolveTemplate(data.getUsePath(), data.getId());

        ElementHandle master;
        if (!data.getChildren().isEmpty()) {
            master = create(ElementHandle.class);
            master.constructor(data.getId(), template.getType(),
                    template.getSpriteName(), template.getText(),
                    template.getFontName(), template.getColor(),
                    template.getLayout(), template.getClickAction(),
                    template.getMenuAwareAction(),
                    buildChildPlacements(filePath, data.getChildren()));
            elementSystem.registerMaster(filePath + "/" + data.getId(), master);
        } else {
            master = template;
        }

        LayoutStruct layoutOverride = data.getLayout() != null
                ? LayoutStruct.merge(template.getLayout(), data.getLayout())
                : null;
        String spriteNameOverride = data.getSpritePath() != null
                ? resolveSpriteName(data)
                : null;

        Runnable clickOverride = null;
        MenuAwareAction maaOverride = null;
        if (data.getActionClass() != null) {
            Object action = resolveClickActionRaw(data);
            clickOverride = action instanceof Runnable r ? r : null;
            maaOverride = action instanceof MenuAwareAction m ? m : null;
        }

        // Color override — use data color if specified
        float[] colorOverride = data.hasColor() ? data.getColor() : null;

        boolean hasOverride = layoutOverride != null || spriteNameOverride != null
                || data.getText() != null || clickOverride != null
                || maaOverride != null || colorOverride != null;

        ElementPlacementHandle placement = create(ElementPlacementHandle.class);
        placement.constructor(master, hasOverride
                ? new ElementOverrideStruct(spriteNameOverride, data.getText(),
                        colorOverride, clickOverride, maaOverride, layoutOverride)
                : null);
        return placement;
    }

    private ElementPlacementHandle buildRefPlacement(String filePath, ElementData data) {

        ElementOverrideStruct override = data.getLayout() != null
                ? new ElementOverrideStruct(null, null, null, null, null, data.getLayout())
                : null;

        ElementHandle resolved = resolveRefKey(data.getRefPath());

        if (resolved != null) {
            ElementPlacementHandle placement = create(ElementPlacementHandle.class);
            placement.constructor(resolved, override);
            return placement;
        }

        ElementPlacementHandle placeholder = create(ElementPlacementHandle.class);
        placeholder.constructor(null, override);

        final String finalRefKey = data.getRefPath();
        final String finalId = data.getId();
        deferredRefs.add(() -> {
            ElementHandle target = resolveRefKey(finalRefKey);
            if (target == null)
                throwException("Unresolved ref: '" + finalRefKey + "' (id: '" + finalId + "')");
            placeholder.setMaster(target);
        });

        return placeholder;
    }

    // Ref Key Resolution \\

    private ElementHandle resolveRefKey(String refKey) {
        ElementHandle resolved = elementSystem.getMaster(refKey);
        if (resolved != null)
            return resolved;
        String suffix = "/" + refKey;
        for (String key : elementSystem.getMasterKeys())
            if (key.endsWith(suffix))
                return elementSystem.getMaster(key);
        return null;
    }

    // Template Resolution \\

    private ElementHandle resolveTemplate(String usePath, String localId) {

        String candidateKey = usePath + "/" + localId;
        if (elementSystem.hasMaster(candidateKey))
            return elementSystem.getMaster(candidateKey);

        File fileByPath = tryResolveFile(usePath);
        if (fileByPath != null) {
            processFile(fileByPath, usePath);
            ElementHandle master = elementSystem.getMaster(candidateKey);
            if (master == null)
                throwException("Element '" + localId + "' not found in file '" + usePath + "'");
            return master;
        }

        if (elementSystem.hasMaster(usePath))
            return elementSystem.getMaster(usePath);

        int lastSlash = usePath.lastIndexOf('/');
        if (lastSlash < 0)
            throwException("Cannot resolve use path '" + usePath
                    + "' — no file found and path has no slash.");

        String filePath = usePath.substring(0, lastSlash);
        processFile(resolveFile(filePath), filePath);

        ElementHandle master = elementSystem.getMaster(usePath);
        if (master == null)
            throwException("Element '" + usePath + "' not found after loading '" + filePath + "'");
        return master;
    }

    private File tryResolveFile(String filePath) {
        for (String ext : EngineSetting.JSON_FILE_EXTENSIONS) {
            File f = new File(root, filePath + (ext.startsWith(".") ? "" : ".") + ext);
            if (f.exists())
                return f;
        }
        return null;
    }

    private File resolveFile(String filePath) {
        File f = tryResolveFile(filePath);
        if (f == null)
            throwException("File not found: '" + filePath + "' (root: " + root.getAbsolutePath() + ")");
        return f;
    }

    // Sprite Resolution \\

    private String resolveSpriteName(ElementData data) {
        if (data.getSpritePath() == null)
            return null;
        if (!spriteManager.hasSprite(data.getSpritePath()))
            throwException("Sprite not found for element '" + data.getId()
                    + "': '" + data.getSpritePath() + "'");
        return data.getSpritePath();
    }

    // Click Action Resolution \\

    private Object resolveClickActionRaw(ElementData data) {

        if (data.getActionClass() == null)
            return null;

        Object target = resolveTarget(data.getActionClass(), data.getActionMethod());

        if (PARENT_ARG.equals(data.getActionArg())) {
            Method method;
            try {
                method = target.getClass().getMethod(data.getActionMethod(), MenuInstance.class);
            } catch (NoSuchMethodException e) {
                throwException("$parent method '" + data.getActionMethod()
                        + "' must accept MenuInstance on '" + data.getActionClass() + "'", e);
                return null;
            }
            return (MenuAwareAction) parent -> {
                try {
                    method.invoke(target, parent);
                } catch (Exception e) {
                    throwException("Button action failed: " + data.getActionMethod(), e);
                }
            };
        }

        Method method = resolveMethod(target, data.getActionClass(),
                data.getActionMethod(), data.getActionArg());

        if (data.getActionArg() != null) {
            String capturedArg = data.getActionArg();
            return (Runnable) () -> {
                try {
                    method.invoke(target, capturedArg);
                } catch (Exception e) {
                    throwException("Button action failed: " + data.getActionMethod(), e);
                }
            };
        }

        return (Runnable) () -> {
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
                throwException("on_click class not registered: '" + className
                        + "' (method: '" + methodName + "')");
            return target;
        } catch (ClassNotFoundException e) {
            throwException("on_click class not found: '" + className
                    + "' (method: '" + methodName + "')", e);
            return null;
        }
    }

    private Method resolveMethod(Object target, String className, String methodName, String arg) {
        try {
            return arg != null
                    ? target.getClass().getMethod(methodName, String.class)
                    : target.getClass().getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throwException("on_click method not found: '" + methodName
                    + "' on '" + className + "'", e);
            return null;
        }
    }
}