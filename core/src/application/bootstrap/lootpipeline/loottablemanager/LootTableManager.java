package application.bootstrap.lootpipeline.loottablemanager;

import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.bootstrap.lootpipeline.loottable.LootEntryStruct;
import application.bootstrap.lootpipeline.loottable.LootTableHandle;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.WeightedTableUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LootTableManager extends ManagerPackage {

    /*
     * Owns the loot table palette and is the one way loot is drawn. Every
     * table is resolved in awake(), before any vehicle enters the world or any
     * chunk generates. drawLoot() turns one draw of a table — every entry it
     * guarantees, then its rolls — into real items through ItemManager, each
     * salted by the seed it is given and the table's name seed, so the same
     * seed always draws the same loot; fillContainer() packs one draw into a
     * container item, topping up the stacks already inside first, and
     * whatever finds no room is left out. Loot table IDs are assigned in
     * registration order.
     */

    // Internal
    private ItemManager itemManager;

    // Palette
    private Object2IntOpenHashMap<String> lootTableName2LootTableID;
    private ObjectArrayList<LootTableHandle> lootTableID2LootTableHandle;

    // Scratch
    private ObjectArrayList<ItemInstance> lootScratch;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.lootTableName2LootTableID = RegistryUtility.createNameIndex();
        this.lootTableID2LootTableHandle = RegistryUtility.createPalette();

        // Scratch
        this.lootScratch = new ObjectArrayList<>();

        create(LootTableLoader.class);
    }

    @Override
    protected void get() {
        this.itemManager = get(ItemManager.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    short registerLootTableName(String lootTableName) {
        return (short) RegistryUtility.registerID(
                lootTableName2LootTableID, lootTableID2LootTableHandle, lootTableName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addLootTableHandle(LootTableHandle lootTableHandle) {

        short lootTableID = lootTableHandle.getLootTableID();

        if (lootTableID2LootTableHandle.get(lootTableID) != null)
            throwException("Duplicate loot table name: '" + lootTableHandle.getLootTableName()
                    + "' was registered more than once");

        lootTableID2LootTableHandle.set(lootTableID, lootTableHandle);
    }

    // On-Demand \\

    public void request(String lootTableName) {
        ((LootTableLoader) internalLoader).request(lootTableName);
    }

    // Draw \\

    // Every item one draw of a table yields, appended to the list given — the same seed always draws the same
    public void drawLoot(short lootTableID, long seed, ObjectArrayList<ItemInstance> out) {

        LootTableHandle lootTableHandle = getLootTableHandleFromLootTableID(lootTableID);
        int nameSeed = lootTableHandle.getNameSeed();

        addGuaranteed(lootTableHandle.getGuaranteedEntries(), seed, nameSeed, out);

        if (lootTableHandle.hasRolls())
            addRolls(lootTableHandle, seed, nameSeed, out);
    }

    private void addGuaranteed(
            LootEntryStruct[] guaranteedEntries,
            long seed,
            int nameSeed,
            ObjectArrayList<ItemInstance> out) {

        for (int i = 0; i < guaranteedEntries.length; i++)
            addEntry(
                    guaranteedEntries[i],
                    BiomeFieldUtility.hashCell(seed ^ EngineSetting.LOOT_GUARANTEED_SALT, i, nameSeed),
                    out);
    }

    private void addRolls(
            LootTableHandle lootTableHandle,
            long seed,
            int nameSeed,
            ObjectArrayList<ItemInstance> out) {

        LootEntryStruct[] entries = lootTableHandle.getEntries();
        int rolls = drawCount(
                lootTableHandle.getRollsMin(),
                lootTableHandle.getRollsMax(),
                BiomeFieldUtility.hashCell(seed ^ EngineSetting.LOOT_ROLLS_SALT, nameSeed, nameSeed));

        for (int roll = 0; roll < rolls; roll++) {

            long rollSeed = BiomeFieldUtility.hashCell(seed ^ EngineSetting.LOOT_ROLL_SALT, roll, nameSeed);
            int index = WeightedTableUtility.pick(
                    lootTableHandle.getCumulativeWeights(),
                    BiomeFieldUtility.hash01(rollSeed));

            if (index < entries.length)
                addEntry(entries[index], rollSeed, out);
        }
    }

    // An entry's count drawn and given as whole stacks, each as full as the item's stack size allows
    private void addEntry(LootEntryStruct entry, long seed, ObjectArrayList<ItemInstance> out) {

        ItemDefinitionHandle itemDefinitionHandle = entry.getItemDefinitionHandle();
        int stackSize = itemDefinitionHandle.getStackSize();
        int count = drawCount(entry.getCountMin(), entry.getCountMax(), seed ^ EngineSetting.LOOT_COUNT_SALT);

        for (int remaining = count; remaining > 0; remaining -= stackSize)
            out.add(itemManager.createStack(itemDefinitionHandle, Math.min(remaining, stackSize)));
    }

    // One draw of a table packed into a container item — nothing happens to an item that holds nothing
    public void fillContainer(ItemInstance containerItem, short lootTableID, long seed) {

        if (!containerItem.hasContainer())
            return;

        ContainerInstance containerInstance = containerItem.getContainerInstance();

        lootScratch.clear();
        drawLoot(lootTableID, seed, lootScratch);

        for (int i = 0; i < lootScratch.size(); i++) {

            ItemInstance itemInstance = lootScratch.get(i);

            if (!containerInstance.mergeStack(itemInstance))
                containerInstance.autoPlace(itemInstance);
        }

        lootScratch.clear();
    }

    // A whole count from the least to the most, each as likely as the next
    private int drawCount(int min, int max, long seed) {

        int span = max - min + 1;

        return min + Math.min(span - 1, (int) (BiomeFieldUtility.hash01(seed) * span));
    }

    // Accessible \\

    public boolean hasLootTable(String lootTableName) {
        return RegistryUtility.getHandle(
                lootTableName2LootTableID, lootTableID2LootTableHandle, lootTableName) != null;
    }

    public LootTableHandle getLootTableHandleFromLootTableName(String lootTableName) {

        LootTableHandle handle = RegistryUtility.getHandle(
                lootTableName2LootTableID, lootTableID2LootTableHandle, lootTableName);

        if (handle == null) {
            request(lootTableName);
            handle = RegistryUtility.getHandle(
                    lootTableName2LootTableID, lootTableID2LootTableHandle, lootTableName);
        }

        if (handle == null)
            throwException("Loot table \"" + lootTableName + "\" was not registered after its on-demand load "
                    + "completed — check for a resource-name/path mismatch in the loot table directory.");

        return handle;
    }

    public short getLootTableIDFromLootTableName(String lootTableName) {
        return getLootTableHandleFromLootTableName(lootTableName).getLootTableID();
    }

    public LootTableHandle getLootTableHandleFromLootTableID(short lootTableID) {

        LootTableHandle handle = RegistryUtility.getHandle(lootTableID2LootTableHandle, lootTableID);

        if (handle == null)
            throwException("No handle registered for loot table ID: " + lootTableID);

        return handle;
    }
}
