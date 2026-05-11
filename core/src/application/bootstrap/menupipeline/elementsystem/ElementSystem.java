package application.bootstrap.menupipeline.elementsystem;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.function.Supplier;

import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementStateStruct;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.font.FontInstance;
import application.bootstrap.menupipeline.fontmanager.FontManager;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menu.MenuNodeStruct;
import application.bootstrap.shaderpipeline.sprite.SpriteInstance;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.registry.RegistryUtility;

public class ElementSystem extends SystemPackage {

    /*
     * Owns the master element registry and drives runtime instantiation of element
     * trees. Masters are registered during bootstrap keyed by composite
     * file/element path. Cycle detection prevents circular file dependencies during
     * template resolution.
     *
     * At runtime, createInstances walks the MenuNodeStruct tree passed to it.
     * master.getChildren() is a bootstrap-only concept used when building ref nodes
     * — ElementSystem never reads it during instantiation.
     *
     * For each element, hover and click state sprite instances are cloned from
     * the state's sprite override (if any), and hover/click state children are
     * instantiated from their respective MenuNodeStruct lists. All three are
     * passed into the ElementInstance constructor so the hit and render systems
     * can traverse them without re-resolving the handle each frame.
     *
     * When a hover state has a master handle, a hoverStateRoot ElementInstance is
     * built from that master with the hover children as its child list. The render
     * system renders it as a full positioned overlay so its own layout, visuals,
     * and stack direction are respected.
     */

    // Internal
    private SpriteManager spriteManager;
    private FontManager fontManager;

    // Palette
    private Object2IntOpenHashMap<String> masterKey2MasterID;
    private Int2ObjectOpenHashMap<ElementHandle> masterID2MasterHandle;

    // State
    private ObjectOpenHashSet<String> loadingFiles;

    // Internal \\

    @Override
    protected void create() {
        this.masterKey2MasterID = new Object2IntOpenHashMap<>();
        this.masterID2MasterHandle = new Int2ObjectOpenHashMap<>();
        this.masterKey2MasterID.defaultReturnValue(-1);
        this.loadingFiles = new ObjectOpenHashSet<>();
    }

    @Override
    protected void get() {
        this.spriteManager = get(SpriteManager.class);
        this.fontManager = get(FontManager.class);
    }

    // Master Registry \\

    public boolean hasMaster(String key) {
        return masterKey2MasterID.containsKey(key);
    }

    public ElementHandle getMaster(String key) {
        int id = masterKey2MasterID.getInt(key);
        return id == -1 ? null : masterID2MasterHandle.get(id);
    }

    public void registerMaster(String key, ElementHandle handle) {
        int id = RegistryUtility.toIntID(key);
        masterKey2MasterID.put(key, id);
        masterID2MasterHandle.put(id, handle);
    }

    public Iterable<String> getMasterKeys() {
        return masterKey2MasterID.keySet();
    }

    // Cycle Detection \\

    public boolean isFileLoading(String filePath) {
        return loadingFiles.contains(filePath);
    }

    public void beginFileLoad(String filePath) {
        loadingFiles.add(filePath);
    }

    public void endFileLoad(String filePath) {
        loadingFiles.remove(filePath);
    }

    // Runtime Instantiation \\

    public ObjectArrayList<ElementInstance> createInstances(
            ObjectArrayList<MenuNodeStruct> nodes,
            Supplier<MenuInstance> parentRef) {

        ObjectArrayList<ElementInstance> result = new ObjectArrayList<>(nodes.size());

        for (int i = 0; i < nodes.size(); i++)
            result.add(createInstance(nodes.get(i), parentRef));

        return result;
    }

    private ElementInstance createInstance(
            MenuNodeStruct node,
            Supplier<MenuInstance> parentRef) {

        ElementHandle master = node.getMaster();
        ElementData data = master.getElementData();

        // Default sprite — node override takes priority over handle definition
        String sourceName = node.getSpriteNameOverride() != null
                ? node.getSpriteNameOverride()
                : data.getSpriteName();

        SpriteInstance spriteInstance = sourceName != null
                ? spriteManager.cloneSprite(sourceName)
                : null;

        // Hover sprite — cloned from state sprite override if present
        ElementStateStruct hoverState = master.getHoverState();
        SpriteInstance hoverSpriteInstance = null;

        if (hoverState != null && hoverState.hasSpriteOverride())
            hoverSpriteInstance = spriteManager.cloneSprite(hoverState.getSpriteOverride());

        // Click sprite — cloned from state sprite override if present
        ElementStateStruct clickState = master.getClickState();
        SpriteInstance clickSpriteInstance = null;

        if (clickState != null && clickState.hasSpriteOverride())
            clickSpriteInstance = spriteManager.cloneSprite(clickState.getSpriteOverride());

        // Font
        FontInstance fontInstance = null;

        String resolvedFontName = data.hasFont()
                ? data.getFontName()
                : data.getType() == ElementType.LABEL ? EngineSetting.FONT_DEFAULT_NAME : null;

        if (resolvedFontName != null) {

            String materialName = data.hasMaterial()
                    ? data.getMaterialName()
                    : EngineSetting.FONT_DEFAULT_MATERIAL;

            fontInstance = fontManager.cloneFont(resolvedFontName, materialName);

            Color color = node.hasColorOverride()
                    ? node.getColorOverride()
                    : data.hasColor()
                            ? data.getColor()
                            : EngineSetting.FONT_DEFAULT_COLOR;

            fontInstance.setColor(color.r, color.g, color.b, color.a);

            String text = node.getTextOverride() != null
                    ? node.getTextOverride()
                    : data.getText();

            if (text != null)
                fontInstance.setText(text);
        }

        // Default children
        ObjectArrayList<MenuNodeStruct> childNodes = node.getChildren();
        ObjectArrayList<ElementInstance> childInstances = new ObjectArrayList<>(childNodes.size());

        for (int i = 0; i < childNodes.size(); i++)
            childInstances.add(createInstance(childNodes.get(i), parentRef));

        // Hover state children — replace default children in-place when hovered
        ObjectArrayList<ElementInstance> hoverStateInstances = new ObjectArrayList<>();

        if (hoverState != null && hoverState.hasChildren()) {
            ObjectArrayList<MenuNodeStruct> hoverNodes = hoverState.getChildren();
            for (int i = 0; i < hoverNodes.size(); i++)
                hoverStateInstances.add(createInstance(hoverNodes.get(i), parentRef));
        }

        // Click state children — rendered as dropdown overlay when click-expanded
        ObjectArrayList<ElementInstance> clickStateInstances = new ObjectArrayList<>();

        if (clickState != null && clickState.hasChildren()) {
            ObjectArrayList<MenuNodeStruct> clickStateNodes = clickState.getChildren();
            for (int i = 0; i < clickStateNodes.size(); i++)
                clickStateInstances.add(createInstance(clickStateNodes.get(i), parentRef));
        }

        ElementInstance instance = create(ElementInstance.class);
        instance.constructor(
                master,
                spriteInstance,
                hoverSpriteInstance,
                clickSpriteInstance,
                fontInstance,
                node.getTextOverride(),
                null,
                node.getActionClassOverride(),
                node.getActionMethodOverride(),
                node.getActionArgOverride(),
                master.getActionClass(),
                master.getActionMethod(),
                master.getActionArg(),
                node.getLayoutOverride(),
                childInstances,
                hoverStateInstances,
                clickStateInstances);

        // Hover state root — when the hover state references a master handle,
        // build a full ElementInstance for that container so the render system
        // can position and render it as a proper overlay with its own layout,
        // visuals, and stack direction rather than inlining children into this
        // element's bounds
        if (hoverState != null && hoverState.hasMaster()) {
            ElementInstance hoverStateRoot = createHoverStateRoot(
                    hoverState.getMaster(), hoverStateInstances, parentRef);
            instance.setHoverStateRoot(hoverStateRoot);
        }

        return instance;
    }

    private ElementInstance createHoverStateRoot(
            ElementHandle master,
            ObjectArrayList<ElementInstance> hoverChildren,
            Supplier<MenuInstance> parentRef) {

        ElementData data = master.getElementData();

        // Sprite for the root container panel itself
        SpriteInstance spriteInstance = data.hasSprite()
                ? spriteManager.cloneSprite(data.getSpriteName())
                : null;

        // Font for the root container — labels directly on the panel are rare
        // but supported
        FontInstance fontInstance = null;

        String resolvedFontName = data.hasFont()
                ? data.getFontName()
                : data.getType() == ElementType.LABEL ? EngineSetting.FONT_DEFAULT_NAME : null;

        if (resolvedFontName != null) {

            String materialName = data.hasMaterial()
                    ? data.getMaterialName()
                    : EngineSetting.FONT_DEFAULT_MATERIAL;

            fontInstance = fontManager.cloneFont(resolvedFontName, materialName);

            if (data.hasColor()) {
                Color color = data.getColor();
                fontInstance.setColor(color.r, color.g, color.b, color.a);
            } else {
                Color color = EngineSetting.FONT_DEFAULT_COLOR;
                fontInstance.setColor(color.r, color.g, color.b, color.a);
            }

            if (data.getText() != null)
                fontInstance.setText(data.getText());
        }

        // The hover children are already instantiated — pass them directly as
        // the child list so the root renders them through its own stack logic
        ElementInstance root = create(ElementInstance.class);
        root.constructor(
                master,
                spriteInstance,
                null,
                null,
                fontInstance,
                null,
                null,
                null,
                null,
                null,
                master.getActionClass(),
                master.getActionMethod(),
                master.getActionArg(),
                null,
                hoverChildren,
                new ObjectArrayList<>(),
                new ObjectArrayList<>());

        return root;
    }

    public ElementInstance createDetachedInstance(MenuNodeStruct node) {
        return createInstance(node, () -> null);
    }
}