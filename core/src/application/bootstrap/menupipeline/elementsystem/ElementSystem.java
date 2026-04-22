package application.bootstrap.menupipeline.elementsystem;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.function.Supplier;

import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.font.FontInstance;
import application.bootstrap.menupipeline.fontmanager.FontManager;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menu.MenuNodeStruct;
import application.bootstrap.menupipeline.util.MenuAwareAction;
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
     * file/element
     * path. Cycle detection prevents circular file dependencies during template
     * resolution.
     *
     * At runtime, createInstances walks the MenuNodeStruct tree passed to it.
     * master.getChildren() is a bootstrap-only concept used when building ref nodes
     * — ElementSystem never reads it during instantiation.
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

        String sourceName = node.getSpriteNameOverride() != null
                ? node.getSpriteNameOverride()
                : data.getSpriteName();

        SpriteInstance spriteInstance = sourceName != null
                ? spriteManager.cloneSprite(sourceName)
                : null;

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

        Runnable resolvedAction = resolveAction(master, node, parentRef);

        // Children always come from the node — the tree is fully resolved at build time
        ObjectArrayList<MenuNodeStruct> childNodes = node.getChildren();
        ObjectArrayList<ElementInstance> childInstances = new ObjectArrayList<>(childNodes.size());

        for (int i = 0; i < childNodes.size(); i++)
            childInstances.add(createInstance(childNodes.get(i), parentRef));

        ElementInstance instance = create(ElementInstance.class);
        instance.constructor(
                data,
                spriteInstance,
                fontInstance,
                node.getTextOverride(),
                resolvedAction,
                node.getLayoutOverride(),
                childInstances);

        return instance;
    }

    private Runnable resolveAction(
            ElementHandle master,
            MenuNodeStruct node,
            Supplier<MenuInstance> parentRef) {

        MenuAwareAction maaOverride = node.getMenuAwareActionOverride();

        if (maaOverride != null)
            return () -> maaOverride.execute(parentRef.get());

        Runnable clickOverride = node.getClickActionOverride();

        if (clickOverride != null)
            return clickOverride;

        if (master.hasMenuAwareAction()) {
            MenuAwareAction maa = master.getMenuAwareAction();
            return () -> maa.execute(parentRef.get());
        }

        if (master.hasClickAction())
            return master.getClickAction();

        return null;
    }

    public ElementInstance createDetachedInstance(MenuNodeStruct node) {
        return createInstance(node, () -> null);
    }
}