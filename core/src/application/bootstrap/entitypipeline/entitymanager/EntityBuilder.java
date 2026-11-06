package application.bootstrap.entitypipeline.entitymanager;

import java.io.File;

import application.bootstrap.combatpipeline.weaponmanager.WeaponManager;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityHandle;
import application.bootstrap.entitypipeline.race.RaceHandle;
import application.bootstrap.entitypipeline.racemanager.RaceManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;

class EntityBuilder extends BuilderPackage {

    /*
     * Parses entity template ARPG into EntityData wrapped in an EntityHandle,
     * defaulting size, weight and eye level. "race" is required and names the
     * race the entity is made of, which holds the genders it may be made as
     * and the character model each requires. "weapon" names what its own body
     * fights with, bare hands unless it names another, and must be made for
     * the race's rig. Bootstrap only, apart from the unregistered preview
     * builds an editor makes of a template it is changing.
     */

    // Internal
    private RaceManager raceManager;
    private WeaponManager weaponManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.raceManager = get(RaceManager.class);
        this.weaponManager = get(WeaponManager.class);
    }

    // Build \\

    EntityHandle build(File file) {
        return build(ArpgUtility.loadObject(file), file.getName());
    }

    EntityHandle build(ArpgObjectStruct arpg, String sourceName) {

        RaceHandle raceHandle = raceManager.getRaceHandleFromRaceName(ArpgUtility.validateString(arpg, "race"));
        EntityData entityData = new EntityData(
                parseSizeMin(arpg),
                parseSizeMax(arpg),
                parseWeightMin(arpg),
                parseWeightMax(arpg),
                parseEyeLevel(arpg),
                parseBehaviorName(arpg, sourceName),
                parseWeapon(arpg, raceHandle, sourceName),
                raceHandle);

        EntityHandle entityHandle = create(EntityHandle.class);
        entityHandle.constructor(entityData);

        return entityHandle;
    }

    // Combat Parsing \\

    // The weapon the entity's own body fights with — made for its race's rig
    private short parseWeapon(ArpgObjectStruct arpg, RaceHandle raceHandle, String sourceName) {

        String weaponName = ArpgUtility.getString(arpg, "weapon", EngineSetting.DEFAULT_ENTITY_WEAPON);
        short weaponID = weaponManager.getWeaponIDFromWeaponName(weaponName);

        if (!weaponManager.getWeaponHandleFromWeaponID(weaponID).hasRig(raceHandle.getRaceData().getRigHandle()))
            throwException("Entity weapon \"" + weaponName
                    + "\" has no animation for the rig of race \"" + raceHandle.getRaceName() + "\". Source: "
                    + sourceName);

        return weaponID;
    }

    // Parse \\

    private Vector3 parseSizeMin(ArpgObjectStruct arpg) {

        if (!arpg.has("size_min"))
            return new Vector3(
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE);

        ArpgObjectStruct o = arpg.getAsObject("size_min");

        return new Vector3(
                o.has("x") ? o.get("x").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("y") ? o.get("y").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("z") ? o.get("z").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE);
    }

    private Vector3 parseSizeMax(ArpgObjectStruct arpg) {

        if (!arpg.has("size_max"))
            return new Vector3(
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE,
                    EngineSetting.DEFAULT_ENTITY_SIZE);

        ArpgObjectStruct o = arpg.getAsObject("size_max");

        return new Vector3(
                o.has("x") ? o.get("x").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("y") ? o.get("y").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE,
                o.has("z") ? o.get("z").getAsFloat() : EngineSetting.DEFAULT_ENTITY_SIZE);
    }

    private float parseWeightMin(ArpgObjectStruct arpg) {
        return arpg.has("weight_min")
                ? arpg.get("weight_min").getAsFloat()
                : EngineSetting.DEFAULT_ENTITY_WEIGHT;
    }

    private float parseWeightMax(ArpgObjectStruct arpg) {
        return arpg.has("weight_max")
                ? arpg.get("weight_max").getAsFloat()
                : EngineSetting.DEFAULT_ENTITY_WEIGHT;
    }

    private float parseEyeLevel(ArpgObjectStruct arpg) {
        return arpg.has("eye_level")
                ? arpg.get("eye_level").getAsFloat()
                : EngineSetting.DEFAULT_EYE_LEVEL;
    }

    private String parseBehaviorName(ArpgObjectStruct arpg, String sourceName) {

        if (!arpg.has("behavior"))
            throwException("Entity ARPG missing 'behavior' field: " + sourceName);

        return arpg.get("behavior").getAsString();
    }
}