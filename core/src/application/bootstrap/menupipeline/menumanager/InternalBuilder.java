package application.bootstrap.menupipeline.menumanager;

import java.io.File;
import java.lang.reflect.Method;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementOrigin;
import application.bootstrap.menupipeline.element.ElementPlacementStruct;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.elementsystem.ElementSystem;
import application.bootstrap.menupipeline.menu.MenuData;
import application.bootstrap.menupipeline.menu.MenuHandle;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.util.DimensionValue;
import application.bootstrap.menupipeline.util.DimensionVector2;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.MenuAwareAction;
import application.bootstrap.menupipeline.util.StackDirection;
import application.bootstrap.menupipeline.util.TextAlign;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses menu JSON files into MenuHandles and ElementHandles during bootstrap.
     * Builds all handles directly from JSON locals — no intermediate parse structs.
     * Deferred ref resolution runs after all files are processed.
     */

    private static final String PARENT_ARG = "$parent";

    // Internal
    private SpriteManager spriteManager;
    private ElementSystem elementSystem;
    private File root;
    private ObjectOpenHashSet<String> registeredFiles;
    private ObjectArrayList<Runnable> deferredRefs;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.spriteManager = get(SpriteManager.class);
        this.elementSystem = get(ElementSystem.class);
    }

    void init(File root) {

        // Internal
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
                registerTopLevelMasters(filePath, json);
            } finally {
                elementSystem.endFileLoad(filePath);
            }
        }

        String fileName = filePath.contains("/")
                ? filePath.substring(filePath.lastIndexOf('/') + 1)
                : filePath;

        ObjectArrayList<MenuHandle> handles = new ObjectArrayList<>();

        if (!json.has("menus"))
            return handles;

        JsonArray menuArray = json.getAsJsonArray("menus");

        for (int i = 0; i < menuArray.size(); i++)
            handles.add(buildMenuHandle(fileName, filePath, menuArray.get(i).getAsJsonObject()));

        return handles;
    }

    void resolveAllDeferredRefs() {
        for (int i = 0; i < deferredRefs.size(); i++)
            deferredRefs.get(i).run();
        deferredRefs.clear();
    }

    // Menu Building \\

    private MenuHandle buildMenuHandle(
            String fileName,
            String filePath,
            JsonObject menuJson) {

        String id = JsonUtility.validateString(menuJson, "id");
        boolean lockInput = JsonUtility.getBoolean(menuJson, "lock_input", false);
        boolean raycastInput = JsonUtility.getBoolean(menuJson, "raycast_input", false);

        ObjectArrayList<String> entryPoints = new ObjectArrayList<>();

        if (menuJson.has("entry_points")) {
            JsonArray eps = menuJson.getAsJsonArray("entry_points");
            for (int i = 0; i < eps.size(); i++)
                entryPoints.add(eps.get(i).getAsString());
        }

        ObjectArrayList<ElementPlacementStruct> placements = buildPlacements(
                filePath,
                menuJson,
                null,
                DimensionValue.parse(EngineSetting.FONT_DEFAULT_SIZE_PERCENT),
                true);

        MenuData data = new MenuData(fileName + "/" + id, lockInput, raycastInput, entryPoints);
        MenuHandle handle = create(MenuHandle.class);
        handle.constructor(data, placements);

        return handle;
    }

    // Top-Level Master Registration \\

    private void registerTopLevelMasters(String filePath, JsonObject json) {

        if (!json.has("elements"))
            return;

        JsonArray elements = json.getAsJsonArray("elements");

        for (int i = 0; i < elements.size(); i++) {

            JsonObject el = elements.get(i).getAsJsonObject();

            if (el.has("ref") || el.has("use"))
                continue;

            String id = JsonUtility.validateString(el, "id");
            String key = filePath + "/" + id;

            if (!elementSystem.hasMaster(key))
                elementSystem.registerMaster(
                        key,
                        buildMasterFromJson(
                                filePath,
                                id,
                                el,
                                null,
                                DimensionValue.parse(EngineSetting.FONT_DEFAULT_SIZE_PERCENT),
                                true));
        }
    }

    // Placement Building \\

    private ObjectArrayList<ElementPlacementStruct> buildPlacements(
            String filePath,
            JsonObject parent,
            String inheritedFontName,
            DimensionValue inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        if (!parent.has("elements"))
            return new ObjectArrayList<>();

        JsonArray array = parent.getAsJsonArray("elements");
        ObjectArrayList<ElementPlacementStruct> placements = new ObjectArrayList<>(array.size());

        for (int i = 0; i < array.size(); i++)
            placements.add(buildPlacement(
                    filePath,
                    array.get(i).getAsJsonObject(),
                    inheritedFontName,
                    inheritedFontSize,
                    inheritedExplicitFontSize));

        return placements;
    }

    private ElementPlacementStruct buildPlacement(
            String filePath,
            JsonObject json,
            String inheritedFontName,
            DimensionValue inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        String id = JsonUtility.validateString(json, "id");

        if (json.has("ref"))
            return buildRefPlacement(filePath, id, json);

        if (json.has("use"))
            return buildUsePlacement(filePath, id, json, inheritedFontName, inheritedFontSize,
                    inheritedExplicitFontSize);

        return buildInlinePlacement(filePath, id, json, inheritedFontName, inheritedFontSize,
                inheritedExplicitFontSize);
    }

    private ElementPlacementStruct buildInlinePlacement(
            String filePath,
            String id,
            JsonObject json,
            String inheritedFontName,
            DimensionValue inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        String key = filePath + "/" + id;
        ElementHandle master = elementSystem.getMaster(key);

        if (master == null) {
            master = buildMasterFromJson(filePath, id, json, inheritedFontName, inheritedFontSize,
                    inheritedExplicitFontSize);
            elementSystem.registerMaster(key, master);
        }

        return new ElementPlacementStruct(master);
    }

    private ElementPlacementStruct buildUsePlacement(
            String filePath,
            String id,
            JsonObject json,
            String inheritedFontName,
            DimensionValue inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        String usePath = json.get("use").getAsString();
        ElementHandle template = resolveTemplate(usePath, id);

        boolean explicitFontSize = json.has("font_size") || template.hasExplicitFontSize();
        String resolvedFontName = JsonUtility.getString(json, "font", template.getFontName());
        DimensionValue resolvedFontSize = json.has("font_size")
                ? DimensionValue.parse(json.get("font_size").getAsString())
                : template.getFontSize();

        ObjectArrayList<ElementPlacementStruct> children = buildPlacements(
                filePath,
                json,
                resolvedFontName,
                resolvedFontSize,
                explicitFontSize);

        ElementHandle master;

        if (!children.isEmpty()) {

            ElementData data = new ElementData(
                    id,
                    template.getType(),
                    template.getSpriteName(),
                    template.getText(),
                    resolvedFontName,
                    resolvedFontSize,
                    explicitFontSize,
                    template.getColor(),
                    template.getLayout(),
                    template.isMask(),
                    template.getStackDirection(),
                    template.getSpacing(),
                    template.getTextAlign());

            master = create(ElementHandle.class);
            master.constructor(
                    data,
                    template.getClickAction(),
                    template.getMenuAwareAction(),
                    children);
        } else {
            master = template;
        }

        LayoutStruct partialOverride = parseLayoutOverride(json);
        LayoutStruct layoutOverride = partialOverride != null
                ? LayoutStruct.merge(template.getLayout(), partialOverride)
                : null;

        String spritePath = JsonUtility.getString(json, "sprite", null);
        String spriteNameOverride = spritePath != null ? resolveSpriteName(id, spritePath) : null;
        String textOverride = JsonUtility.getString(json, "text", null);
        float[] colorOverride = json.has("color") ? parseColor(json) : null;

        String[] onClick = parseOnClick(json);
        Runnable clickOverride = null;
        MenuAwareAction maaOverride = null;

        if (onClick != null) {
            Object action = resolveClickActionRaw(onClick[0], onClick[1], onClick[2]);
            clickOverride = action instanceof Runnable r ? r : null;
            maaOverride = action instanceof MenuAwareAction m ? m : null;
        }

        boolean hasOverride = layoutOverride != null || spriteNameOverride != null
                || textOverride != null || clickOverride != null
                || maaOverride != null || colorOverride != null;

        if (!hasOverride)
            return new ElementPlacementStruct(master);

        return new ElementPlacementStruct(
                master,
                spriteNameOverride,
                textOverride,
                colorOverride,
                clickOverride,
                maaOverride,
                layoutOverride);
    }

    private ElementPlacementStruct buildRefPlacement(
            String filePath,
            String id,
            JsonObject json) {

        String refKey = json.get("ref").getAsString();
        LayoutStruct layoutOverride = parseLayoutOverride(json);
        ElementHandle resolved = resolveRefKey(refKey);

        if (resolved != null)
            return layoutOverride != null
                    ? new ElementPlacementStruct(resolved, null, null, null, null, null, layoutOverride)
                    : new ElementPlacementStruct(resolved);

        ElementPlacementStruct placeholder = layoutOverride != null
                ? new ElementPlacementStruct(null, null, null, null, null, null, layoutOverride)
                : new ElementPlacementStruct(null);

        deferredRefs.add(() -> {
            ElementHandle target = resolveRefKey(refKey);
            if (target == null)
                throwException("Unresolved ref: '" + refKey + "' (id: '" + id + "')");
            placeholder.setMaster(target);
        });

        return placeholder;
    }

    // Master Building \\

    private ElementHandle buildMasterFromJson(
            String filePath,
            String id,
            JsonObject json,
            String inheritedFontName,
            DimensionValue inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        ElementType type = parseElementType(JsonUtility.validateString(json, "type"), id);
        String spritePath = JsonUtility.getString(json, "sprite", null);
        String text = JsonUtility.getString(json, "text", null);
        String fontName = JsonUtility.getString(json, "font", inheritedFontName);
        boolean explicitFontSize = json.has("font_size") || inheritedExplicitFontSize;
        DimensionValue fontSize = json.has("font_size")
                ? DimensionValue.parse(json.get("font_size").getAsString())
                : inheritedFontSize;
        float[] color = parseColor(json);
        LayoutStruct layout = parseLayout(json);
        boolean mask = JsonUtility.getBoolean(json, "mask", false);
        StackDirection stackDirection = json.has("stack")
                ? StackDirection.fromString(json.get("stack").getAsString())
                : StackDirection.NONE;
        DimensionValue spacing = json.has("spacing")
                ? DimensionValue.parse(json.get("spacing").getAsString())
                : null;
        TextAlign textAlign = json.has("align")
                ? TextAlign.fromString(json.get("align").getAsString())
                : TextAlign.CENTER;
        String spriteName = resolveSpriteName(id, spritePath);

        String[] onClick = parseOnClick(json);
        Object action = onClick != null
                ? resolveClickActionRaw(onClick[0], onClick[1], onClick[2])
                : null;

        ObjectArrayList<ElementPlacementStruct> children = buildPlacements(
                filePath,
                json,
                fontName,
                fontSize,
                explicitFontSize);

        ElementData data = new ElementData(
                id, type, spriteName, text, fontName, fontSize, explicitFontSize, color,
                layout, mask, stackDirection, spacing, textAlign);

        ElementHandle master = create(ElementHandle.class);
        master.constructor(
                data,
                action instanceof Runnable r ? r : null,
                action instanceof MenuAwareAction m ? m : null,
                children);

        return master;
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
            throwException("File not found: '" + filePath
                    + "' (root: " + root.getAbsolutePath() + ")");

        return f;
    }

    // Sprite Resolution \\

    private String resolveSpriteName(String elementId, String spritePath) {

        if (spritePath == null)
            return null;

        if (!spriteManager.hasSprite(spritePath))
            throwException("Sprite not found for element '" + elementId
                    + "': '" + spritePath + "'");

        return spritePath;
    }

    // Click Action Resolution \\

    private Object resolveClickActionRaw(
            String actionClass,
            String actionMethod,
            String actionArg) {

        Object target = resolveTarget(actionClass, actionMethod);

        if (PARENT_ARG.equals(actionArg)) {

            Method method;

            try {
                method = target.getClass().getMethod(actionMethod, MenuInstance.class);
            } catch (NoSuchMethodException e) {
                throwException("$parent method '" + actionMethod
                        + "' must accept MenuInstance on '" + actionClass + "'", e);
                return null;
            }

            return (MenuAwareAction) parent -> {
                try {
                    method.invoke(target, parent);
                } catch (Exception e) {
                    throwException("Button action failed: " + actionMethod, e);
                }
            };
        }

        Method method = resolveMethod(target, actionClass, actionMethod, actionArg);

        if (actionArg != null) {
            String capturedArg = actionArg;
            return (Runnable) () -> {
                try {
                    method.invoke(target, capturedArg);
                } catch (Exception e) {
                    throwException("Button action failed: " + actionMethod, e);
                }
            };
        }

        return (Runnable) () -> {
            try {
                method.invoke(target);
            } catch (Exception e) {
                throwException("Button action failed: " + actionMethod, e);
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

    private Method resolveMethod(
            Object target,
            String className,
            String methodName,
            String arg) {

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

    // Layout Parsing \\

    private LayoutStruct parseLayout(JsonObject json) {
        return new LayoutStruct(
                parseOriginField(json, "anchor"),
                parseOriginField(json, "pivot"),
                DimensionVector2.parse(json, "position",
                        EngineSetting.ELEMENT_DEFAULT_POSITION,
                        EngineSetting.ELEMENT_DEFAULT_POSITION),
                DimensionVector2.parse(json, "size",
                        EngineSetting.ELEMENT_DEFAULT_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_SIZE),
                json.has("min_size") ? DimensionVector2.parse(json, "min_size",
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE) : null,
                json.has("max_size") ? DimensionVector2.parse(json, "max_size",
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE) : null);
    }

    private LayoutStruct parseLayoutOverride(JsonObject json) {

        boolean hasAny = json.has("anchor") || json.has("pivot") || json.has("position")
                || json.has("size") || json.has("min_size") || json.has("max_size");

        if (!hasAny)
            return null;

        return new LayoutStruct(
                json.has("anchor") ? parseOriginField(json, "anchor") : null,
                json.has("pivot") ? parseOriginField(json, "pivot") : null,
                json.has("position") ? DimensionVector2.parse(json, "position",
                        EngineSetting.ELEMENT_DEFAULT_POSITION,
                        EngineSetting.ELEMENT_DEFAULT_POSITION) : null,
                json.has("size") ? DimensionVector2.parse(json, "size",
                        EngineSetting.ELEMENT_DEFAULT_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_SIZE) : null,
                json.has("min_size") ? DimensionVector2.parse(json, "min_size",
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE) : null,
                json.has("max_size") ? DimensionVector2.parse(json, "max_size",
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE) : null);
    }

    private Vector2 parseOriginField(JsonObject json, String key) {

        if (!json.has(key))
            return new Vector2(0f, 0f);

        JsonElement el = json.get(key);

        if (el.isJsonPrimitive()) {
            ElementOrigin o = ElementOrigin.fromString(el.getAsString());
            return new Vector2(o.getX(), o.getY());
        }

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            return new Vector2(
                    JsonUtility.getFloat(obj, "x", 0f),
                    JsonUtility.getFloat(obj, "y", 0f));
        }

        return new Vector2(0f, 0f);
    }

    // Element Type Parsing \\

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

    // On Click Parsing \\

    private String[] parseOnClick(JsonObject json) {

        if (!json.has("on_click"))
            return null;

        JsonObject clickJson = json.getAsJsonObject("on_click");

        return new String[] {
                JsonUtility.validateString(clickJson, "class"),
                JsonUtility.validateString(clickJson, "method"),
                JsonUtility.getString(clickJson, "arg", null)
        };
    }

    // Font / Color Parsing \\

    private float[] parseColor(JsonObject json) {

        if (!json.has("color"))
            return null;

        JsonArray arr = json.getAsJsonArray("color");

        if (arr.size() != 4)
            throwException("'color' must be exactly 4 floats [r, g, b, a]");

        return new float[] {
                arr.get(0).getAsFloat(),
                arr.get(1).getAsFloat(),
                arr.get(2).getAsFloat(),
                arr.get(3).getAsFloat()
        };
    }
}