package application.bootstrap.entitypipeline.inventory;

import application.bootstrap.itempipeline.itemdefinition.EquipmentType;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;

public enum EquipmentSlot {

    /*
     * Every place on a body an item can be worn or held. Each slot takes the
     * one equipment type it names; the two hands carry anything at all, since
     * whatever a character picks up without a backpack is held in its hands.
     * Every slot but a ring can be hidden, so worn gear can be left off the
     * character's look without taking it off. Save files name slots by their
     * lower-case constant names.
     */

    BACKPACK(EquipmentType.BACKPACK, false, true),
    HEAD(EquipmentType.HEAD, false, true),
    CLOAK(EquipmentType.CLOAK, false, true),
    CHEST(EquipmentType.CHEST, false, true),
    SHIRT(EquipmentType.SHIRT, false, true),
    BELT(EquipmentType.BELT, false, true),
    PANTS(EquipmentType.PANTS, false, true),
    RIGHT_SHOULDER(EquipmentType.SHOULDER, false, true),
    LEFT_SHOULDER(EquipmentType.SHOULDER, false, true),
    RIGHT_ARM(EquipmentType.ARM, false, true),
    LEFT_ARM(EquipmentType.ARM, false, true),
    RIGHT_GLOVE(EquipmentType.GLOVE, false, true),
    LEFT_GLOVE(EquipmentType.GLOVE, false, true),
    RIGHT_LEG(EquipmentType.LEG, false, true),
    LEFT_LEG(EquipmentType.LEG, false, true),
    RIGHT_FOOT(EquipmentType.FOOT, false, true),
    LEFT_FOOT(EquipmentType.FOOT, false, true),
    MAIN_HAND(EquipmentType.WEAPON, true, true),
    OFF_HAND(EquipmentType.SHIELD, true, true),
    RING_1(EquipmentType.RING, false, false),
    RING_2(EquipmentType.RING, false, false),
    RING_3(EquipmentType.RING, false, false),
    RING_4(EquipmentType.RING, false, false),
    RING_5(EquipmentType.RING, false, false),
    RING_6(EquipmentType.RING, false, false),
    RING_7(EquipmentType.RING, false, false),
    RING_8(EquipmentType.RING, false, false),
    RING_9(EquipmentType.RING, false, false),
    RING_10(EquipmentType.RING, false, false);

    // Internal
    private final EquipmentType equipmentType;
    private final boolean hand;
    private final boolean hideable;

    // Constructor \\

    EquipmentSlot(EquipmentType equipmentType, boolean hand, boolean hideable) {
        this.equipmentType = equipmentType;
        this.hand = hand;
        this.hideable = hideable;
    }

    // Accessible \\

    public boolean accepts(ItemDefinitionHandle itemDefinitionHandle) {
        return hand || itemDefinitionHandle.getEquipmentType() == equipmentType;
    }

    public EquipmentType getEquipmentType() {
        return equipmentType;
    }

    public boolean isHand() {
        return hand;
    }

    public boolean isHideable() {
        return hideable;
    }
}
