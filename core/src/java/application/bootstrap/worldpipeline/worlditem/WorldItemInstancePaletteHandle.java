package application.bootstrap.worldpipeline.worlditem;

import engine.root.HandlePackage;
import engine.util.mathematics.extras.Coordinate3Int;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldItemInstancePaletteHandle extends HandlePackage {

    /*
     * A chunk's runtime world items. The streaming thread stages a fresh build
     * under the chunk's lock and the main thread commits it, so the live
     * palette is only ever read and written on the main thread and every
     * space query reads it without a lock. Live items are kept as a flat list
     * for rendering, and indexed by the block and the block column holding
     * their model grid's corner, so a query only visits the items that can
     * reach it. Index lists are pooled, so streaming allocates nothing new.
     */

    // Staged — built on the streaming thread under the chunk's lock
    private ObjectArrayList<WorldItemInstance> stagedItems;
    private boolean staged;

    // Live — main thread only
    private ObjectArrayList<WorldItemInstance> items;
    private Int2ObjectOpenHashMap<ObjectArrayList<WorldItemInstance>> blockCoord2Items;
    private Int2ObjectOpenHashMap<ObjectArrayList<WorldItemInstance>> columnCoord2Items;
    private boolean committed;

    // Pool
    private ObjectArrayList<ObjectArrayList<WorldItemInstance>> itemListPool;

    // Constructor \\

    public void constructor() {

        // Staged
        this.stagedItems = new ObjectArrayList<>();
        this.staged = false;

        // Live
        this.items = new ObjectArrayList<>();
        this.blockCoord2Items = new Int2ObjectOpenHashMap<>();
        this.columnCoord2Items = new Int2ObjectOpenHashMap<>();
        this.committed = false;

        // Pool
        this.itemListPool = new ObjectArrayList<>();
    }

    // Stage \\

    public void beginStage() {
        stagedItems.clear();
        staged = true;
    }

    public void stageItem(WorldItemInstance item) {
        stagedItems.add(item);
    }

    // Replaces the live palette with the staged build — main thread, under the chunk's lock
    public void commitStage() {

        if (!staged)
            return;

        clearLive();

        for (int i = 0; i < stagedItems.size(); i++)
            addItem(stagedItems.get(i));

        stagedItems.clear();
        staged = false;
        committed = true;
    }

    // Management \\

    public void addItem(WorldItemInstance item) {
        items.add(item);
        addToIndex(blockCoord2Items, item.getPackedBlockCoordinate(), item);
        addToIndex(columnCoord2Items, toColumnCoordinate(item.getPackedBlockCoordinate()), item);
    }

    public void removeItem(WorldItemInstance item) {
        items.remove(item);
        removeFromIndex(blockCoord2Items, item.getPackedBlockCoordinate(), item);
        removeFromIndex(columnCoord2Items, toColumnCoordinate(item.getPackedBlockCoordinate()), item);
    }

    public void clear() {
        clearLive();
        stagedItems.clear();
        staged = false;
        committed = false;
    }

    private void clearLive() {
        items.clear();
        releaseIndex(blockCoord2Items);
        releaseIndex(columnCoord2Items);
    }

    // Index \\

    private void addToIndex(
            Int2ObjectOpenHashMap<ObjectArrayList<WorldItemInstance>> index,
            int key,
            WorldItemInstance item) {

        ObjectArrayList<WorldItemInstance> list = index.get(key);

        if (list == null) {
            list = itemListPool.isEmpty() ? new ObjectArrayList<>() : itemListPool.pop();
            index.put(key, list);
        }

        list.add(item);
    }

    private void removeFromIndex(
            Int2ObjectOpenHashMap<ObjectArrayList<WorldItemInstance>> index,
            int key,
            WorldItemInstance item) {

        ObjectArrayList<WorldItemInstance> list = index.get(key);

        if (list == null)
            return;

        list.remove(item);

        if (!list.isEmpty())
            return;

        index.remove(key);
        itemListPool.push(list);
    }

    private void releaseIndex(Int2ObjectOpenHashMap<ObjectArrayList<WorldItemInstance>> index) {

        for (ObjectArrayList<WorldItemInstance> list : index.values()) {
            list.clear();
            itemListPool.push(list);
        }

        index.clear();
    }

    // The block column of a packed corner block, keyed with the same packing at the column's base
    private static int toColumnCoordinate(int packedBlockCoordinate) {
        return Coordinate3Int.pack(
                Coordinate3Int.unpackX(packedBlockCoordinate),
                0,
                Coordinate3Int.unpackZ(packedBlockCoordinate));
    }

    // Accessible \\

    public ObjectArrayList<WorldItemInstance> getItems() {
        return items;
    }

    public boolean contains(WorldItemInstance item) {
        return items.contains(item);
    }

    // Items whose model grid's corner lies in this chunk-local block, null when none
    public ObjectArrayList<WorldItemInstance> getItemsAtBlock(int blockX, int blockY, int blockZ) {
        return blockCoord2Items.get(Coordinate3Int.pack(blockX, blockY, blockZ));
    }

    // Items whose model grid's corner lies in this chunk-local block column, null when none
    public ObjectArrayList<WorldItemInstance> getItemsAtColumn(int blockX, int blockZ) {
        return columnCoord2Items.get(Coordinate3Int.pack(blockX, 0, blockZ));
    }

    public boolean isCommitted() {
        return committed;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }
}
