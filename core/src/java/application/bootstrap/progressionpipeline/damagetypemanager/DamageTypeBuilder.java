package application.bootstrap.progressionpipeline.damagetypemanager;

import java.io.File;

import application.bootstrap.progressionpipeline.damagetype.DamageTypeData;
import application.bootstrap.progressionpipeline.damagetype.DamageTypeHandle;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class DamageTypeBuilder extends BuilderPackage {

    /*
     * Parses damage type ARPG files and builds DamageTypeHandle instances.
     * Each file lists its types under a "damage_types" array, each named by
     * its "name" field: whether "armor" meets it, whether a "guard" can catch
     * it, and whether it "bleeds". Bootstrap-only.
     */

    // Internal
    private DamageTypeManager damageTypeManager;

    // Base \\

    @Override
    protected void get() {
        this.damageTypeManager = get(DamageTypeManager.class);
    }

    // Build \\

    ObjectArrayList<DamageTypeHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct damageTypeArray = ArpgUtility.validateArray(rootArpg, "damage_types");
        ObjectArrayList<DamageTypeHandle> damageTypes = new ObjectArrayList<>();

        for (int i = 0; i < damageTypeArray.size(); i++)
            damageTypes.add(parseDamageType(damageTypeArray.get(i).getAsObject()));

        return damageTypes;
    }

    // Parse \\

    private DamageTypeHandle parseDamageType(ArpgObjectStruct damageTypeArpg) {

        String damageTypeName = ArpgUtility.validateString(damageTypeArpg, "name");
        short damageTypeID = damageTypeManager.registerDamageTypeName(damageTypeName);

        DamageTypeData damageTypeData = new DamageTypeData(
                damageTypeName,
                damageTypeID,
                ArpgUtility.getString(damageTypeArpg, "display_name", damageTypeName),
                ArpgUtility.getBoolean(damageTypeArpg, "armor", true),
                ArpgUtility.getBoolean(damageTypeArpg, "guard", true),
                ArpgUtility.getBoolean(damageTypeArpg, "bleeds", false));

        DamageTypeHandle damageTypeHandle = create(DamageTypeHandle.class);
        damageTypeHandle.constructor(damageTypeData);

        return damageTypeHandle;
    }
}
