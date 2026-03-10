package com.internal.bootstrap.menupipeline.elementsystem;

import com.internal.bootstrap.menupipeline.element.ElementHandle;
import com.internal.bootstrap.menupipeline.element.ElementInstance;
import com.internal.bootstrap.menupipeline.element.ElementOverrideStruct;
import com.internal.bootstrap.menupipeline.element.ElementPlacementHandle;
import com.internal.bootstrap.menupipeline.element.LayoutStruct;
import com.internal.bootstrap.menupipeline.element.MenuAwareAction;
import com.internal.bootstrap.menupipeline.fonts.FontInstance;
import com.internal.bootstrap.menupipeline.fontmanager.FontManager;
import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteInstance;
import com.internal.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import com.internal.core.engine.SystemPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.function.Supplier;

public class ElementSystem extends SystemPackage {

    // Internal
    private SpriteManager spriteManager;
    private FontManager fontManager;

    // Registry
    private Object2IntOpenHashMap<String> masterKey2ID;
    private Int2ObjectOpenHashMap<ElementHandle> masterID2Handle;
    private int nextMasterID;

    private ObjectOpenHashSet<String> loadingFiles;

    // Base \\

    @Override
    protected void create() {
        this.masterKey2ID = new Object2IntOpenHashMap<>();
        this.masterID2Handle = new Int2ObjectOpenHashMap<>();
        this.masterKey2ID.defaultReturnValue(-1);
        this.nextMasterID = 0;
        this.loadingFiles = new ObjectOpenHashSet<>();
    }

    @Override
    protected void get() {
        this.spriteManager = get(SpriteManager.class);
        this.fontManager = get(FontManager.class);
    }

    // Master Registry \\

    public boolean hasMaster(String key) {
        return masterKey2ID.containsKey(key);
    }

    public ElementHandle getMaster(String key) {
        int id = masterKey2ID.getInt(key);
        return id == -1 ? null : masterID2Handle.get(id);
    }

    public void registerMaster(String key, ElementHandle handle) {
        int id = nextMasterID++;
        masterKey2ID.put(key, id);
        masterID2Handle.put(id, handle);
    }

    public Iterable<String> getMasterKeys() {
        return masterKey2ID.keySet();
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
            ObjectArrayList<ElementPlacementHandle> placements,
            Supplier<MenuInstance> parentRef) {
        ObjectArrayList<ElementInstance> result = new ObjectArrayList<>(placements.size());
        for (ElementPlacementHandle placement : placements)
            result.add(createInstance(placement.getMaster(), placement.getOverride(), parentRef));
        return result;
    }

    private ElementInstance createInstance(
            ElementHandle master,
            ElementOverrideStruct override,
            Supplier<MenuInstance> parentRef) {

        // Sprite
        String sourceName = (override != null && override.getSpriteName() != null)
                ? override.getSpriteName()
                : master.getSpriteName();
        SpriteInstance spriteInstance = sourceName != null
                ? spriteManager.cloneSprite(sourceName)
                : null;

        // Font + color
        FontInstance fontInstance = null;
        if (master.hasFont()) {
            fontInstance = fontManager.cloneFont(master.getFontName());

            // Color — override wins, then handle, then default white
            float[] color = (override != null && override.hasColor())
                    ? override.getColor()
                    : master.hasColor() ? master.getColor() : null;
            if (color != null)
                fontInstance.setColor(color[0], color[1], color[2], color[3]);

            // Text — override wins, then handle
            String text = (override != null && override.getText() != null)
                    ? override.getText()
                    : master.getText();
            if (text != null)
                fontInstance.setText(text);
        }

        // Action
        Runnable resolvedAction = resolveAction(master, override, parentRef);

        // Layout and text overrides stored for read-time fallback
        String textOverride = override != null ? override.getText() : null;
        LayoutStruct layoutOverride = override != null ? override.getLayout() : null;

        // Children
        ObjectArrayList<ElementInstance> childInstances = new ObjectArrayList<>(master.getChildren().size());
        for (ElementPlacementHandle child : master.getChildren())
            childInstances.add(createInstance(child.getMaster(), child.getOverride(), parentRef));

        ElementInstance instance = create(ElementInstance.class);
        instance.constructor(master, spriteInstance, fontInstance,
                textOverride, resolvedAction, layoutOverride, childInstances);
        return instance;
    }

    private Runnable resolveAction(
            ElementHandle master,
            ElementOverrideStruct override,
            Supplier<MenuInstance> parentRef) {

        if (override != null) {
            if (override.getMenuAwareAction() != null) {
                MenuAwareAction maa = override.getMenuAwareAction();
                return () -> maa.execute(parentRef.get());
            }
            if (override.getClickAction() != null)
                return override.getClickAction();
        }

        if (master.hasMenuAwareAction()) {
            MenuAwareAction maa = master.getMenuAwareAction();
            return () -> maa.execute(parentRef.get());
        }

        return null;
    }

    public ElementInstance createDetachedInstance(ElementPlacementHandle placement) {
        return createInstance(placement.getMaster(), placement.getOverride(), () -> null);
    }
}