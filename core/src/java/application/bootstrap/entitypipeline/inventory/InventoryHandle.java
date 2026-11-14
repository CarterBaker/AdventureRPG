package application.bootstrap.entitypipeline.inventory;

import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.EquipmentType;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class InventoryHandle extends HandlePackage {

    /*
     * Per-entity inventory: the item in every equipment slot and which slots
     * are hidden. give() is the one path an item is handed over: a stack tops
     * up the matching stacks already carried when they have room for all of
     * it, and anything else goes into the backpack if it fits and otherwise a
     * free hand — so a give either takes the whole item or nothing at all.
     * wear() puts an item on only in a free slot made for its kind. Gear above
     * the wield level — the highest item level its owner's level reaches — is
     * never worn, though the hands still carry it. takeOne() is the one path a
     * single item leaves a slot, split off its stack through ItemManager. The
     * revision counts every change so views know when to redraw and stats when
     * to rebuild. Lives on EntityInstance.
     */

    // Internal
    private ItemManager itemManager;

    // Equipment
    private ItemInstance[] slot2Item;
    private boolean[] slot2Hidden;

    // Wield
    private int wieldLevel;

    // Revision
    private int revision;

    // Internal \\

    @Override
    protected void create() {

        // Equipment
        this.slot2Item = new ItemInstance[EquipmentSlot.VALUES.length];
        this.slot2Hidden = new boolean[EquipmentSlot.VALUES.length];

        // Wield
        this.wieldLevel = EngineSetting.LEVEL_START;
    }

    @Override
    protected void get() {

        // Internal
        this.itemManager = get(ItemManager.class);
    }

    // Equipment \\

    public boolean canEquip(EquipmentSlot equipmentSlot, ItemInstance itemInstance) {

        if (hasItem(equipmentSlot) || !equipmentSlot.accepts(itemInstance.getItemDefinitionHandle()))
            return false;

        if (!equipmentSlot.isHand() && isAboveWieldLevel(itemInstance))
            return false;

        boolean twoHanded = itemInstance.getItemDefinitionHandle().isTwoHanded();

        return switch (equipmentSlot) {
            case MAIN_HAND -> !twoHanded || !hasItem(EquipmentSlot.OFF_HAND);
            case OFF_HAND -> !twoHanded && !isHoldingTwoHanded();
            default -> true;
        };
    }

    public void equip(EquipmentSlot equipmentSlot, ItemInstance itemInstance) {

        if (!canEquip(equipmentSlot, itemInstance))
            throwException("Item '" + itemInstance.getItemDefinitionHandle().getItemName()
                    + "' cannot be equipped in slot " + equipmentSlot + ".");

        slot2Item[equipmentSlot.ordinal()] = itemInstance;
        revision++;
    }

    public ItemInstance unequip(EquipmentSlot equipmentSlot) {

        ItemInstance itemInstance = slot2Item[equipmentSlot.ordinal()];
        slot2Item[equipmentSlot.ordinal()] = null;
        revision++;

        return itemInstance;
    }

    // Takes one item from a slot: the item itself when it is the last of its stack, otherwise one split off
    public ItemInstance takeOne(EquipmentSlot equipmentSlot) {

        ItemInstance itemInstance = getItem(equipmentSlot);

        if (itemInstance == null || itemInstance.getStackCount() == 1)
            return unequip(equipmentSlot);

        revision++;

        return itemManager.splitStack(itemInstance, 1);
    }

    public EquipmentSlot findSlot(ItemInstance itemInstance) {

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES)
            if (slot2Item[equipmentSlot.ordinal()] == itemInstance)
                return equipmentSlot;

        return null;
    }

    public void clear() {

        for (int i = 0; i < slot2Item.length; i++) {
            slot2Item[i] = null;
            slot2Hidden[i] = false;
        }

        revision++;
    }

    // Carry \\

    // Puts an item on where it is worn — the first free slot of its own type, so a weapon goes in the main hand and
    // a shield in the off hand — false when no such slot is free
    public boolean wear(ItemInstance itemInstance) {

        EquipmentType equipmentType = itemInstance.getItemDefinitionHandle().getEquipmentType();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {

            if (equipmentSlot.getEquipmentType() != equipmentType || !canEquip(equipmentSlot, itemInstance))
                continue;

            equip(equipmentSlot, itemInstance);
            return true;
        }

        return false;
    }

    public boolean give(ItemInstance itemInstance) {

        if (getStackRoom(itemInstance) >= itemInstance.getStackCount()) {
            mergeStack(itemInstance);
            return true;
        }

        if (hasBackpack() && getBackpackContainer().autoPlace(itemInstance) != null)
            return true;

        return hold(itemInstance);
    }

    public boolean hold(ItemInstance itemInstance) {

        if (canEquip(EquipmentSlot.MAIN_HAND, itemInstance)) {
            equip(EquipmentSlot.MAIN_HAND, itemInstance);
            return true;
        }

        if (canEquip(EquipmentSlot.OFF_HAND, itemInstance)) {
            equip(EquipmentSlot.OFF_HAND, itemInstance);
            return true;
        }

        return false;
    }

    // How many more of an item the stacks already carried, in hand or in the backpack, can take
    private int getStackRoom(ItemInstance itemInstance) {

        if (!itemInstance.getItemDefinitionHandle().isStackable())
            return 0;

        int room = hasBackpack() ? getBackpackContainer().getStackRoom(itemInstance) : 0;

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {

            ItemInstance carried = slot2Item[equipmentSlot.ordinal()];

            if (equipmentSlot.isHand() && carried != null && carried.stacksWith(itemInstance))
                room += carried.getStackRoom();
        }

        return room;
    }

    private void mergeStack(ItemInstance itemInstance) {

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {

            ItemInstance carried = slot2Item[equipmentSlot.ordinal()];

            if (!equipmentSlot.isHand() || carried == null || !carried.stacksWith(itemInstance))
                continue;

            if (carried.absorb(itemInstance) > 0)
                revision++;

            if (itemInstance.isSpent())
                return;
        }

        if (hasBackpack())
            getBackpackContainer().mergeStack(itemInstance);
    }

    public boolean isHoldingTwoHanded() {

        ItemInstance mainHand = slot2Item[EquipmentSlot.MAIN_HAND.ordinal()];

        return mainHand != null && mainHand.getItemDefinitionHandle().isTwoHanded();
    }

    // Visibility \\

    public boolean isHidden(EquipmentSlot equipmentSlot) {
        return slot2Hidden[equipmentSlot.ordinal()];
    }

    public void setHidden(EquipmentSlot equipmentSlot, boolean hidden) {

        if (!equipmentSlot.isHideable())
            return;

        slot2Hidden[equipmentSlot.ordinal()] = hidden;
        revision++;
    }

    public boolean isShown(EquipmentSlot equipmentSlot) {
        return hasItem(equipmentSlot) && !isHidden(equipmentSlot);
    }

    // Wield \\

    public boolean isAboveWieldLevel(ItemInstance itemInstance) {
        return itemInstance.getItemDefinitionHandle().isGear() && itemInstance.getLevel() > wieldLevel;
    }

    public int getWieldLevel() {
        return wieldLevel;
    }

    public void setWieldLevel(int wieldLevel) {
        this.wieldLevel = wieldLevel;
    }

    // Totals \\

    public float getCarriedWeight() {

        float weight = 0f;

        for (int i = 0; i < slot2Item.length; i++)
            if (slot2Item[i] != null)
                weight += slot2Item[i].getTotalWeight();

        return weight;
    }

    // Revision \\

    public void markChanged() {
        revision++;
    }

    // Accessible \\

    public int getRevision() {
        return revision;
    }

    public ItemInstance getItem(EquipmentSlot equipmentSlot) {
        return slot2Item[equipmentSlot.ordinal()];
    }

    public boolean hasItem(EquipmentSlot equipmentSlot) {
        return slot2Item[equipmentSlot.ordinal()] != null;
    }

    public ItemInstance getMainHand() {
        return getItem(EquipmentSlot.MAIN_HAND);
    }

    public boolean hasMainHand() {
        return hasItem(EquipmentSlot.MAIN_HAND);
    }

    public ItemInstance getOffHand() {
        return getItem(EquipmentSlot.OFF_HAND);
    }

    public boolean hasOffHand() {
        return hasItem(EquipmentSlot.OFF_HAND);
    }

    public boolean hasBackpack() {
        return hasItem(EquipmentSlot.BACKPACK);
    }

    public ContainerInstance getBackpackContainer() {
        return hasBackpack() ? getItem(EquipmentSlot.BACKPACK).getContainerInstance() : null;
    }
}
