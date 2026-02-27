package com.internal.bootstrap.menupipeline.elementsystem;

import com.internal.bootstrap.menupipeline.element.ElementHandle;
import com.internal.bootstrap.menupipeline.element.ElementInstance;
import com.internal.bootstrap.menupipeline.element.ElementOverrideStruct;
import com.internal.bootstrap.menupipeline.element.ElementPlacementHandle;
import com.internal.bootstrap.menupipeline.element.LayoutStruct;
import com.internal.bootstrap.menupipeline.element.MenuAwareAction;
import com.internal.bootstrap.menupipeline.menu.MenuInstance;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import com.internal.core.engine.SystemPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.function.Supplier;

public class ElementSystem extends SystemPackage {

    private SpriteManager spriteManager;

    // String lookup once at build time — int from there
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

        // Sprite — override wins, clone whichever is chosen
        SpriteHandle sourceSprite = (override != null && override.getSpriteHandle() != null)
                ? override.getSpriteHandle()
                : master.getSpriteHandle();
        SpriteHandle instanceSprite = sourceSprite != null
                ? spriteManager.cloneSprite(sourceSprite.getName())
                : null;

        // Action — bind $parent at spawn time if needed, null falls through to handle
        // at execute()
        Runnable resolvedAction = resolveAction(master, override, parentRef);

        // Text and layout — null means fall back to handle at read time
        String textOverride = override != null ? override.getText() : null;
        LayoutStruct layoutOverride = override != null ? override.getLayout() : null;

        // Children — uniform: every child is a placement carrying its own master +
        // override
        ObjectArrayList<ElementInstance> childInstances = new ObjectArrayList<>(master.getChildren().size());
        for (ElementPlacementHandle child : master.getChildren())
            childInstances.add(createInstance(child.getMaster(), child.getOverride(), parentRef));

        ElementInstance instance = create(ElementInstance.class);
        instance.constructor(master, instanceSprite,
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

        // Normal Runnable — ElementInstance.execute() reads it from the handle directly
        return null;
    }
}