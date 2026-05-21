package application.bootstrap.menupipeline.menumanager;

import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementStateStruct;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.elementsystem.ElementSystem;
import application.bootstrap.menupipeline.menu.MenuData;
import application.bootstrap.menupipeline.menu.MenuHandle;
import application.bootstrap.menupipeline.menu.MenuNodeStruct;
import application.bootstrap.menupipeline.util.DimensionValue;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.StackDirection;
import application.bootstrap.menupipeline.util.TextAlign;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses menu JSON files into MenuHandles and ElementHandles during bootstrap.
     *
     * Four state blocks are parsed per element: on_hover_enter, on_hover,
     * on_hover_exit, click_state. All four go through parseStateBlock which
     * handles use/inline element, sprite, layout, color, text, children, and
     * optional method callback identically.
     *
     * on_drag is parsed as a plain method callback — no state block, no element
     * swap. on_click is unchanged.
     */

    private static final String PARENT_ARG = "$parent";

    // Internal
    private SpriteManager spriteManager;
    private ElementSystem elementSystem;
    private File root;
    private ObjectOpenHashSet<String> registeredFiles;
    private ObjectArrayList<Runnable> deferredRefs;

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
                registerTopLevelMasters(filePath, json);
            } finally {
                elementSystem.endFileLoad(filePath);
            }
        }

        ObjectArrayList<MenuHandle> handles = new ObjectArrayList<>();

        if (!json.has("menus"))
            return handles;

        JsonArray menuArray = json.getAsJsonArray("menus");

        for (int i = 0; i < menuArray.size(); i++)
            handles.add(buildMenuHandle(filePath, menuArray.get(i).getAsJsonObject()));

        return handles;
    }

    void resolveAllDeferredRefs() {
        for (int i = 0; i < deferredRefs.size(); i++)
            deferredRefs.get(i).run();
        deferredRefs.clear();
    }

    // Menu Building \\

    private MenuHandle buildMenuHandle(String filePath, JsonObject menuJson) {

        String id = JsonUtility.validateString(menuJson, "id");
        boolean lockInput = JsonUtility.getBoolean(menuJson, "lock_input", false);
        boolean raycastInput = JsonUtility.getBoolean(menuJson, "raycast_input", false);
        boolean hasCanvasArea = scanForCanvasArea(menuJson);

        ObjectArrayList<String> entryPoints = new ObjectArrayList<>();

        if (menuJson.has("entry_points")) {
            JsonArray eps = menuJson.getAsJsonArray("entry_points");
            for (int i = 0; i < eps.size(); i++)
                entryPoints.add(eps.get(i).getAsString());
        }

        ObjectArrayList<MenuNodeStruct> nodes = buildNodes(
                filePath, menuJson, null,
                DimensionValue.parse(EngineSetting.FONT_DEFAULT_SIZE_PERCENT), true);

        MenuData data = new MenuData(
                filePath + "/" + id, lockInput, raycastInput, hasCanvasArea, entryPoints);
        MenuHandle handle = create(MenuHandle.class);
        handle.constructor(data, nodes);

        return handle;
    }

    private boolean scanForCanvasArea(JsonObject json) {

        if (!json.has("elements"))
            return false;

        JsonArray elements = json.getAsJsonArray("elements");

        for (int i = 0; i < elements.size(); i++) {
            JsonObject el = elements.get(i).getAsJsonObject();
            if (el.has("type") && el.get("type").getAsString().equalsIgnoreCase("canvas_area"))
                return true;
            if (scanForCanvasArea(el))
                return true;
        }

        return false;
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
                elementSystem.registerMaster(key,
                        buildMasterFromJson(filePath, id, el, null,
                                DimensionValue.parse(EngineSetting.FONT_DEFAULT_SIZE_PERCENT), true));
        }
    }

    // Node Building \\

    private ObjectArrayList<MenuNodeStruct> buildNodes(
            String filePath,
            JsonObject parent,
            String inheritedFontName,
            DimensionValue inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        if (!parent.has("elements"))
            return new ObjectArrayList<>();

        JsonArray array = parent.getAsJsonArray("elements");
        ObjectArrayList<MenuNodeStruct> nodes = new ObjectArrayList<>(array.size());

        for (int i = 0; i < array.size(); i++)
            nodes.add(buildNode(filePath, array.get(i).getAsJsonObject(),
                    inheritedFontName, inheritedFontSize, inheritedExplicitFontSize));

        return nodes;
    }

    private MenuNodeStruct buildNode(
            String filePath,
            JsonObject json,
            String inheritedFontName,
            DimensionValue inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        String id = JsonUtility.validateString(json, "id");

        if (json.has("ref"))
            return buildRefNode(filePath, id, json);

        if (json.has("use"))
            return buildUseNode(filePath, id, json, inheritedFontName, inheritedFontSize,
                    inheritedExplicitFontSize);

        return buildInlineNode(filePath, id, json, inheritedFontName, inheritedFontSize,
                inheritedExplicitFontSize);
    }

    private MenuNodeStruct buildInlineNode(
            String filePath,
            String id,
            JsonObject json,
            String inheritedFontName,
            DimensionValue inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        String key = filePath + "/" + id;
        ElementHandle master = elementSystem.getMaster(key);

        if (master == null) {
            master = buildMasterFromJson(filePath, id, json, inheritedFontName,
                    inheritedFontSize, inheritedExplicitFontSize);
            elementSystem.registerMaster(key, master);
        }

        return new MenuNodeStruct(master, master.getChildren());
    }

    private MenuNodeStruct buildUseNode(
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

        ObjectArrayList<MenuNodeStruct> jsonChildren = buildNodes(
                filePath, json, resolvedFontName, resolvedFontSize, explicitFontSize);
        ObjectArrayList<MenuNodeStruct> children = !jsonChildren.isEmpty()
                ? jsonChildren
                : template.getChildren();

        LayoutStruct partialOverride = FileParserUtility.parseLayoutOverride(json);
        LayoutStruct layoutOverride = partialOverride != null
                ? LayoutStruct.merge(template.getLayout(), partialOverride)
                : null;

        String spritePath = JsonUtility.getString(json, "sprite", null);
        String spriteNameOverride = spritePath != null ? resolveSpriteName(id, spritePath) : null;
        String textOverride = JsonUtility.getString(json, "text", null);
        Color colorOverride = FileParserUtility.parseColor(json);
        String[] onClick = FileParserUtility.parseOnClick(json);
        String[] onDrag = FileParserUtility.parseOnDrag(json);

        boolean hasOverride = layoutOverride != null || spriteNameOverride != null
                || textOverride != null || colorOverride != null
                || onClick != null || onDrag != null;

        if (!hasOverride)
            return new MenuNodeStruct(template, children);

        return new MenuNodeStruct(
                template,
                spriteNameOverride,
                textOverride,
                colorOverride,
                onClick != null ? onClick[0] : null,
                onClick != null ? onClick[1] : null,
                onClick != null ? onClick[2] : null,
                onDrag != null ? onDrag[0] : null,
                onDrag != null ? onDrag[1] : null,
                onDrag != null ? onDrag[2] : null,
                layoutOverride,
                children);
    }

    private MenuNodeStruct buildRefNode(String filePath, String id, JsonObject json) {

        String refKey = json.get("ref").getAsString();
        LayoutStruct partialOverride = FileParserUtility.parseLayoutOverride(json);
        ElementHandle resolved = resolveRefKey(refKey);

        if (resolved != null) {

            LayoutStruct layoutOverride = partialOverride != null
                    ? LayoutStruct.merge(resolved.getLayout(), partialOverride)
                    : null;

            ObjectArrayList<MenuNodeStruct> children = resolved.getChildren();

            return layoutOverride != null
                    ? new MenuNodeStruct(resolved, null, null, null, null, null, null,
                            null, null, null, layoutOverride, children)
                    : new MenuNodeStruct(resolved, children);
        }

        ObjectArrayList<MenuNodeStruct> children = new ObjectArrayList<>();
        MenuNodeStruct placeholder = partialOverride != null
                ? new MenuNodeStruct(null, null, null, null, null, null, null,
                        null, null, null, partialOverride, children)
                : new MenuNodeStruct(null, children);

        deferredRefs.add(() -> {
            ElementHandle target = resolveRefKey(refKey);

            if (target == null)
                throwException("Unresolved ref: '" + refKey + "' (id: '" + id + "')");

            if (partialOverride != null)
                placeholder.setLayoutOverride(
                        LayoutStruct.merge(target.getLayout(), partialOverride));

            placeholder.setMaster(target);
            children.addAll(target.getChildren());
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

        ElementType type = FileParserUtility.parseElementType(
                JsonUtility.validateString(json, "type"), id);
        String spritePath = JsonUtility.getString(json, "sprite", null);
        String text = JsonUtility.getString(json, "text", null);
        String fontName = JsonUtility.getString(json, "font", inheritedFontName);
        String materialName = JsonUtility.getString(json, "material", null);
        boolean explicitFontSize = json.has("font_size") || inheritedExplicitFontSize;
        DimensionValue fontSize = json.has("font_size")
                ? DimensionValue.parse(json.get("font_size").getAsString())
                : inheritedFontSize;
        Color color = FileParserUtility.parseColor(json);
        LayoutStruct layout = FileParserUtility.parseLayout(json);
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
        boolean startExpanded = JsonUtility.getBoolean(json, "start_expanded", false);
        String spriteName = resolveSpriteName(id, spritePath);

        String[] onClick = FileParserUtility.parseOnClick(json);
        String[] onDrag = FileParserUtility.parseOnDrag(json);

        ObjectArrayList<MenuNodeStruct> defaultChildren = buildNodes(
                filePath, json, fontName, fontSize, explicitFontSize);

        ElementData data = new ElementData(
                id, type, spriteName, text, fontName, materialName, fontSize, explicitFontSize,
                color, layout, mask, stackDirection, spacing, textAlign, startExpanded,
                onClick != null ? onClick[0] : null,
                onClick != null ? onClick[1] : null,
                onClick != null ? onClick[2] : null,
                onDrag != null ? onDrag[0] : null,
                onDrag != null ? onDrag[1] : null,
                onDrag != null ? onDrag[2] : null);

        ElementStateStruct hoverEnterState = parseStateBlock(
                filePath, id, json, "on_hover_enter", fontName, fontSize, explicitFontSize);
        ElementStateStruct hoverState = parseStateBlock(
                filePath, id, json, "on_hover", fontName, fontSize, explicitFontSize);
        ElementStateStruct hoverExitState = parseStateBlock(
                filePath, id, json, "on_hover_exit", fontName, fontSize, explicitFontSize);
        ElementStateStruct clickState = parseStateBlock(
                filePath, id, json, "click_state", fontName, fontSize, explicitFontSize);

        ElementHandle master = create(ElementHandle.class);
        master.constructor(data, defaultChildren, hoverEnterState, hoverState, hoverExitState, clickState);

        return master;
    }

    // State Block Parsing \\

    private ElementStateStruct parseStateBlock(
            String filePath,
            String id,
            JsonObject json,
            String stateKey,
            String inheritedFontName,
            DimensionValue inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        if (!json.has(stateKey))
            return null;

        JsonObject stateJson = json.getAsJsonObject(stateKey);

        ElementHandle baseMaster = null;

        if (stateJson.has("use")) {
            String usePath = stateJson.get("use").getAsString();
            baseMaster = resolveTemplate(usePath, id);
        }

        boolean explicitFontSize = stateJson.has("font_size")
                || (baseMaster != null
                        ? baseMaster.hasExplicitFontSize()
                        : inheritedExplicitFontSize);
        String fontName = JsonUtility.getString(stateJson, "font",
                baseMaster != null ? baseMaster.getFontName() : inheritedFontName);
        DimensionValue fontSize = stateJson.has("font_size")
                ? DimensionValue.parse(stateJson.get("font_size").getAsString())
                : (baseMaster != null ? baseMaster.getFontSize() : inheritedFontSize);

        ObjectArrayList<MenuNodeStruct> jsonChildren = buildNodes(
                filePath, stateJson, fontName, fontSize, explicitFontSize);
        ObjectArrayList<MenuNodeStruct> children = !jsonChildren.isEmpty()
                ? jsonChildren
                : baseMaster != null ? baseMaster.getChildren() : new ObjectArrayList<>();

        LayoutStruct partialLayout = FileParserUtility.parseLayoutOverride(stateJson);
        LayoutStruct layoutOverride = null;

        if (partialLayout != null)
            layoutOverride = baseMaster != null
                    ? LayoutStruct.merge(baseMaster.getLayout(), partialLayout)
                    : partialLayout;

        String spritePath = JsonUtility.getString(stateJson, "sprite", null);
        String spriteOverride = spritePath != null ? resolveSpriteName(id, spritePath) : null;
        String textOverride = JsonUtility.getString(stateJson, "text", null);
        Color colorOverride = FileParserUtility.parseColor(stateJson);

        String[] callback = FileParserUtility.parseOnClick(stateJson);
        String actionClass = callback != null ? callback[0] : null;
        String actionMethod = callback != null ? callback[1] : null;
        String actionArg = callback != null ? callback[2] : null;

        return new ElementStateStruct(
                baseMaster,
                spriteOverride,
                textOverride,
                colorOverride,
                layoutOverride,
                actionClass,
                actionMethod,
                actionArg,
                children);
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
}