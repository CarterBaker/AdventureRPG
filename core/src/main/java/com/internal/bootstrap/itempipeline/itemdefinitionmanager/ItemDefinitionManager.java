package com.internal.bootstrap.itempipeline.itemdefinitionmanager;

import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ItemDefinitionManager extends ManagerPackage {

    /*
     * Owns the item definition palette for the engine lifetime. Detects and
     * rejects ID collisions on registration. Supports on-demand loading via
     * InternalLoader for items not yet in the palette at runtime.
     */

    // Palette
    private Object2IntOpenHashMap<String> itemName2ItemID;
    private Int2ObjectOpenHashMap<ItemDefinitionHandle> itemID2ItemHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.itemName2ItemID = new Object2IntOpenHashMap<>();
        this.itemID2ItemHandle = new Int2ObjectOpenHashMap<>();
        create(InternalLoader.class);
    }

    // Management \\

    void addItem(ItemDefinitionHandle item) {

        int id = item.getItemID();

        if (itemID2ItemHandle.containsKey(id)) {
            ItemDefinitionHandle existing = itemID2ItemHandle.get(id);
            if (RegistryUtility.isCollision(item.getItemName(), existing.getItemName(), id))
                throwException("Item ID collision: '"
                        + item.getItemName() + "' collides with '"
                        + existing.getItemName()
                        + "' (ID " + id + ") — rename one item to resolve");
        }

        itemName2ItemID.put(item.getItemName(), id);
        itemID2ItemHandle.put(id, item);
    }

    // Accessible \\

    public boolean hasItem(String itemName) {
        return itemName2ItemID.containsKey(itemName);
    }

    public int getItemIDFromItemName(String itemName) {

        if (!itemName2ItemID.containsKey(itemName))
            request(itemName);

        if (!itemName2ItemID.containsKey(itemName))
            throwException("Item name not found after request: " + itemName);

        return itemName2ItemID.getInt(itemName);
    }

    public ItemDefinitionHandle getItemHandleFromItemID(int itemID) {

        ItemDefinitionHandle item = itemID2ItemHandle.get(itemID);

        if (item == null)
            throwException("Item ID not found: " + itemID);

        return item;
    }

    public ItemDefinitionHandle getItemHandleFromItemName(String itemName) {
        return getItemHandleFromItemID(getItemIDFromItemName(itemName));
    }

    public void request(String itemName) {
        ((InternalLoader) internalLoader).request(itemName);
    }
}