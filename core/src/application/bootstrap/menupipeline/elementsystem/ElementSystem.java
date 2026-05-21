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
     * Owns the master element registry and drives runtime instantiation.
     *
     * For each element, sprite instances are cloned for all four states
     * (hoverEnter, hover, hoverExit, click). State children are instantiated
     * from their respective MenuNodeStruct lists. When a state has a master
     * handle, a state root ElementInstance is built from that master so the
     * render system can position and render it as a full positioned overlay.
     */

    private SpriteManager spriteManager;
    private FontManager fontManager;

    private Object2IntOpenHashMap<String> masterKey2MasterID;
    private Int2ObjectOpenHashMap<ElementHandle> masterID2MasterHandle;
    private ObjectOpenHashSet<String> loadingFiles;

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

    private ElementInstance createInstance(MenuNodeStruct node, Supplier<MenuInstance> parentRef) {

        ElementHandle master = node.getMaster();
        ElementData data = master.getElementData();

        // Default sprite
        String sourceName = node.getSpriteNameOverride() != null
                ? node.getSpriteNameOverride()
                : data.getSpriteName();
        SpriteInstance spriteInstance = sourceName != null
                ? spriteManager.cloneSprite(sourceName)
                : null;

        // State sprites
        ElementStateStruct hoverEnterState = master.getHoverEnterState();
        ElementStateStruct hoverState = master.getHoverState();
        ElementStateStruct hoverExitState = master.getHoverExitState();
        ElementStateStruct clickState = master.getClickState();

        SpriteInstance hoverEnterSprite = hoverEnterState != null && hoverEnterState.hasSpriteOverride()
                ? spriteManager.cloneSprite(hoverEnterState.getSpriteOverride())
                : null;
        SpriteInstance hoverSprite = hoverState != null && hoverState.hasSpriteOverride()
                ? spriteManager.cloneSprite(hoverState.getSpriteOverride())
                : null;
        SpriteInstance hoverExitSprite = hoverExitState != null && hoverExitState.hasSpriteOverride()
                ? spriteManager.cloneSprite(hoverExitState.getSpriteOverride())
                : null;
        SpriteInstance clickSprite = clickState != null && clickState.hasSpriteOverride()
                ? spriteManager.cloneSprite(clickState.getSpriteOverride())
                : null;

        // Font
        FontInstance fontInstance = buildFontInstance(data, node);

        // Default children
        ObjectArrayList<MenuNodeStruct> childNodes = node.getChildren();
        ObjectArrayList<ElementInstance> childInstances = new ObjectArrayList<>(childNodes.size());
        for (int i = 0; i < childNodes.size(); i++)
            childInstances.add(createInstance(childNodes.get(i), parentRef));

        // State children
        ObjectArrayList<ElementInstance> hoverEnterChildren = buildStateChildren(hoverEnterState, parentRef);
        ObjectArrayList<ElementInstance> hoverChildren = buildStateChildren(hoverState, parentRef);
        ObjectArrayList<ElementInstance> hoverExitChildren = buildStateChildren(hoverExitState, parentRef);
        ObjectArrayList<ElementInstance> clickChildren = buildStateChildren(clickState, parentRef);

        ElementInstance instance = create(ElementInstance.class);
        instance.constructor(
                master,
                spriteInstance,
                hoverEnterSprite,
                hoverSprite,
                hoverExitSprite,
                clickSprite,
                fontInstance,
                node.getTextOverride(),
                node.getActionClassOverride(),
                node.getActionMethodOverride(),
                node.getActionArgOverride(),
                node.getOnDragClassOverride(),
                node.getOnDragMethodOverride(),
                node.getOnDragArgOverride(),
                node.getLayoutOverride(),
                childInstances,
                hoverEnterChildren,
                hoverChildren,
                hoverExitChildren,
                clickChildren);

        // State roots — when state has a master, build a full positioned overlay
        if (hoverEnterState != null && hoverEnterState.hasMaster())
            instance.setHoverEnterStateRoot(
                    buildStateRoot(hoverEnterState.getMaster(), hoverEnterChildren));
        if (hoverState != null && hoverState.hasMaster())
            instance.setHoverStateRoot(
                    buildStateRoot(hoverState.getMaster(), hoverChildren));
        if (hoverExitState != null && hoverExitState.hasMaster())
            instance.setHoverExitStateRoot(
                    buildStateRoot(hoverExitState.getMaster(), hoverExitChildren));

        return instance;
    }

    private ObjectArrayList<ElementInstance> buildStateChildren(
            ElementStateStruct state,
            Supplier<MenuInstance> parentRef) {

        ObjectArrayList<ElementInstance> result = new ObjectArrayList<>();

        if (state != null && state.hasChildren()) {
            ObjectArrayList<MenuNodeStruct> nodes = state.getChildren();
            for (int i = 0; i < nodes.size(); i++)
                result.add(createInstance(nodes.get(i), parentRef));
        }

        return result;
    }

    private ElementInstance buildStateRoot(
            ElementHandle master,
            ObjectArrayList<ElementInstance> stateChildren) {

        ElementData data = master.getElementData();

        SpriteInstance spriteInstance = data.hasSprite()
                ? spriteManager.cloneSprite(data.getSpriteName())
                : null;

        FontInstance fontInstance = buildFontInstanceFromData(data);

        ElementInstance root = create(ElementInstance.class);
        root.constructor(
                master,
                spriteInstance,
                null, null, null, null,
                fontInstance,
                null,
                null, null, null,
                null, null, null,
                null,
                stateChildren,
                new ObjectArrayList<>(),
                new ObjectArrayList<>(),
                new ObjectArrayList<>(),
                new ObjectArrayList<>());

        return root;
    }

    private FontInstance buildFontInstance(ElementData data, MenuNodeStruct node) {

        String resolvedFontName = data.hasFont() ? data.getFontName()
                : data.getType() == ElementType.LABEL ? EngineSetting.FONT_DEFAULT_NAME : null;

        if (resolvedFontName == null)
            return null;

        String materialName = data.hasMaterial()
                ? data.getMaterialName()
                : EngineSetting.FONT_DEFAULT_MATERIAL;

        FontInstance fontInstance = fontManager.cloneFont(resolvedFontName, materialName);

        Color color = node.hasColorOverride() ? node.getColorOverride()
                : data.hasColor() ? data.getColor()
                        : EngineSetting.FONT_DEFAULT_COLOR;

        fontInstance.setColor(color.r, color.g, color.b, color.a);

        String text = node.getTextOverride() != null ? node.getTextOverride() : data.getText();
        if (text != null)
            fontInstance.setText(text);

        return fontInstance;
    }

    private FontInstance buildFontInstanceFromData(ElementData data) {

        String resolvedFontName = data.hasFont() ? data.getFontName()
                : data.getType() == ElementType.LABEL ? EngineSetting.FONT_DEFAULT_NAME : null;

        if (resolvedFontName == null)
            return null;

        String materialName = data.hasMaterial()
                ? data.getMaterialName()
                : EngineSetting.FONT_DEFAULT_MATERIAL;

        FontInstance fontInstance = fontManager.cloneFont(resolvedFontName, materialName);

        Color color = data.hasColor() ? data.getColor() : EngineSetting.FONT_DEFAULT_COLOR;
        fontInstance.setColor(color.r, color.g, color.b, color.a);

        if (data.getText() != null)
            fontInstance.setText(data.getText());

        return fontInstance;
    }

    public ElementInstance createDetachedInstance(MenuNodeStruct node) {
        return createInstance(node, () -> null);
    }
}