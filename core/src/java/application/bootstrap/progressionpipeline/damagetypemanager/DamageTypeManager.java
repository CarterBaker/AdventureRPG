package application.bootstrap.progressionpipeline.damagetypemanager;

import application.bootstrap.progressionpipeline.damagetype.DamageTypeHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DamageTypeManager extends ManagerPackage {

    /*
     * Owns the damage type palette: whether armor meets a blow of each kind,
     * whether a guard can catch it, and whether it bleeds. IDs are assigned in
     * registration order, and a name declared twice is rejected. Every damage
     * type is resolved in awake(), before any weapon names one.
     */

    // Palette
    private Object2IntOpenHashMap<String> damageTypeName2DamageTypeID;
    private ObjectArrayList<DamageTypeHandle> damageTypeID2DamageTypeHandle;

    // Engine Damage Types
    private short bluntDamageTypeID;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.damageTypeName2DamageTypeID = RegistryUtility.createNameIndex();
        this.damageTypeID2DamageTypeHandle = RegistryUtility.createPalette();
        create(DamageTypeLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        // Engine Damage Types
        this.bluntDamageTypeID = getDamageTypeIDFromDamageTypeName(EngineSetting.DAMAGE_TYPE_BLUNT);
    }

    // Management \\

    short registerDamageTypeName(String damageTypeName) {
        return (short) RegistryUtility.registerID(
                damageTypeName2DamageTypeID, damageTypeID2DamageTypeHandle, damageTypeName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addDamageTypeHandle(DamageTypeHandle damageTypeHandle) {

        short damageTypeID = damageTypeHandle.getDamageTypeID();

        if (damageTypeID2DamageTypeHandle.get(damageTypeID) != null)
            throwException("Duplicate damage type name: '" + damageTypeHandle.getDamageTypeName()
                    + "' is declared more than once — every damage type name must be unique");

        damageTypeID2DamageTypeHandle.set(damageTypeID, damageTypeHandle);
    }

    // Accessible \\

    public boolean hasDamageType(String damageTypeName) {
        return RegistryUtility.getHandle(
                damageTypeName2DamageTypeID, damageTypeID2DamageTypeHandle, damageTypeName) != null;
    }

    public short getDamageTypeIDFromDamageTypeName(String damageTypeName) {

        if (!hasDamageType(damageTypeName) && internalLoader != null)
            ((DamageTypeLoader) internalLoader).request(damageTypeName);

        if (!hasDamageType(damageTypeName))
            throwException("Damage type not found after load: \"" + damageTypeName + "\"");

        return (short) damageTypeName2DamageTypeID.getInt(damageTypeName);
    }

    public DamageTypeHandle getDamageTypeHandleFromDamageTypeID(short damageTypeID) {

        DamageTypeHandle handle = RegistryUtility.getHandle(damageTypeID2DamageTypeHandle, damageTypeID);

        if (handle == null)
            throwException("Damage type ID not found: " + damageTypeID);

        return handle;
    }

    public DamageTypeHandle getDamageTypeHandleFromDamageTypeName(String damageTypeName) {
        return getDamageTypeHandleFromDamageTypeID(getDamageTypeIDFromDamageTypeName(damageTypeName));
    }

    // Every damage type ID lies below this — the length of an array indexed by damage type ID
    public int getDamageTypeIDCount() {
        return damageTypeID2DamageTypeHandle.size();
    }

    // Engine Damage Types \\

    public short getBluntDamageTypeID() {
        return bluntDamageTypeID;
    }
}
