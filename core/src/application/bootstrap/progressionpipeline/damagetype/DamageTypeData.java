package application.bootstrap.progressionpipeline.damagetype;

import engine.root.DataPackage;

public class DamageTypeData extends DataPackage {

    /*
     * Immutable damage type definition loaded from ARPG. Armor turns aside
     * part of a blow only when its type meets armor, a raised guard catches
     * part only when its type can be guarded, and a blow whose type bleeds
     * draws the veins over a wounded player's sight. Owned by DamageTypeHandle
     * for the engine lifetime.
     */

    // Identity
    private final String damageTypeName;
    private final short damageTypeID;

    // Presentation
    private final String displayName;

    // Behavior
    private final boolean armored;
    private final boolean guardable;
    private final boolean bleeding;

    // Constructor \\

    public DamageTypeData(
            String damageTypeName,
            short damageTypeID,
            String displayName,
            boolean armored,
            boolean guardable,
            boolean bleeding) {

        // Identity
        this.damageTypeName = damageTypeName;
        this.damageTypeID = damageTypeID;

        // Presentation
        this.displayName = displayName;

        // Behavior
        this.armored = armored;
        this.guardable = guardable;
        this.bleeding = bleeding;
    }

    // Accessible \\

    public String getDamageTypeName() {
        return damageTypeName;
    }

    public short getDamageTypeID() {
        return damageTypeID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isArmored() {
        return armored;
    }

    public boolean isGuardable() {
        return guardable;
    }

    public boolean isBleeding() {
        return bleeding;
    }
}
