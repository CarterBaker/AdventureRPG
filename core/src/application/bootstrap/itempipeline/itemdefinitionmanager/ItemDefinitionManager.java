package application.bootstrap.itempipeline.itemdefinitionmanager;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.util.ItemRegistryUtility;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
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
    private Object2IntOpenHashMap<String> itemName2ItemID;
    private Int2ObjectOpenHashMap<ItemDefinitionHandle> itemID2ItemHandle;
    private ObjectArrayList<ItemDefinitionHandle> itemHandles;
    private Int2ObjectOpenHashMap<ItemDefinitionHandle> blockID2BlockPieceHandle;

    // Reach
    private int maxReachBefore;
    private int maxReachAfter;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.itemName2ItemID = new Object2IntOpenHashMap<>();
        this.itemID2ItemHandle = new Int2ObjectOpenHashMap<>();
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

    public boolean hasItem(String itemName) {

        if (!itemName2ItemID.containsKey(itemName) && ItemRegistryUtility.isBlockPieceName(itemName))
            requestBlockPiece(itemName);

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