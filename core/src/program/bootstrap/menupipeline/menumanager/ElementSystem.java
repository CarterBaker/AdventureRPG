package program.bootstrap.menupipeline.menumanager;

import program.bootstrap.menupipeline.element.ElementData;
import program.bootstrap.menupipeline.element.ElementHandle;
import program.bootstrap.menupipeline.element.ElementInstance;
import program.bootstrap.menupipeline.element.ElementPlacementStruct;
import program.bootstrap.menupipeline.fontmanager.FontManager;
import program.bootstrap.menupipeline.fonts.FontInstance;
import program.bootstrap.menupipeline.menu.MenuInstance;
import program.bootstrap.menupipeline.util.MenuAwareAction;
import program.bootstrap.shaderpipeline.sprite.SpriteInstance;
import program.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import program.core.engine.SystemPackage;
import program.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.function.Supplier;

class ElementSystem extends SystemPackage {

    /*
     * Owns the master element registry and drives runtime instantiation of element
     * trees. Masters are registered during bootstrap keyed by composite
     * file/element
     * path. Cycle detection prevents circular file dependencies during template
     * resolution.
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

        // Palette
        this.masterKey2MasterID = new Object2IntOpenHashMap<>();
        this.masterID2MasterHandle = new Int2ObjectOpenHashMap<>();
        this.masterKey2MasterID.defaultReturnValue(-1);

        // State
        this.loadingFiles = new ObjectOpenHashSet<>();
    }

    @Override
    protected void get() {

        // Internal
        this.spriteManager = get(SpriteManager.class);
        this.fontManager = get(FontManager.class);
    }

    // Master Registry \\

    boolean hasMaster(String key) {
        return masterKey2MasterID.containsKey(key);
    }

    ElementHandle getMaster(String key) {

        int id = masterKey2MasterID.getInt(key);

        return id == -1 ? null : masterID2MasterHandle.get(id);
    }

    void registerMaster(String key, ElementHandle handle) {

        int id = RegistryUtility.toIntID(key);

        masterKey2MasterID.put(key, id);
        masterID2MasterHandle.put(id, handle);
    }

    Iterable<String> getMasterKeys() {
        return masterKey2MasterID.keySet();
    }

    // Cycle Detection \\

    boolean isFileLoading(String filePath) {
        return loadingFiles.contains(filePath);
    }

    void beginFileLoad(String filePath) {
        loadingFiles.add(filePath);
    }

    void endFileLoad(String filePath) {
        loadingFiles.remove(filePath);
    }

    // Runtime Instantiation \\

    ObjectArrayList<ElementInstance> createInstances(
            ObjectArrayList<ElementPlacementStruct> placements,
            Supplier<MenuInstance> parentRef) {

        ObjectArrayList<ElementInstance> result = new ObjectArrayList<>(placements.size());

        for (int i = 0; i < placements.size(); i++)
            result.add(createInstance(placements.get(i), parentRef));

        return result;
    }

    private ElementInstance createInstance(
            ElementPlacementStruct placement,
            Supplier<MenuInstance> parentRef) {

        ElementHandle master = placement.getMaster();
        ElementData data = master.getElementData();

        String sourceName = placement.getSpriteNameOverride() != null
                ? placement.getSpriteNameOverride()
                : data.getSpriteName();

        SpriteInstance spriteInstance = sourceName != null
                ? spriteManager.cloneSprite(sourceName)
                : null;

        FontInstance fontInstance = null;

        if (data.hasFont()) {

            fontInstance = fontManager.cloneFont(data.getFontName());

            float[] color = placement.hasColorOverride()
                    ? placement.getColorOverride()
                    : data.hasColor() ? data.getColor() : null;

            if (color != null)
                fontInstance.setColor(color[0], color[1], color[2], color[3]);

            String text = placement.getTextOverride() != null
                    ? placement.getTextOverride()
                    : data.getText();

            if (text != null)
                fontInstance.setText(text);
        }

        Runnable resolvedAction = resolveAction(master, placement, parentRef);
        ObjectArrayList<ElementPlacementStruct> childPlacements = master.getChildren();
        ObjectArrayList<ElementInstance> childInstances = new ObjectArrayList<>(childPlacements.size());

        for (int i = 0; i < childPlacements.size(); i++)
            childInstances.add(createInstance(childPlacements.get(i), parentRef));

        ElementInstance instance = create(ElementInstance.class);
        instance.constructor(
                data,
                spriteInstance,
                fontInstance,
                placement.getTextOverride(),
                resolvedAction,
                placement.getLayoutOverride(),
                childInstances);

        return instance;
    }

    private Runnable resolveAction(
            ElementHandle master,
            ElementPlacementStruct placement,
            Supplier<MenuInstance> parentRef) {

        MenuAwareAction maaOverride = placement.getMenuAwareActionOverride();

        if (maaOverride != null)
            return () -> maaOverride.execute(parentRef.get());

        Runnable clickOverride = placement.getClickActionOverride();

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

    ElementInstance createDetachedInstance(ElementPlacementStruct placement) {
        return createInstance(placement, () -> null);
    }
}