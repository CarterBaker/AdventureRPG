package application.bootstrap.itempipeline.itemdefinition;

public enum EquipmentType {

    /*
     * Where on a body an item can be worn. Paired body parts share one type —
     * a greave fits either leg — and the entity's equipment slots decide which
     * types each slot accepts. NONE marks an item that is only ever carried.
     */

    NONE,
    HEAD,
    CLOAK,
    CHEST,
    SHIRT,
    BELT,
    PANTS,
    SHOULDER,
    ARM,
    GLOVE,
    LEG,
    FOOT,
    RING,
    BACKPACK,
    WEAPON,
    SHIELD
}
