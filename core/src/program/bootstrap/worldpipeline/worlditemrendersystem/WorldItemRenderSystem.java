package program.bootstrap.worldpipeline.worlditemrendersystem;

import program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import program.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import program.bootstrap.renderpipeline.rendermanager.RenderManager;
import program.bootstrap.renderpipeline.window.WindowInstance;
import program.bootstrap.renderpipeline.windowmanager.WindowManager;
import program.bootstrap.shaderpipeline.material.MaterialInstance;
import program.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import program.bootstrap.worldpipeline.worlditem.WorldItemCompositeInstance;
import program.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import program.core.engine.ManagerPackage;
import program.core.settings.EngineSetting;
import program.core.util.mathematics.extrasa.Coordinate2Long;
import program.core.util.mathematics.extrasa.Coordinate4Long;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Owns all composite buffer state for world items.
 * Push and pull operate on entire chunks — O(1) chunk lookup via coord map.
 * Single instance add/remove available for runtime placement.
 * Submit loop each frame pushes live buffers to the render system.
 *
 * Swap-remove fixup uses itemDefID2SlotMap (slot → instance per composite)
 * so displaced instances are always found in O(1) regardless of which chunk
 * they belong to. chunkCoord2Items is purely for chunk-scoped bookkeeping.
 */
public class WorldItemRenderSystem extends ManagerPackage {

    private static final int[] INSTANCE_ATTR_SIZES = { 4, 2 };

    private MaterialManager materialManager;
    private CompositeBufferManager compositeBufferManager;
    private RenderManager renderSystem;
    private WindowManager windowManager;

    // Per item definition — composite buffer + material
    private Int2ObjectOpenHashMap<WorldItemCompositeInstance> itemDefID2Composite;

    // Per item definition — slot → instance, kept in sync with the composite buffer
    private Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<WorldItemInstance>> itemDefID2SlotMap;

    // Per chunk — tracks which instances belong to each chunk for O(1) pull
    private Long2ObjectOpenHashMap<ObjectArrayList<WorldItemInstance>> chunkCoord2Items;

    // Internal \\

    @Override
    protected void create() {
        this.itemDefID2Composite = new Int2ObjectOpenHashMap<>();
        this.itemDefID2SlotMap = new Int2ObjectOpenHashMap<>();
        this.chunkCoord2Items = new Long2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
        this.compositeBufferManager = get(CompositeBufferManager.class);
        this.renderSystem = get(RenderManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void update() {
        if (windowManager.getWindows().isEmpty())
            return;

        for (var entry : itemDefID2Composite.int2ObjectEntrySet()) {
            WorldItemCompositeInstance composite = entry.getValue();
            CompositeBufferInstance buffer = composite.getCompositeBuffer();

            if (buffer.isEmpty())
                continue;

            for (int i = 0; i < windowManager.getWindows().size(); i++) {
                WindowInstance window = windowManager.getWindows().get(i);
                renderSystem.pushCompositeCall(composite.getMaterial(), buffer, window);
            }
        }
    }

    // Chunk Push / Pull \\

    /*
     * Adds all items for a chunk into the composite buffers in one call.
     * Stores the list under the chunk coordinate for O(1) pull later.
     */
    public void push(long chunkCoordinate, ObjectArrayList<WorldItemInstance> items) {
        if (items.isEmpty())
            return;
        debug("pushing chunk to renderer: " + Coordinate2Long.toString(chunkCoordinate));
        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);
        ObjectArrayList<WorldItemInstance> stored = new ObjectArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            WorldItemInstance instance = items.get(i);
            addToBuffer(instance, chunkX, chunkZ);
            stored.add(instance);
        }
        chunkCoord2Items.put(chunkCoordinate, stored);
    }

    /*
     * Removes all items for a chunk from the composite buffers in one call.
     * O(1) chunk lookup, O(n) over items in that chunk only.
     */
    public void pull(long chunkCoordinate) {
        ObjectArrayList<WorldItemInstance> items = chunkCoord2Items.remove(chunkCoordinate);
        if (items == null)
            return;
        debug("pulling chunk from renderer: " + Coordinate2Long.toString(chunkCoordinate));
        for (int i = 0; i < items.size(); i++)
            removeFromBuffer(items.get(i));
    }

    // Runtime Single Instance \\

    public void addItem(WorldItemInstance instance, long chunkCoordinate) {
        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);
        addToBuffer(instance, chunkX, chunkZ);
        chunkCoord2Items
                .computeIfAbsent(chunkCoordinate, k -> new ObjectArrayList<>())
                .add(instance);
    }

    public void removeItem(WorldItemInstance instance) {
        removeFromBuffer(instance);
        ObjectArrayList<WorldItemInstance> list = chunkCoord2Items.get(instance.getChunkCoordinate());
        if (list != null)
            list.remove(instance);
    }

    // Buffer \\

    private void addToBuffer(WorldItemInstance instance, int chunkX, int chunkZ) {
        long packed = instance.getPackedPosition();
        int subX = Coordinate4Long.unpackX(packed);
        int subY = Coordinate4Long.unpackY(packed);
        int subZ = Coordinate4Long.unpackZ(packed);
        int orientation = Coordinate4Long.unpackW(packed);

        float svr = EngineSetting.SUB_VOXEL_RESOLUTION;
        WorldItemCompositeInstance composite = getOrCreateComposite(instance.getItemDefinitionHandle());
        int itemDefID = instance.getItemDefinitionHandle().getItemID();

        float[] data = {
                Float.intBitsToFloat(chunkX),
                Float.intBitsToFloat(chunkZ),
                subX / svr, subZ / svr,
                subY / svr, orientation
        };
        int slot = composite.getCompositeBuffer().addInstance(data);
        instance.setInstanceSlot(slot);

        // Register in the slot map so swap-remove fixup is O(1) and cross-chunk correct
        itemDefID2SlotMap
                .computeIfAbsent(itemDefID, k -> new Int2ObjectOpenHashMap<>())
                .put(slot, instance);
    }

    /*
     * Removes the instance from its composite buffer.
     * If the buffer performed a swap-remove, the displaced instance is found
     * via the slot map in O(1) and its slot is updated in place — no chunk scan.
     */
    private void removeFromBuffer(WorldItemInstance instance) {
        int itemDefID = instance.getItemDefinitionHandle().getItemID();
        WorldItemCompositeInstance composite = itemDefID2Composite.get(itemDefID);
        if (composite == null)
            return;
        int slot = instance.getInstanceSlot();
        if (slot == -1)
            return;

        CompositeBufferInstance buffer = composite.getCompositeBuffer();
        int movedFromSlot = buffer.removeInstance(slot);
        instance.clearInstanceSlot();

        Int2ObjectOpenHashMap<WorldItemInstance> slotMap = itemDefID2SlotMap.get(itemDefID);
        if (slotMap == null)
            return;

        slotMap.remove(slot);

        if (slot != movedFromSlot) {
            // The instance that occupied movedFromSlot is now at slot — update it
            WorldItemInstance displaced = slotMap.remove(movedFromSlot);
            if (displaced != null) {
                displaced.setInstanceSlot(slot);
                slotMap.put(slot, displaced);
            }
        }
    }

    // Composite \\

    private WorldItemCompositeInstance getOrCreateComposite(ItemDefinitionHandle def) {
        int itemDefID = def.getItemID();
        WorldItemCompositeInstance composite = itemDefID2Composite.get(itemDefID);
        if (composite == null) {
            MaterialInstance material = materialManager.cloneMaterial(def.getMaterialID());
            CompositeBufferInstance buffer = create(CompositeBufferInstance.class);
            compositeBufferManager.constructor(buffer, def.getMeshHandle(), INSTANCE_ATTR_SIZES);
            composite = create(WorldItemCompositeInstance.class);
            composite.constructor(material, buffer);
            itemDefID2Composite.put(itemDefID, composite);
        }
        return composite;
    }
}