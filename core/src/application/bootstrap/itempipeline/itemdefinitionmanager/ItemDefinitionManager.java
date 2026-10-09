package application.bootstrap.itempipeline.itemdefinitionmanager;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.util.ItemRegistryUtility;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ItemDefinitionManager extends ManagerPackage {

    /*
     * Owns the item definition palette for the engine lifetime. Items are
     * indexed in registration order, each ID carrying its index in the upper
     * 16 bits, and an item name declared twice is rejected. Supports on-demand
     * loading via ItemDefinitionLoader for items not yet in the palette at
     * runtime. findItemHandle() resolves what a person types — a full item name, or a local or display
     * name that only one item carries — ignoring case. Block pieces are not
     * loaded from files: getBlockPieceHandle() builds a block's piece through
     * BlockPieceBranch the first time it is needed, and a piece's name
     * resolves to its block the same way. The farthest any registered item's
     * shape reaches past the block its corner lies in, before it and after
     * it, tells world queries how far around a block to look for items.
     */

    // Internal
    private BlockManager blockManager;
    private BlockPieceBranch blockPieceBranch;

    // Palette
    private Object2IntOpenHashMap<String> itemName2ItemIndex;
    private ObjectArrayList<ItemDefinitionHandle> itemIndex2ItemHandle;
    private ObjectArrayList<ItemDefinitionHandle> itemHandles;
    private Int2ObjectOpenHashMap<ItemDefinitionHandle> blockID2BlockPieceHandle;

    // Reach
    private int maxReachBefore;
    private int maxReachAfter;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.itemName2ItemIndex = RegistryUtility.createNameIndex();
        this.itemIndex2ItemHandle = RegistryUtility.createPalette();
        this.itemHandles = new ObjectArrayList<>();
        this.blockID2BlockPieceHandle = new Int2ObjectOpenHashMap<>();
        this.blockPieceBranch = create(BlockPieceBranch.class);
        create(ItemDefinitionLoader.class);
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
    }

    // Management \\

    int registerItemName(String itemName) {
        return ItemRegistryUtility.toItemID(RegistryUtility.registerID(
                itemName2ItemIndex, itemIndex2ItemHandle, itemName, EngineSetting.REGISTRY_ITEM_ID_COUNT));
    }

    void addItem(ItemDefinitionHandle item) {

        int itemIndex = ItemRegistryUtility.toItemIndex(item.getItemID());

        if (itemIndex2ItemHandle.get(itemIndex) != null)
            throwException("Duplicate item name: '" + item.getItemName()
                    + "' is declared more than once — every item name must be unique");

        itemIndex2ItemHandle.set(itemIndex, item);
        itemHandles.add(item);

        maxReachBefore = Math.max(maxReachBefore, item.getShape().getReachBefore());
        maxReachAfter = Math.max(maxReachAfter, item.getShape().getReachAfter());
    }

    // Block Pieces \\

    public ItemDefinitionHandle getBlockPieceHandle(short blockID) {

        ItemDefinitionHandle blockPiece = blockID2BlockPieceHandle.get(blockID);

        if (blockPiece != null)
            return blockPiece;

        blockPiece = blockPieceBranch.build(blockManager.getBlockHandleFromBlockID(blockID));
        blockID2BlockPieceHandle.put(blockID, blockPiece);
        addItem(blockPiece);

        return blockPiece;
    }

    // A piece's name names its block — false when that block is unknown or breaks into no pieces
    private boolean requestBlockPiece(String itemName) {

        String blockName = ItemRegistryUtility.toBlockName(itemName);

        if (!blockManager.hasBlock(blockName))
            return false;

        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockName(blockName);

        if (!blockHandle.hasPiece())
            return false;

        getBlockPieceHandle(blockHandle.getBlockID());
        return true;
    }

    // Accessible \\

    private boolean isItemRegistered(String itemName) {
        return RegistryUtility.getHandle(itemName2ItemIndex, itemIndex2ItemHandle, itemName) != null;
    }

    public boolean hasItem(String itemName) {

        if (!isItemRegistered(itemName) && ItemRegistryUtility.isBlockPieceName(itemName))
            requestBlockPiece(itemName);

        return isItemRegistered(itemName);
    }

    public int getItemIDFromItemName(String itemName) {

        if (!isItemRegistered(itemName))
            request(itemName);

        if (!isItemRegistered(itemName))
            throwException("Item name not found after request: " + itemName);

        return ItemRegistryUtility.toItemID(itemName2ItemIndex.getInt(itemName));
    }

    public ItemDefinitionHandle getItemHandleFromItemID(int itemID) {

        ItemDefinitionHandle item = RegistryUtility.getHandle(
                itemIndex2ItemHandle, ItemRegistryUtility.toItemIndex(itemID));

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

    // The most blocks before its corner's block any registered item reaches on an axis
    public int getMaxReachBefore() {
        return maxReachBefore;
    }

    // The most blocks after its corner's block any registered item reaches on an axis
    public int getMaxReachAfter() {
        return maxReachAfter;
    }

    public void request(String itemName) {

        if (ItemRegistryUtility.isBlockPieceName(itemName)) {

            if (!requestBlockPiece(itemName))
                throwException("Block piece '" + itemName + "' names no breakable block.");

            return;
        }

        ((ItemDefinitionLoader) internalLoader).request(itemName);
    }
}