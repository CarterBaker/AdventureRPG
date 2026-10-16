package application.bootstrap.progressionpipeline.damagetype;

import engine.root.HandlePackage;

public class DamageTypeHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded damage type definition. Registered and
     * owned by DamageTypeManager. Delegates all accessors through
     * DamageTypeData.
     */

    // Internal
    private DamageTypeData damageTypeData;

    // Constructor \\

    public void constructor(DamageTypeData damageTypeData) {

        // Internal
        this.damageTypeData = damageTypeData;
    }

    // Accessible \\

    public DamageTypeData getDamageTypeData() {
        return damageTypeData;
    }

    public String getDamageTypeName() {
        return damageTypeData.getDamageTypeName();
    }

    public short getDamageTypeID() {
        return damageTypeData.getDamageTypeID();
    }

    public String getDisplayName() {
        return damageTypeData.getDisplayName();
    }

    public boolean isArmored() {
        return damageTypeData.isArmored();
    }

    public boolean isGuardable() {
        return damageTypeData.isGuardable();
    }

    public boolean isBleeding() {
        return damageTypeData.isBleeding();
    }
}
