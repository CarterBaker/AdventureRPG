package application.bootstrap.itempipeline.itemdefinitionmanager;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ItemDefinitionManager extends ManagerPackage {

    /*
     * Owns the item definition palette for the engine lifetime. Detects and
     * rejects ID collisions on registration. Supports on-demand loading via
     * ItemDefinitionLoader for items not yet in the palette at runtime. findItemHandle()
     * resolves what a person types — a full item name, or a local or display
     * name that only one item carries — ignoring case.
     */

    // Palette
    private Object2IntOpenHashMap<String> itemName2ItemID;
    private Int2ObjectOpenHashMap<ItemDefinitionHandle> itemID2ItemHandle;
    private ObjectArrayList<ItemDefinitionHandle> itemHandles;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.itemName2ItemID = new Object2IntOpenHashMap<>();
        this.itemID2ItemHandle = new Int2ObjectOpenHashMap<>();
        this.itemHandles = new ObjectArrayList<>();
        create(ItemDefinitionLoader.class);
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
        itemHandles.add(item);
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

    public ItemDefinitionHandle findItemHandle(String query) {

        ItemDefinitionHandle match = null;

        for (int i = 0; i < itemHandles.size(); i++) {

            ItemDefinitionHandle item = itemHandles.get(i);

            if (item.getItemName().equalsIgnoreCase(query))
                return item;

            if (!item.getLocalName().equalsIgnoreCase(query) && !item.getDisplayName().equalsIgnoreCase(query))
                continue;

            if (match != null && match != item)
                return null;

            match = item;
        }

        return match;
    }

    public ObjectArrayList<ItemDefinitionHandle> getItemHandles() {
        return itemHandles;
    }

    public void request(String itemName) {
        ((ItemDefinitionLoader) internalLoader).request(itemName);
    }
}