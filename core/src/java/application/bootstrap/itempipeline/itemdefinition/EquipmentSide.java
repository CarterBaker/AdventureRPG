package application.bootstrap.itempipeline.itemdefinition;

public enum EquipmentSide {

    /*
     * The side of the body an equipment slot sits on, so a piece worn on
     * either side of a pair, a pauldron or a boot, is drawn as the model
     * fitted to the side it is worn on. NONE is a slot at the body's middle,
     * and a worn model of side NONE fits a slot on either side.
     */

    NONE,
    LEFT,
    RIGHT;

    // Values
    public static final EquipmentSide[] VALUES = values();
}
