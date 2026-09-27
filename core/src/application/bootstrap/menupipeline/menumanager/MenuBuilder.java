package application.bootstrap.menupipeline.menumanager;

import java.io.File;

import application.bootstrap.menupipeline.element.ElementAnimationStruct;
import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementStateStruct;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.menu.MenuData;
import application.bootstrap.menupipeline.menu.MenuHandle;
import application.bootstrap.menupipeline.menu.MenuNodeStruct;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.StackDirection;
import application.bootstrap.menupipeline.util.TextAlign;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class MenuBuilder extends BuilderPackage {

    /*
     * Parses menu ARPG into MenuHandles and ElementHandles at bootstrap: the
     * four state blocks, click and drag callbacks, and animation timelines.
     * Inline masters are registered under the scope they are declared in, so
     * two templates can share a child id.
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

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        if (!registeredFiles.contains(filePath)) {

            if (elementSystem.isFileLoading(filePath))
                throwException("Circular file dependency at: '" + filePath + "'");

            elementSystem.beginFileLoad(filePath);
            registeredFiles.add(filePath);

            try {
                registerTopLevelMasters(filePath, arpg);
            } finally {
                elementSystem.endFileLoad(filePath);
            }
        }

        ObjectArrayList<MenuHandle> handles = new ObjectArrayList<>();

        if (!arpg.has("menus"))
            return handles;

        ArpgArrayStruct menuArray = arpg.getAsArray("menus");

        for (int i = 0; i < menuArray.size(); i++)
            handles.add(buildMenuHandle(filePath, menuArray.get(i).getAsObject()));

        return handles;
    }

    void resolveAllDeferredRefs() {
        for (int i = 0; i < deferredRefs.size(); i++)
            deferredRefs.get(i).run();
        deferredRefs.clear();
    }

    // Menu Building \\

    private MenuHandle buildMenuHandle(String filePath, ArpgObjectStruct menuArpg) {

        String id = ArpgUtility.validateString(menuArpg, "id");
        boolean lockInput = ArpgUtility.getBoolean(menuArpg, "lock_input", false);
        boolean raycastInput = ArpgUtility.getBoolean(menuArpg, "raycast_input", false);
        boolean hasCanvasArea = scanForCanvasArea(menuArpg);

        ObjectArrayList<String> entryPoints = new ObjectArrayList<>();

        if (menuArpg.has("entry_points")) {
            ArpgArrayStruct eps = menuArpg.getAsArray("entry_points");
            for (int i = 0; i < eps.size(); i++)
                entryPoints.add(eps.get(i).getAsString());
        }

        ObjectArrayList<MenuNodeStruct> nodes = buildNodes(
                filePath + "/" + id, menuArpg, null,
                DimensionValueStruct.parse(EngineSetting.FONT_DEFAULT_SIZE_PERCENT), true);

        MenuData data = new MenuData(
                filePath + "/" + id, lockInput, raycastInput, hasCanvasArea, entryPoints);
        MenuHandle handle = create(MenuHandle.class);
        handle.constructor(data, nodes);

        return handle;
    }

    private boolean scanForCanvasArea(ArpgObjectStruct arpg) {

        if (!arpg.has("elements"))
            return false;

        ArpgArrayStruct elements = arpg.getAsArray("elements");

        for (int i = 0; i < elements.size(); i++) {
            ArpgObjectStruct el = elements.get(i).getAsObject();
            if (el.has("type") && el.get("type").getAsString().equalsIgnoreCase("canvas_area"))
                return true;
            if (scanForCanvasArea(el))
                return true;
        }

        return false;
    }

    // Top-Level Master Registration \\

    private void registerTopLevelMasters(String filePath, ArpgObjectStruct arpg) {

        if (!arpg.has("elements"))
            return;

        ArpgArrayStruct elements = arpg.getAsArray("elements");

        for (int i = 0; i < elements.size(); i++) {

            ArpgObjectStruct el = elements.get(i).getAsObject();

            if (el.has("ref") || el.has("use"))
                continue;

            String id = ArpgUtility.validateString(el, "id");
            String key = filePath + "/" + id;

            if (!elementSystem.hasMaster(key))
                elementSystem.registerMaster(key,
                        buildMasterFromArpg(filePath, id, el, null,
                                DimensionValueStruct.parse(EngineSetting.FONT_DEFAULT_SIZE_PERCENT), true));
        }
    }

    // Node Building \\

    private ObjectArrayList<MenuNodeStruct> buildNodes(
            String scope,
            ArpgObjectStruct parent,
            String inheritedFontName,
            DimensionValueStruct inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        if (!parent.has("elements"))
            return new ObjectArrayList<>();

        ArpgArrayStruct array = parent.getAsArray("elements");
        ObjectArrayList<MenuNodeStruct> nodes = new ObjectArrayList<>(array.size());

        for (int i = 0; i < array.size(); i++)
            nodes.add(buildNode(scope, array.get(i).getAsObject(),
                    inheritedFontName, inheritedFontSize, inheritedExplicitFontSize));

        return nodes;
    }

    private MenuNodeStruct buildNode(
            String scope,
            ArpgObjectStruct arpg,
            String inheritedFontName,
            DimensionValueStruct inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        String id = ArpgUtility.validateString(arpg, "id");

        if (arpg.has("ref"))
            return buildRefNode(scope, id, arpg);

        if (arpg.has("use"))
            return buildUseNode(scope, id, arpg, inheritedFontName, inheritedFontSize,
                    inheritedExplicitFontSize);

        return buildInlineNode(scope, id, arpg, inheritedFontName, inheritedFontSize,
                inheritedExplicitFontSize);
    }

    private MenuNodeStruct buildInlineNode(
            String scope,
            String id,
            ArpgObjectStruct arpg,
            String inheritedFontName,
            DimensionValueStruct inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        String key = scope + "/" + id;
        ElementHandle master = elementSystem.getMaster(key);

        if (master == null) {
            master = buildMasterFromArpg(scope, id, arpg, inheritedFontName,
                    inheritedFontSize, inheritedExplicitFontSize);
            elementSystem.registerMaster(key, master);
        }

        return new MenuNodeStruct(master, master.getChildren());
    }

    private MenuNodeStruct buildUseNode(
            String scope,
            String id,
            ArpgObjectStruct arpg,
            String inheritedFontName,
            DimensionValueStruct inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        String usePath = arpg.get("use").getAsString();
        ElementHandle template = resolveTemplate(usePath, id);

        boolean explicitFontSize = arpg.has("font_size") || template.hasExplicitFontSize();
        String resolvedFontName = ArpgUtility.getString(arpg, "font", template.getFontName());
        DimensionValueStruct resolvedFontSize = arpg.has("font_size")
                ? DimensionValueStruct.parse(arpg.get("font_size").getAsString())
                : template.getFontSize();

        ObjectArrayList<MenuNodeStruct> arpgChildren = buildNodes(
                scope + "/" + id, arpg, resolvedFontName, resolvedFontSize, explicitFontSize);
        ObjectArrayList<MenuNodeStruct> children = !arpgChildren.isEmpty()
                ? arpgChildren
                : template.getChildren();

        LayoutStruct partialOverride = MenuFileParserUtility.parseLayoutOverride(arpg);
        LayoutStruct layoutOverride = partialOverride != null
                ? LayoutStruct.merge(template.getLayout(), partialOverride)
                : null;

        String spritePath = ArpgUtility.getString(arpg, "sprite", null);
        String spriteNameOverride = spritePath != null ? resolveSpriteName(id, spritePath) : null;
        String textOverride = ArpgUtility.getString(arpg, "text", null);
        MenuColorStruct colorOverride = MenuFileParserUtility.parseColor(arpg);
        String[] onClick = MenuFileParserUtility.parseOnClick(arpg);
        String[] onDrag = MenuFileParserUtility.parseOnDrag(arpg);

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

    private MenuNodeStruct buildRefNode(String scope, String id, ArpgObjectStruct arpg) {

        String refKey = arpg.get("ref").getAsString();
        LayoutStruct partialOverride = MenuFileParserUtility.parseLayoutOverride(arpg);
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

    private ElementHandle buildMasterFromArpg(
            String scope,
            String id,
            ArpgObjectStruct arpg,
            String inheritedFontName,
            DimensionValueStruct inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        ElementType type = MenuFileParserUtility.parseElementType(
                ArpgUtility.validateString(arpg, "type"), id);
        String spritePath = ArpgUtility.getString(arpg, "sprite", null);
        String text = ArpgUtility.getString(arpg, "text", null);
        String fontName = ArpgUtility.getString(arpg, "font", inheritedFontName);
        String materialName = ArpgUtility.getString(arpg, "material", null);
        boolean explicitFontSize = arpg.has("font_size") || inheritedExplicitFontSize;
        DimensionValueStruct fontSize = arpg.has("font_size")
                ? DimensionValueStruct.parse(arpg.get("font_size").getAsString())
                : inheritedFontSize;
        MenuColorStruct color = MenuFileParserUtility.parseColor(arpg);
        MenuColorStruct hoverColor = MenuFileParserUtility.parseHoverColor(arpg);
        MenuColorStruct parentHoverColor = MenuFileParserUtility.parseParentHoverColor(arpg);
        LayoutStruct layout = MenuFileParserUtility.parseLayout(arpg);
        boolean mask = ArpgUtility.getBoolean(arpg, "mask", false);
        StackDirection stackDirection = arpg.has("stack")
                ? StackDirection.fromString(arpg.get("stack").getAsString())
                : StackDirection.NONE;
        DimensionValueStruct spacing = arpg.has("spacing")
                ? DimensionValueStruct.parse(arpg.get("spacing").getAsString())
                : null;
        TextAlign textAlign = arpg.has("align")
                ? TextAlign.fromString(arpg.get("align").getAsString())
                : TextAlign.CENTER;
        boolean startExpanded = ArpgUtility.getBoolean(arpg, "start_expanded", false);
        ElementAnimationStruct animation = MenuFileParserUtility.parseAnimation(arpg);
        String spriteName = resolveSpriteName(id, spritePath);

        String[] onClick = MenuFileParserUtility.parseOnClick(arpg);
        String[] onDrag = MenuFileParserUtility.parseOnDrag(arpg);

        ObjectArrayList<MenuNodeStruct> defaultChildren = buildNodes(
                scope + "/" + id, arpg, fontName, fontSize, explicitFontSize);

        ElementData data = new ElementData(
                id, type, spriteName, text, fontName, materialName, fontSize, explicitFontSize,
                color, hoverColor, parentHoverColor, layout, mask, stackDirection, spacing, textAlign, startExpanded,
                animation,
                onClick != null ? onClick[0] : null,
                onClick != null ? onClick[1] : null,
                onClick != null ? onClick[2] : null,
                onDrag != null ? onDrag[0] : null,
                onDrag != null ? onDrag[1] : null,
                onDrag != null ? onDrag[2] : null);

        ElementStateStruct hoverEnterState = parseStateBlock(
                scope, id, arpg, "on_hover_enter", fontName, fontSize, explicitFontSize);
        ElementStateStruct hoverState = parseStateBlock(
                scope, id, arpg, "on_hover", fontName, fontSize, explicitFontSize);
        ElementStateStruct hoverExitState = parseStateBlock(
                scope, id, arpg, "on_hover_exit", fontName, fontSize, explicitFontSize);
        ElementStateStruct clickState = parseStateBlock(
                scope, id, arpg, "click_state", fontName, fontSize, explicitFontSize);

        ElementHandle master = create(ElementHandle.class);
        master.constructor(data, defaultChildren, hoverEnterState, hoverState, hoverExitState, clickState);

        return master;
    }

    // State Block Parsing \\

    private ElementStateStruct parseStateBlock(
            String scope,
            String id,
            ArpgObjectStruct arpg,
            String stateKey,
            String inheritedFontName,
            DimensionValueStruct inheritedFontSize,
            boolean inheritedExplicitFontSize) {

        if (!arpg.has(stateKey))
            return null;

        ArpgObjectStruct stateArpg = arpg.getAsObject(stateKey);

        ElementHandle baseMaster = null;

        if (stateArpg.has("use")) {
            String usePath = stateArpg.get("use").getAsString();
            baseMaster = resolveTemplate(usePath, id);
        }

        boolean explicitFontSize = stateArpg.has("font_size")
                || (baseMaster != null
                        ? baseMaster.hasExplicitFontSize()
                        : inheritedExplicitFontSize);
        String fontName = ArpgUtility.getString(stateArpg, "font",
                baseMaster != null ? baseMaster.getFontName() : inheritedFontName);
        DimensionValueStruct fontSize = stateArpg.has("font_size")
                ? DimensionValueStruct.parse(stateArpg.get("font_size").getAsString())
                : (baseMaster != null ? baseMaster.getFontSize() : inheritedFontSize);

        ObjectArrayList<MenuNodeStruct> arpgChildren = buildNodes(
                scope + "/" + id + "/" + stateKey, stateArpg, fontName, fontSize, explicitFontSize);
        ObjectArrayList<MenuNodeStruct> children = !arpgChildren.isEmpty()
                ? arpgChildren
                : baseMaster != null ? baseMaster.getChildren() : new ObjectArrayList<>();

        LayoutStruct partialLayout = MenuFileParserUtility.parseLayoutOverride(stateArpg);
        LayoutStruct layoutOverride = null;

        if (partialLayout != null)
            layoutOverride = baseMaster != null
                    ? LayoutStruct.merge(baseMaster.getLayout(), partialLayout)
                    : partialLayout;

        String spritePath = ArpgUtility.getString(stateArpg, "sprite", null);
        String spriteOverride = spritePath != null ? resolveSpriteName(id, spritePath) : null;
        String textOverride = ArpgUtility.getString(stateArpg, "text", null);
        MenuColorStruct colorOverride = MenuFileParserUtility.parseColor(stateArpg);

        String[] callback = MenuFileParserUtility.parseOnClick(stateArpg);
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
        for (String ext : EngineSetting.ARPG_FILE_EXTENSIONS) {
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