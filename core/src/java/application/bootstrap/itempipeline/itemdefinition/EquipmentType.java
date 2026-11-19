package application.bootstrap.itempipeline.itemdefinition;

public enum EquipmentType {

    /*
     * Where on a body an item can be worn. Paired body parts share one type —
     * a greave fits either leg — and the entity's equipment slots decide which
     * types each slot accepts. NONE marks an item that is only ever carried.
     * A type worn on the body may be drawn as a worn model skinned to the
     * wearer's rig; a weapon or shield is always held, keeping its own model.
     */

    NONE(false),
    HEAD(true),
    CLOAK(true),
    CHEST(true),
    SHIRT(true),
    BELT(true),
    PANTS(true),
    SHOULDER(true),
    ARM(true),
    GLOVE(true),
    LEG(true),
    FOOT(true),
    RING(true),
    BACKPACK(true),
    WEAPON(false),
    SHIELD(false);

    // Internal
    private final boolean wornOnBody;

    // Constructor \\

    EquipmentType(boolean wornOnBody) {
        this.wornOnBody = wornOnBody;
    }

    // Accessible \\

    public boolean isWornOnBody() {
        return wornOnBody;
    }
}
