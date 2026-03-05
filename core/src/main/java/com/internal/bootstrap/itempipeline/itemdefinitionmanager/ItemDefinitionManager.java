package com.internal.bootstrap.itempipeline.itemdefinitionmanager;

import com.internal.bootstrap.itempipeline.util.ItemRegistryUtility;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ItemDefinitionManager extends ManagerPackage {

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> itemName2ItemID;
    private Int2ObjectOpenHashMap<ItemDefinitionData> itemID2Item;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoadManager.class);
        this.itemName2ItemID = new Object2IntOpenHashMap<>();
        this.itemID2Item = new Int2ObjectOpenHashMap<>();
    }

    // On-Demand Loading \\

    public void request(String itemName) {
        ((InternalLoadManager) internalLoader).request(itemName);
    }

    // Item Management \\

    void addItem(ItemDefinitionData item) {
        if (itemID2Item.containsKey(item.getItemID())) {
            ItemDefinitionData existing = itemID2Item.get(item.getItemID());
            if (ItemRegistryUtility.isCollision(item.getItemName(), existing.getItemName(), item.getItemID()))
                throwException("Item ID collision: '"
                        + item.getItemName() + "' collides with '"
                        + existing.getItemName() + "' (ID " + item.getItemID() + ") — rename one item to resolve");
        }
        itemName2ItemID.put(item.getItemName(), item.getItemID());
        itemID2Item.put(item.getItemID(), item);
    }

    // Accessible \\

    public boolean hasItem(String itemName) {
        return itemName2ItemID.containsKey(itemName);
    }

    public int getItemIDFromItemName(String itemName) {
        if (!itemName2ItemID.containsKey(itemName))
            request(itemName);
        return itemName2ItemID.getInt(itemName);
    }

    public ItemDefinitionData getItemFromItemID(int itemID) {
        ItemDefinitionData item = itemID2Item.get(itemID);
        if (item == null)
            throwException("Item ID not found: " + itemID);
        return item;
    }
}