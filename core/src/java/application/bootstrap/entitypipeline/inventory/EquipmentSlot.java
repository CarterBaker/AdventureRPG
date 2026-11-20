package application.bootstrap.entitypipeline.inventory;

import application.bootstrap.itempipeline.itemdefinition.EquipmentSide;
import application.bootstrap.itempipeline.itemdefinition.EquipmentType;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;

public enum EquipmentSlot {

    /*
     * Every place on a body an item can be worn or held. Each slot takes the
     * one equipment type it names; the two hands carry anything at all, since
     * whatever a character picks up without a backpack is held in its hands.
     * Every slot but a ring can be hidden, so worn gear can be left off the
     * character's look without taking it off. Each slot names the side of the
     * body it sits on, so a paired piece is drawn as the model fitted to that
     * side. Save files name slots by their lower-case constant names.
     */

    BACKPACK(EquipmentType.BACKPACK, false, true, EquipmentSide.NONE),
    HEAD(EquipmentType.HEAD, false, true, EquipmentSide.NONE),
    CLOAK(EquipmentType.CLOAK, false, true, EquipmentSide.NONE),
    CHEST(EquipmentType.CHEST, false, true, EquipmentSide.NONE),
    SHIRT(EquipmentType.SHIRT, false, true, EquipmentSide.NONE),
    BELT(EquipmentType.BELT, false, true, EquipmentSide.NONE),
    PANTS(EquipmentType.PANTS, false, true, EquipmentSide.NONE),
    RIGHT_SHOULDER(EquipmentType.SHOULDER, false, true, EquipmentSide.RIGHT),
    LEFT_SHOULDER(EquipmentType.SHOULDER, false, true, EquipmentSide.LEFT),
    RIGHT_ARM(EquipmentType.ARM, false, true, EquipmentSide.RIGHT),
    LEFT_ARM(EquipmentType.ARM, false, true, EquipmentSide.LEFT),
    RIGHT_GLOVE(EquipmentType.GLOVE, false, true, EquipmentSide.RIGHT),
    LEFT_GLOVE(EquipmentType.GLOVE, false, true, EquipmentSide.LEFT),
    RIGHT_LEG(EquipmentType.LEG, false, true, EquipmentSide.RIGHT),
    LEFT_LEG(EquipmentType.LEG, false, true, EquipmentSide.LEFT),
    RIGHT_FOOT(EquipmentType.FOOT, false, true, EquipmentSide.RIGHT),
    LEFT_FOOT(EquipmentType.FOOT, false, true, EquipmentSide.LEFT),
    MAIN_HAND(EquipmentType.WEAPON, true, true, EquipmentSide.NONE),
    OFF_HAND(EquipmentType.SHIELD, true, true, EquipmentSide.NONE),
    RING_1(EquipmentType.RING, false, false, EquipmentSide.NONE),
    RING_2(EquipmentType.RING, false, false, EquipmentSide.NONE),
    RING_3(EquipmentType.RING, false, false, EquipmentSide.NONE),
    RING_4(EquipmentType.RING, false, false, EquipmentSide.NONE),
    RING_5(EquipmentType.RING, false, false, EquipmentSide.NONE),
    RING_6(EquipmentType.RING, false, false, EquipmentSide.NONE),
    RING_7(EquipmentType.RING, false, false, EquipmentSide.NONE),
    RING_8(EquipmentType.RING, false, false, EquipmentSide.NONE),
    RING_9(EquipmentType.RING, false, false, EquipmentSide.NONE),
    RING_10(EquipmentType.RING, false, false, EquipmentSide.NONE);

    // Values
    public static final EquipmentSlot[] VALUES = values();

    // Internal
    private final EquipmentType equipmentType;
    private final boolean hand;
    private final boolean hideable;
    private final EquipmentSide equipmentSide;

    // Constructor \\

    EquipmentSlot(EquipmentType equipmentType, boolean hand, boolean hideable, EquipmentSide equipmentSide) {
        this.equipmentType = equipmentType;
        this.hand = hand;
        this.hideable = hideable;
        this.equipmentSide = equipmentSide;
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

    public EquipmentSide getEquipmentSide() {
        return equipmentSide;
    }
}
