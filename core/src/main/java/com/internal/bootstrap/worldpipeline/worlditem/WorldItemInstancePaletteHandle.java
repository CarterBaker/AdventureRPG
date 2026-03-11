package com.internal.bootstrap.worldpipeline.worlditem;

import com.internal.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldItemInstancePaletteHandle extends HandlePackage {

    // Flat list for render iteration
    private ObjectArrayList<WorldItemInstance> items;

    // Block coordinate lookup for placement validation and pickup
    private Int2ObjectOpenHashMap<ObjectArrayList<WorldItemInstance>> blockCoord2Items;

    // Constructor \\

    public void constructor() {
        this.items = new ObjectArrayList<>();
        this.blockCoord2Items = new Int2ObjectOpenHashMap<>();
    }

    // Management \\

    public void addItem(WorldItemInstance item) {
        items.add(item);
        int packedBlock = item.getPackedBlockCoordinate();
        ObjectArrayList<WorldItemInstance> blockList = blockCoord2Items.get(packedBlock);
        if (blockList == null) {
            blockList = new ObjectArrayList<>();
            blockCoord2Items.put(packedBlock, blockList);
        }
        blockList.add(item);
    }

    public void removeItem(WorldItemInstance item) {
        items.remove(item);
        int packedBlock = item.getPackedBlockCoordinate();
        ObjectArrayList<WorldItemInstance> blockList = blockCoord2Items.get(packedBlock);
        if (blockList == null)
            return;
        blockList.remove(item);
        if (blockList.isEmpty())
            blockCoord2Items.remove(packedBlock);
    }

    public void clear() {
        items.clear();
        blockCoord2Items.clear();
    }

    // Accessible \\

    public ObjectArrayList<WorldItemInstance> getItems() {
        return items;
    }

    public ObjectArrayList<WorldItemInstance> getItemsAtBlock(int packedBlockCoordinate) {
        return blockCoord2Items.get(packedBlockCoordinate);
    }

    public boolean hasItemsAtBlock(int packedBlockCoordinate) {
        ObjectArrayList<WorldItemInstance> list = blockCoord2Items.get(packedBlockCoordinate);
        return list != null && !list.isEmpty();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }
}