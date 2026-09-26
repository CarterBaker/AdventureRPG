package application.runtime.menueventsmanager.menus.inventory;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.EquipmentType;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemdefinition.ItemStat;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValue;
import application.bootstrap.menupipeline.util.DimensionVector2;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.input.InputNameUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.settings.KeyBindings;

public class InventoryEquipmentBranch extends BranchPackage {

    /*
     * Fills and keeps the equipment panels: a column of slots down each side
     * of the preview window with the backpack slot at the foot of the first,
     * then the ten ring slots and the statistics box. The character faces out
     * of its preview, so the slots for its right side stand in the screen's
     * left column. Every slot but a ring carries an eye that shows or hides
     * its item on the character. Slots and statistics are redrawn only when
     * the inventory or the backpack's contents change; the details under the
     * statistics follow whatever item the cursor points at.
     */

    // Slot columns, top to bottom
    private static final EquipmentSlot[] COLUMN_A = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.BELT,
            EquipmentSlot.RIGHT_SHOULDER, EquipmentSlot.RIGHT_ARM, EquipmentSlot.RIGHT_GLOVE,
            EquipmentSlot.RIGHT_LEG, EquipmentSlot.RIGHT_FOOT, EquipmentSlot.MAIN_HAND };
    private static final EquipmentSlot[] COLUMN_B = {
            EquipmentSlot.CLOAK, EquipmentSlot.SHIRT, EquipmentSlot.PANTS,
            EquipmentSlot.LEFT_SHOULDER, EquipmentSlot.LEFT_ARM, EquipmentSlot.LEFT_GLOVE,
            EquipmentSlot.LEFT_LEG, EquipmentSlot.LEFT_FOOT, EquipmentSlot.OFF_HAND };

    // Statistics, in the order the box lists them
    private static final ItemStat[] ATTRIBUTE_STATS = {
            ItemStat.STRENGTH, ItemStat.DEXTERITY, ItemStat.CONSTITUTION,
            ItemStat.INTELLIGENCE, ItemStat.WISDOM, ItemStat.CHARISMA };
    private static final ItemStat[] COMBAT_STATS = {
            ItemStat.HEALTH, ItemStat.STAMINA, ItemStat.ARMOR, ItemStat.DAMAGE };

    // Internal
    private MenuManager menuManager;
    private InventoryBranch inventoryBranch;
    private InventoryDragBranch inventoryDragBranch;

    // Colors
    private MenuColorStruct eyeShownColor;
    private MenuColorStruct eyeHiddenColor;

    // Layout
    private DimensionVector2 fillSize;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.eyeShownColor = new MenuColorStruct(RuntimeSetting.INVENTORY_EYE_SHOWN_COLOR);
        this.eyeHiddenColor = new MenuColorStruct(RuntimeSetting.INVENTORY_EYE_HIDDEN_COLOR);

        // Layout
        this.fillSize = new DimensionVector2(DimensionValue.ofPercent(100f), DimensionValue.ofPercent(100f));
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inventoryBranch = get(InventoryBranch.class);
        this.inventoryDragBranch = get(InventoryDragBranch.class);
    }

    // Populate \\

    void populate(InventorySessionStruct session) {

        session.getEquipmentMenu().getEntryPoint(RuntimeSetting.ENTRY_INVENTORY_CLOSE_HINT).setFontText(String.format(
                RuntimeSetting.INVENTORY_FORMAT_CLOSE_HINT,
                InputNameUtility.getName(KeyBindings.INVENTORY),
                InputNameUtility.getName(KeyBindings.PAUSE)));

        injectSlot(session, RuntimeSetting.ENTRY_INVENTORY_BACKPACK_SLOT, RuntimeSetting.MENU_INVENTORY_EQUIPMENT_SLOT,
                EquipmentSlot.BACKPACK).setSizeOverride(fillSize);

        for (EquipmentSlot equipmentSlot : COLUMN_A)
            injectSlot(session, RuntimeSetting.ENTRY_INVENTORY_SLOT_COLUMN_A,
                    RuntimeSetting.MENU_INVENTORY_EQUIPMENT_SLOT, equipmentSlot);

        for (EquipmentSlot equipmentSlot : COLUMN_B)
            injectSlot(session, RuntimeSetting.ENTRY_INVENTORY_SLOT_COLUMN_B,
                    RuntimeSetting.MENU_INVENTORY_EQUIPMENT_SLOT, equipmentSlot);

        for (EquipmentSlot equipmentSlot : EquipmentSlot.values())
            if (equipmentSlot.getEquipmentType() == EquipmentType.RING)
                injectSlot(session, RuntimeSetting.ENTRY_INVENTORY_RING_COLUMN,
                        RuntimeSetting.MENU_INVENTORY_RING_SLOT, equipmentSlot);
    }

    private ElementInstance injectSlot(
            InventorySessionStruct session,
            int entryPoint,
            String masterKey,
            EquipmentSlot equipmentSlot) {

        ElementInstance slotElement = menuManager.inject(
                session.getEquipmentMenu(), entryPoint, masterKey,
                slot -> {
                    slot.setOnDragArgOverride(equipmentSlot.name());

                    ElementInstance eye = slot.findChildById(RuntimeSetting.ELEMENT_INVENTORY_SLOT_EYE);

                    if (eye != null)
                        eye.setActionArgOverride(equipmentSlot.name());
                });

        session.setSlotElement(equipmentSlot, slotElement);

        return slotElement;
    }

    // Update \\

    void update(InventorySessionStruct session) {

        InventoryHandle inventory = session.getInventory();
        ContainerInstance backpack = inventory.getBackpackContainer();
        int contentRevision = backpack != null ? backpack.getRevision() : EngineSetting.INDEX_NOT_FOUND;

        if (inventory.getRevision() != session.getShownRevision()
                || contentRevision != session.getShownContentRevision()) {

            refreshSlots(session);
            refreshStats(session);
            session.setShownRevision(inventory.getRevision());
            session.setShownContentRevision(contentRevision);
            session.invalidateDetails();
        }

        if (!session.isDetailsShown() || session.getHoveredItem() != session.getDetailedItem())
            showDetails(session, session.getHoveredItem());
    }

    // Slots \\

    private void refreshSlots(InventorySessionStruct session) {

        InventoryHandle inventory = session.getInventory();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {

            ElementInstance slotElement = session.getSlotElement(equipmentSlot);

            slotElement.findChildById(RuntimeSetting.ELEMENT_INVENTORY_SLOT_LABEL).setFontText(
                    inventory.hasItem(equipmentSlot) ? RuntimeSetting.INVENTORY_TEXT_NONE : toSlotTitle(equipmentSlot));

            if (!equipmentSlot.isHideable())
                continue;

            boolean hidden = inventory.isHidden(equipmentSlot);

            slotElement.findChildById(RuntimeSetting.ELEMENT_INVENTORY_EYE_OPEN)
                    .setColorOverride(hidden ? eyeHiddenColor : eyeShownColor);
            slotElement.findChildById(RuntimeSetting.ELEMENT_INVENTORY_EYE_CLOSED)
                    .setColorOverride(hidden ? eyeShownColor : eyeHiddenColor);
        }
    }

    public void dragSlot(String equipmentSlotName, WindowInstance window) {

        InventorySessionStruct session = inventoryBranch.getSession(window);

        if (session == null || session.getDragMode() != InventoryDragMode.NONE)
            return;

        inventoryDragBranch.pickUpFromSlot(session, EquipmentSlot.valueOf(equipmentSlotName));
    }

    public void toggleSlot(String equipmentSlotName, WindowInstance window) {

        InventorySessionStruct session = inventoryBranch.getSession(window);

        if (session == null)
            return;

        EquipmentSlot equipmentSlot = EquipmentSlot.valueOf(equipmentSlotName);
        InventoryHandle inventory = session.getInventory();

        inventory.setHidden(equipmentSlot, !inventory.isHidden(equipmentSlot));
    }

    // Statistics \\

    private void refreshStats(InventorySessionStruct session) {

        MenuInstance menu = session.getEquipmentMenu();
        EntityInstance player = session.getPlayer();
        InventoryHandle inventory = session.getInventory();

        menuManager.ejectAll(menu, RuntimeSetting.ENTRY_INVENTORY_STATS);

        injectStatHeader(menu, RuntimeSetting.INVENTORY_STAT_SECTION_ATTRIBUTES);

        for (ItemStat itemStat : ATTRIBUTE_STATS)
            injectStatRow(menu, itemStat.getTitle(), formatStat(player, itemStat));

        injectStatHeader(menu, RuntimeSetting.INVENTORY_STAT_SECTION_COMBAT);

        for (ItemStat itemStat : COMBAT_STATS)
            injectStatRow(menu, itemStat.getTitle(), formatStat(player, itemStat));

        injectStatHeader(menu, RuntimeSetting.INVENTORY_STAT_SECTION_LOAD);
        injectStatRow(menu, RuntimeSetting.INVENTORY_STAT_CARRIED, String.format(
                RuntimeSetting.INVENTORY_FORMAT_LOAD, inventory.getCarriedWeight(), player.getCarryCapacity()));
        injectStatRow(menu, RuntimeSetting.INVENTORY_STAT_ITEMS_WORN, String.valueOf(countWorn(inventory)));
    }

    private void injectStatHeader(MenuInstance menu, String title) {
        menuManager.inject(
                menu, RuntimeSetting.ENTRY_INVENTORY_STATS, RuntimeSetting.MENU_INVENTORY_STAT_HEADER,
                header -> header.setFontText(title));
    }

    private void injectStatRow(MenuInstance menu, String name, String value) {
        menuManager.inject(
                menu, RuntimeSetting.ENTRY_INVENTORY_STATS, RuntimeSetting.MENU_INVENTORY_STAT_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_INVENTORY_STAT_NAME).setFontText(name);
                    row.findChildById(RuntimeSetting.ELEMENT_INVENTORY_STAT_VALUE).setFontText(value);
                });
    }

    private String formatStat(EntityInstance player, ItemStat itemStat) {

        float bonus = player.getInventoryHandle().getStatBonus(itemStat);

        if (bonus == 0f)
            return String.format(RuntimeSetting.INVENTORY_FORMAT_STAT, player.getStat(itemStat));

        return String.format(RuntimeSetting.INVENTORY_FORMAT_STAT_BONUS, player.getStat(itemStat), bonus);
    }

    private int countWorn(InventoryHandle inventory) {

        int count = 0;

        for (EquipmentSlot equipmentSlot : EquipmentSlot.values())
            if (inventory.hasItem(equipmentSlot))
                count++;

        return count;
    }

    // Details \\

    private void showDetails(InventorySessionStruct session, ItemInstance itemInstance) {

        MenuInstance menu = session.getEquipmentMenu();

        menuManager.ejectAll(menu, RuntimeSetting.ENTRY_INVENTORY_DETAILS);
        session.setDetailedItem(itemInstance);

        if (itemInstance == null) {
            injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, RuntimeSetting.INVENTORY_TEXT_NO_SELECTION);
            return;
        }

        ItemDefinitionHandle item = itemInstance.getItemDefinitionHandle();
        ItemShapeStruct shape = item.getShape();

        injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_TITLE, item.getDisplayName());
        injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, toKindText(item));
        injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE,
                String.format(RuntimeSetting.INVENTORY_FORMAT_ITEM_WEIGHT, itemInstance.getTotalWeight()));
        injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, String.format(
                RuntimeSetting.INVENTORY_FORMAT_ITEM_SIZE, shape.getSizeX(), shape.getSizeY(), shape.getSizeZ()));

        if (item.isContainer())
            injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, String.format(
                    RuntimeSetting.INVENTORY_FORMAT_ITEM_SPACE,
                    item.getContainerSize().x, item.getContainerSize().y, item.getContainerSize().z));

        if (item.isTwoHanded())
            injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, RuntimeSetting.INVENTORY_TEXT_TWO_HANDED);

        for (ItemStat itemStat : ItemStat.values())
            if (item.getStat(itemStat) != 0f)
                injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, String.format(
                        RuntimeSetting.INVENTORY_FORMAT_ITEM_STAT, itemStat.getTitle(), item.getStat(itemStat)));

        if (!item.getDescription().isEmpty())
            injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, item.getDescription());
    }

    private void injectDetail(MenuInstance menu, String masterKey, String text) {
        menuManager.inject(
                menu, RuntimeSetting.ENTRY_INVENTORY_DETAILS, masterKey,
                line -> line.setFontText(text));
    }

    // Text \\

    private String toKindText(ItemDefinitionHandle item) {

        if (item.getEquipmentType() == EquipmentType.NONE)
            return item.getCategory().getTitle();

        return String.format(
                RuntimeSetting.INVENTORY_FORMAT_ITEM_KIND,
                item.getCategory().getTitle(),
                toTitle(item.getEquipmentType().name()));
    }

    private String toTitle(String enumName) {
        return enumName.charAt(0) + enumName.substring(1).toLowerCase();
    }

    private String toSlotTitle(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case BACKPACK -> RuntimeSetting.INVENTORY_SLOT_BACKPACK;
            case HEAD -> RuntimeSetting.INVENTORY_SLOT_HEAD;
            case CLOAK -> RuntimeSetting.INVENTORY_SLOT_CLOAK;
            case CHEST -> RuntimeSetting.INVENTORY_SLOT_CHEST;
            case SHIRT -> RuntimeSetting.INVENTORY_SLOT_SHIRT;
            case BELT -> RuntimeSetting.INVENTORY_SLOT_BELT;
            case PANTS -> RuntimeSetting.INVENTORY_SLOT_PANTS;
            case RIGHT_SHOULDER -> RuntimeSetting.INVENTORY_SLOT_RIGHT_SHOULDER;
            case LEFT_SHOULDER -> RuntimeSetting.INVENTORY_SLOT_LEFT_SHOULDER;
            case RIGHT_ARM -> RuntimeSetting.INVENTORY_SLOT_RIGHT_ARM;
            case LEFT_ARM -> RuntimeSetting.INVENTORY_SLOT_LEFT_ARM;
            case RIGHT_GLOVE -> RuntimeSetting.INVENTORY_SLOT_RIGHT_GLOVE;
            case LEFT_GLOVE -> RuntimeSetting.INVENTORY_SLOT_LEFT_GLOVE;
            case RIGHT_LEG -> RuntimeSetting.INVENTORY_SLOT_RIGHT_LEG;
            case LEFT_LEG -> RuntimeSetting.INVENTORY_SLOT_LEFT_LEG;
            case RIGHT_FOOT -> RuntimeSetting.INVENTORY_SLOT_RIGHT_FOOT;
            case LEFT_FOOT -> RuntimeSetting.INVENTORY_SLOT_LEFT_FOOT;
            case MAIN_HAND -> RuntimeSetting.INVENTORY_SLOT_MAIN_HAND;
            case OFF_HAND -> RuntimeSetting.INVENTORY_SLOT_OFF_HAND;
            default -> RuntimeSetting.INVENTORY_SLOT_RING;
        };
    }
}
