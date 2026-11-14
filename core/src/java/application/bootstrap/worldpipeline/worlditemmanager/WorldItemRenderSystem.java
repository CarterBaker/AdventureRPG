package application.bootstrap.worldpipeline.worlditemmanager;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worlditem.WorldItemCompositeInstance;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate4Long;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class WorldItemRenderSystem extends SystemPackage {

    /*
     * Owns the composite buffers for world items. Chunks push and pull their
     * items in O(1) by coordinate, single items add and remove at runtime with
     * swap-remove fixups, and every frame each grid's buffers are pushed into
     * its world target so items are depth tested and lit with the terrain. An
     * item can be hidden while it stays placed — an open chest is drawn open
     * by whoever opened it — and is shown again from its chunk's list. Chunk
     * lists are pooled and instance data is staged in one scratch array, so
     * streaming items in and out allocates nothing. A chunk pushed again first
     * takes back what it pushed before, so no item is ever left in a buffer
     * without a chunk to pull it.
     */

    private static final int[] INSTANCE_ATTR_SIZES = { 4, 2 };

    private MaterialManager materialManager;
    private CompositeBufferManager compositeBufferManager;
    private RenderManager renderSystem;
    private WorldStreamManager worldStreamManager;

    // Per item definition — composite buffer + material
    private Int2ObjectOpenHashMap<WorldItemCompositeInstance> itemDefID2Composite;

    // Per item definition — slot → instance, kept in sync with the composite buffer
    private Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<WorldItemInstance>> itemDefID2SlotMap;

    // Per chunk — tracks which instances belong to each chunk for O(1) pull
    private Long2ObjectOpenHashMap<ObjectArrayList<WorldItemInstance>> chunkCoord2Items;
    private ObjectArrayList<ObjectArrayList<WorldItemInstance>> itemListPool;

    // Scratch
    private float[] instanceDataScratch;

    // Internal \\

    @Override
    protected void create() {
        this.itemDefID2Composite = new Int2ObjectOpenHashMap<>();
        this.itemDefID2SlotMap = new Int2ObjectOpenHashMap<>();
        this.chunkCoord2Items = new Long2ObjectOpenHashMap<>();
        this.itemListPool = new ObjectArrayList<>();
        this.instanceDataScratch = new float[countInstanceFloats()];
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
        this.compositeBufferManager = get(CompositeBufferManager.class);
        this.renderSystem = get(RenderManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void update() {
        if (!worldStreamManager.hasGrids())
            return;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();

        ObjectIterator<Int2ObjectMap.Entry<WorldItemCompositeInstance>> iterator = itemDefID2Composite
                .int2ObjectEntrySet()
                .fastIterator();

        while (iterator.hasNext()) {
            WorldItemCompositeInstance composite = iterator.next().getValue();
            CompositeBufferInstance buffer = composite.getCompositeBuffer();

            if (buffer.isEmpty())
                continue;

            for (int i = 0; i < grids.size(); i++) {
                WindowInstance window = grids.get(i).getWindowInstance();
                FBOInstance worldFbo = grids.get(i).getRenderTargetFbo();

                if (window == null || worldFbo == null)
                    continue;

                renderSystem.ensureFboRendered(worldFbo, window);
                renderSystem.pushCompositeCall(composite.getMaterial(), buffer, worldFbo, window);
            }
        }
    }

    // Chunk Push / Pull \\

    public void push(long chunkCoordinate, ObjectArrayList<WorldItemInstance> items) {
        pull(chunkCoordinate);
        if (items.isEmpty())
            return;
        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);
        ObjectArrayList<WorldItemInstance> stored = acquireItemList();
        for (int i = 0; i < items.size(); i++) {
            WorldItemInstance instance = items.get(i);
            addToBuffer(instance, chunkX, chunkZ);
            stored.add(instance);
        }
        chunkCoord2Items.put(chunkCoordinate, stored);
    }

    public void pull(long chunkCoordinate) {
        ObjectArrayList<WorldItemInstance> items = chunkCoord2Items.remove(chunkCoordinate);
        if (items == null)
            return;
        for (int i = 0; i < items.size(); i++)
            removeFromBuffer(items.get(i));

        items.clear();
        itemListPool.push(items);
    }

    private ObjectArrayList<WorldItemInstance> acquireItemList() {
        return itemListPool.isEmpty() ? new ObjectArrayList<>() : itemListPool.pop();
    }

    // Runtime Single Instance \\

    public void addItem(WorldItemInstance instance, long chunkCoordinate) {
        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);
        addToBuffer(instance, chunkX, chunkZ);

        ObjectArrayList<WorldItemInstance> items = chunkCoord2Items.get(chunkCoordinate);

        if (items == null) {
            items = acquireItemList();
            chunkCoord2Items.put(chunkCoordinate, items);
        }

        items.add(instance);
    }

    public void removeItem(WorldItemInstance instance) {
        removeFromBuffer(instance);
        ObjectArrayList<WorldItemInstance> list = chunkCoord2Items.get(instance.getChunkCoordinate());
        if (list != null)
            list.remove(instance);
    }

    // Visibility \\

    public void hideItem(WorldItemInstance instance) {
        removeFromBuffer(instance);
    }

    // Returns the item to its buffer, unless its chunk has left the renderer meanwhile
    public void showItem(WorldItemInstance instance) {

        ObjectArrayList<WorldItemInstance> list = chunkCoord2Items.get(instance.getChunkCoordinate());

        if (instance.getInstanceSlot() != -1 || list == null || !list.contains(instance))
            return;

        addToBuffer(
                instance,
                Coordinate2Long.unpackX(instance.getChunkCoordinate()),
                Coordinate2Long.unpackY(instance.getChunkCoordinate()));
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

        float[] data = instanceDataScratch;
        data[0] = Float.intBitsToFloat(chunkX);
        data[1] = Float.intBitsToFloat(chunkZ);
        data[2] = subX / svr;
        data[3] = subZ / svr;
        data[4] = subY / svr;
        data[5] = orientation;

        int slot = composite.getCompositeBuffer().addInstance(data);
        instance.setInstanceSlot(slot);

        // Register in the slot map so swap-remove fixup is O(1) and cross-chunk correct
        itemDefID2SlotMap
                .computeIfAbsent(itemDefID, k -> new Int2ObjectOpenHashMap<>())
                .put(slot, instance);
    }

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

    private static int countInstanceFloats() {

        int count = 0;

        for (int attributeSize : INSTANCE_ATTR_SIZES)
            count += attributeSize;

        return count;
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
