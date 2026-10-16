package application.runtime.menueventsmanager.menus.inventory;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.EquipmentType;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.progressionpipeline.attribute.AttributeHandle;
import application.bootstrap.progressionpipeline.attributemanager.AttributeManager;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.progressionpipeline.skillmanager.SkillManager;
import application.bootstrap.progressionpipeline.stat.StatHandle;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.tier.TierHandle;
import application.bootstrap.progressionpipeline.tiermanager.TierManager;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.menueventsmanager.util.ProgressionDisplayBranch;
import engine.input.InputNameUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.settings.KeyBindings;
import engine.util.mathematics.vectors.Vector3Int;

public class InventoryEquipmentBranch extends BranchPackage {

    /*
     * Fills and keeps the equipment board: a column of slots down each side of
     * the preview window set into it, the backpack slot under the first, the
     * ten ring slots in two rows of five under the preview, and the statistics
     * box beside them. The character faces out of its preview, so the slots
     * for its right side stand in the screen's left column. Every slot but a
     * ring carries an eye that shows or hides its item on the character. Slots
     * are redrawn only when the inventory or the backpack's contents change,
     * and the statistics — level, experience and profession, every attribute,
     * every stat the character page shows, and the load carried — only when
     * the character or its progress through its level does; the details under
     * the statistics follow whatever item the cursor points at, its name
     * tinted by its tier, with its level, condition, skill and every stat it
     * lends at its own level. A container opened where it lies shows no board.
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

    // Internal
    private MenuManager menuManager;
    private InventoryBranch inventoryBranch;
    private InventoryDragBranch inventoryDragBranch;
    private ToolTypeManager toolTypeManager;
    private ProgressionManager progressionManager;
    private AttributeManager attributeManager;
    private StatManager statManager;
    private SkillManager skillManager;
    private TierManager tierManager;
    private ProgressionDisplayBranch progressionDisplayBranch;

    // Colors
    private MenuColorStruct eyeShownColor;
    private MenuColorStruct eyeHiddenColor;
    private MenuColorStruct lockedColor;

    // Layout
    private DimensionVector2Struct fillSize;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.eyeShownColor = new MenuColorStruct(RuntimeSetting.INVENTORY_EYE_SHOWN_COLOR);
        this.eyeHiddenColor = new MenuColorStruct(RuntimeSetting.INVENTORY_EYE_HIDDEN_COLOR);
        this.lockedColor = new MenuColorStruct(RuntimeSetting.INVENTORY_LOCKED_COLOR);

        // Layout
        this.fillSize = new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(EngineSetting.PERCENT_MAX),
                DimensionValueStruct.ofPercent(EngineSetting.PERCENT_MAX));
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inventoryBranch = get(InventoryBranch.class);
        this.inventoryDragBranch = get(InventoryDragBranch.class);
        this.toolTypeManager = get(ToolTypeManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.attributeManager = get(AttributeManager.class);
        this.statManager = get(StatManager.class);
        this.skillManager = get(SkillManager.class);
        this.tierManager = get(TierManager.class);
        this.progressionDisplayBranch = get(ProgressionDisplayBranch.class);
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

        int ringCount = 0;

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES)
            if (equipmentSlot.getEquipmentType() == EquipmentType.RING)
                injectSlot(session, ringCount++ < RuntimeSetting.INVENTORY_RING_ROW_LENGTH
                        ? RuntimeSetting.ENTRY_INVENTORY_RING_ROW_A
                        : RuntimeSetting.ENTRY_INVENTORY_RING_ROW_B,
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

        if (!session.hasEquipment())
            return;

        InventoryHandle inventory = session.getInventory();
        ContainerInstance backpack = inventory.getBackpackContainer();
        int contentRevision = backpack != null ? backpack.getRevision() : EngineSetting.INDEX_NOT_FOUND;

        if (inventory.getRevision() != session.getShownRevision()
                || contentRevision != session.getShownContentRevision()) {

            refreshSlots(session);
            session.setShownRevision(inventory.getRevision());
            session.setShownContentRevision(contentRevision);
            session.invalidateDetails();
        }

        EntityInstance player = session.getPlayer();
        int statRevision = progressionDisplayBranch.getDisplayRevision(player);
        double progress = player.getProgressionHandle().getProgress();

        if (statRevision != session.getShownStatRevision() || progress != session.getShownProgress()) {
            refreshStats(session);
            session.setShownStatRevision(statRevision);
            session.setShownProgress(progress);
        }

        if (!session.isDetailsShown() || session.getHoveredItem() != session.getDetailedItem())
            showDetails(session, session.getHoveredItem());
    }

    // Slots \\

    private void refreshSlots(InventorySessionStruct session) {

        InventoryHandle inventory = session.getInventory();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {

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

        if (session == null || !session.hasEquipment() || session.getDragMode() != InventoryDragMode.NONE)
            return;

        inventoryDragBranch.pickUpFromSlot(session, EquipmentSlot.valueOf(equipmentSlotName));
    }

    public void toggleSlot(String equipmentSlotName, WindowInstance window) {

        InventorySessionStruct session = inventoryBranch.getSession(window);

        if (session == null || !session.hasEquipment())
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

        injectStatHeader(menu, RuntimeSetting.INVENTORY_STAT_SECTION_CHARACTER);
        injectStatRow(menu, RuntimeSetting.INVENTORY_STAT_LEVEL, String.valueOf(player.getLevel()));
        injectStatRow(menu, RuntimeSetting.INVENTORY_STAT_EXPERIENCE,
                progressionDisplayBranch.formatExperience(player));
        injectStatRow(menu, RuntimeSetting.INVENTORY_STAT_PROFESSION,
                progressionDisplayBranch.formatProfession(player));

        if (player.getProgressionHandle().getUnspentPoints() > 0)
            injectStatRow(menu, RuntimeSetting.INVENTORY_STAT_UNSPENT,
                    String.valueOf(player.getProgressionHandle().getUnspentPoints()));

        injectStatHeader(menu, RuntimeSetting.INVENTORY_STAT_SECTION_ATTRIBUTES);

        for (short attributeID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                attributeID < attributeManager.getAttributeIDCount();
                attributeID++) {

            AttributeHandle attributeHandle = attributeManager.getAttributeHandleFromAttributeID(attributeID);

            injectStatRow(menu, attributeHandle.getDisplayName(),
                    String.valueOf(player.getProgressionHandle().getAttribute(attributeID)));
        }

        injectStatHeader(menu, RuntimeSetting.INVENTORY_STAT_SECTION_COMBAT);

        for (short statID = EngineSetting.REGISTRY_RESERVED_ID + 1; statID < statManager.getStatIDCount(); statID++) {

            StatHandle statHandle = statManager.getStatHandleFromStatID(statID);

            if (statHandle.isShown())
                injectStatRow(menu, statHandle.getDisplayName(), progressionDisplayBranch.formatStat(player, statID));
        }

        injectStatHeader(menu, RuntimeSetting.INVENTORY_STAT_SECTION_LOAD);
        injectStatRow(menu, RuntimeSetting.INVENTORY_STAT_CARRIED, String.format(
                RuntimeSetting.INVENTORY_FORMAT_LOAD,
                inventory.getCarriedWeight(),
                player.getStat(statManager.getCarryCapacityStatID())));
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

    private int countWorn(InventoryHandle inventory) {

        int count = 0;

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES)
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

        menuManager.inject(
                menu, RuntimeSetting.ENTRY_INVENTORY_DETAILS, RuntimeSetting.MENU_INVENTORY_DETAIL_TITLE,
                title -> {
                    MenuColorStruct tierColor = progressionDisplayBranch.getTierColor(itemInstance.getTierID());
                    title.setFontText(item.getDisplayName());

                    if (tierColor != null)
                        title.setColorOverride(tierColor);
                });
        injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, toKindText(item));

        if (item.isGear())
            injectGearDetails(session, itemInstance);
        injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE,
                String.format(RuntimeSetting.INVENTORY_FORMAT_ITEM_WEIGHT, itemInstance.getTotalWeight()));
        injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, String.format(
                RuntimeSetting.INVENTORY_FORMAT_ITEM_SIZE, shape.getSizeX(), shape.getSizeY(), shape.getSizeZ()));

        if (item.isContainer()) {

            Vector3Int space = item.getContainerSpace().getSize();

            injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, String.format(
                    RuntimeSetting.INVENTORY_FORMAT_ITEM_SPACE, space.x, space.y, space.z));
        }

        if (item.isStackable())
            injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, String.format(
                    RuntimeSetting.INVENTORY_FORMAT_ITEM_STACK, itemInstance.getStackCount(), item.getStackSize()));

        if (item.isTool())
            injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, String.format(
                    RuntimeSetting.INVENTORY_FORMAT_ITEM_TOOL,
                    toolTypeManager.getToolTypeHandleFromToolTypeID(item.getToolTypeID()).getLocalName(),
                    item.getToolTier()));

        if (item.isTwoHanded())
            injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, RuntimeSetting.INVENTORY_TEXT_TWO_HANDED);

        ValueMapStruct stats = item.getStats();

        for (int i = 0; i < stats.size(); i++)
            injectItemStat(menu, itemInstance, stats.getID(i), RuntimeSetting.INVENTORY_FORMAT_ITEM_STAT);

        for (int i = 0; i < itemInstance.getBonusCount(); i++)
            injectItemStat(
                    menu, itemInstance, itemInstance.getBonusStatID(i), RuntimeSetting.INVENTORY_FORMAT_ITEM_BONUS);

        if (!item.getDescription().isEmpty())
            injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, item.getDescription());
    }

    // The tier, level and condition of a piece of gear, its level red while the player cannot yet wield it, and the
    // skill it trains
    private void injectGearDetails(InventorySessionStruct session, ItemInstance itemInstance) {

        MenuInstance menu = session.getEquipmentMenu();
        ItemDefinitionHandle item = itemInstance.getItemDefinitionHandle();
        boolean wieldable = !session.getInventory().isAboveWieldLevel(itemInstance);

        if (itemInstance.getTierID() != EngineSetting.TIER_NONE) {

            TierHandle tierHandle = tierManager.getTierHandleFromTierID(itemInstance.getTierID());

            injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, String.format(
                    RuntimeSetting.INVENTORY_FORMAT_ITEM_TIER, tierHandle.getDisplayName()));
        }

        menuManager.inject(
                menu, RuntimeSetting.ENTRY_INVENTORY_DETAILS, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE,
                line -> {
                    line.setFontText(wieldable
                            ? String.format(RuntimeSetting.INVENTORY_FORMAT_ITEM_LEVEL, itemInstance.getLevel())
                            : String.format(
                                    RuntimeSetting.INVENTORY_FORMAT_ITEM_LEVEL_LOCKED,
                                    itemInstance.getLevel(),
                                    progressionManager.getRequiredLevel(itemInstance.getLevel())));

                    if (!wieldable)
                        line.setColorOverride(lockedColor);
                });
        injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, String.format(
                RuntimeSetting.INVENTORY_FORMAT_ITEM_CONDITION,
                itemInstance.getCondition() * EngineSetting.PERCENT_MAX,
                progressionDisplayBranch.resolveConditionWord(itemInstance.getCondition())));

        if (item.hasSkill())
            injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, String.format(
                    RuntimeSetting.INVENTORY_FORMAT_ITEM_SKILL,
                    skillManager.getSkillHandleFromSkillID(item.getSkillID()).getDisplayName()));
    }

    private void injectItemStat(MenuInstance menu, ItemInstance itemInstance, short statID, String format) {
        injectDetail(menu, RuntimeSetting.MENU_INVENTORY_DETAIL_LINE, String.format(
                format,
                statManager.getStatHandleFromStatID(statID).getDisplayName(),
                progressionDisplayBranch.formatItemStat(itemInstance, statID)));
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
