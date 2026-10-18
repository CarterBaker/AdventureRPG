package application.bootstrap.combatpipeline.weaponmanager;

import java.io.File;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.combatpipeline.weapon.AttackStruct;
import application.bootstrap.combatpipeline.weapon.WeaponData;
import application.bootstrap.combatpipeline.weapon.WeaponHandle;
import application.bootstrap.combatpipeline.weapon.WeaponRigStruct;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.progressionpipeline.damagetypemanager.DamageTypeManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class WeaponBuilder extends BuilderPackage {

    /*
     * Parses one weapon ARPG file into a WeaponData and wraps it in a
     * WeaponHandle, under the short weapon ID WeaponManager assigns its name.
     * "attacks" names at least one CombatDirection by its lower-case name,
     * each with the seconds it takes to "windup" and to "recovery", and
     * optionally its "damage" and "reach" against the entity's plain strike,
     * the "arc" in degrees it sweeps, and the "damage_type" its blow lands
     * as, blunt unless it names one. The "guard" names the "directions" it
     * can be held toward, how many seconds it takes to "raise" and to "shift"
     * between sides, and the share of a blow it will "catch". Every side the
     * weapon lacks resolves here through CombatDirection's fallbacks to the
     * first it has. "mass" is what a body's own weapon drives into a blow, a
     * held item driving its own weight instead. "rigs" names every rig that can wield the weapon, each
     * with the "attacks" clip and optional "strike" fraction for every side
     * the weapon attacks from and the "guard" clip for every side it guards,
     * nothing more and nothing less; every clip plays once and is made for
     * the rig it is named under. Bootstrap-only.
     */

    // Internal
    private WeaponManager weaponManager;
    private AnimationManager animationManager;
    private RigManager rigManager;
    private DamageTypeManager damageTypeManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.weaponManager = get(WeaponManager.class);
        this.animationManager = get(AnimationManager.class);
        this.rigManager = get(RigManager.class);
        this.damageTypeManager = get(DamageTypeManager.class);
    }

    // Build \\

    WeaponHandle build(File file, String weaponName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        short weaponID = weaponManager.registerWeaponName(weaponName);
        ArpgObjectStruct attacksArpg = ArpgUtility.validateObject(arpg, "attacks");
        ArpgObjectStruct guardArpg = ArpgUtility.validateObject(arpg, "guard");
        ArpgObjectStruct rigsArpg = ArpgUtility.validateObject(arpg, "rigs");

        AttackStruct[] attacks = parseAttacks(attacksArpg, weaponName, file);
        boolean[] attacked = new boolean[CombatDirection.VALUES.length];
        boolean[] guarded = parseGuardSides(ArpgUtility.validateArray(guardArpg, "directions"), weaponName, file);

        for (CombatDirection direction : CombatDirection.VALUES)
            attacked[direction.ordinal()] = attacks[direction.ordinal()] != null;

        CombatDirection[] attackSides = resolveSides(attacked, "attacks", weaponName, file);
        CombatDirection[] guardSides = resolveSides(guarded, "guard", weaponName, file);

        float guardRaise = ArpgUtility.getFloat(guardArpg, "raise", EngineSetting.DEFAULT_GUARD_RAISE_SECONDS);
        float guardShift = ArpgUtility.getFloat(guardArpg, "shift", EngineSetting.DEFAULT_GUARD_SHIFT_SECONDS);
        float guardCatch = ArpgUtility.getFloat(guardArpg, "catch", EngineSetting.DEFAULT_GUARD_CATCH);

        if (guardRaise <= 0f || guardShift <= 0f)
            throwException("Weapon \"" + weaponName + "\" guard needs a positive \"raise\" and \"shift\". File: "
                    + file.getName());

        if (guardCatch < 0f || guardCatch > EngineSetting.BLOCK_MITIGATION_MAX)
            throwException("Weapon \"" + weaponName + "\" guard must \"catch\" between 0 and "
                    + EngineSetting.BLOCK_MITIGATION_MAX + ". File: " + file.getName());

        float mass = ArpgUtility.getFloat(arpg, "mass", EngineSetting.DEFAULT_WEAPON_MASS);

        if (mass <= 0f)
            throwException("Weapon \"" + weaponName + "\" needs a positive \"mass\". File: " + file.getName());

        WeaponRigStruct[] rigs = parseRigs(rigsArpg, attacked, guarded, weaponName, file);

        WeaponData weaponData = new WeaponData(
                weaponName,
                weaponID,
                attacks,
                attackSides,
                guardSides,
                guardRaise,
                guardShift,
                guardCatch,
                mass,
                rigs);

        WeaponHandle handle = create(WeaponHandle.class);
        handle.constructor(weaponData);

        return handle;
    }

    // Attack Parsing \\

    // The attack from every side the weapon names, null for every side it lacks
    private AttackStruct[] parseAttacks(ArpgObjectStruct attacksArpg, String weaponName, File file) {

        AttackStruct[] attacks = new AttackStruct[CombatDirection.VALUES.length];

        if (attacksArpg.size() == 0)
            throwException("Weapon \"" + weaponName + "\" has no attacks. File: " + file.getName());

        for (String directionName : attacksArpg.keySet()) {

            CombatDirection direction = ArpgUtility.toEnum(directionName, CombatDirection.class);

            attacks[direction.ordinal()] = parseAttack(
                    ArpgUtility.validateObject(attacksArpg, directionName),
                    directionName,
                    weaponName,
                    file);
        }

        return attacks;
    }

    private AttackStruct parseAttack(
            ArpgObjectStruct attackArpg,
            String directionName,
            String weaponName,
            File file) {

        String owner = "Weapon \"" + weaponName + "\" attack \"" + directionName + "\"";
        float windup = ArpgUtility.validateFloat(attackArpg, "windup");
        float recovery = ArpgUtility.validateFloat(attackArpg, "recovery");
        float damage = ArpgUtility.getFloat(attackArpg, "damage", EngineSetting.DEFAULT_ATTACK_DAMAGE);
        float reach = ArpgUtility.getFloat(attackArpg, "reach", EngineSetting.DEFAULT_ATTACK_REACH);
        float arc = ArpgUtility.getFloat(attackArpg, "arc", EngineSetting.DEFAULT_ATTACK_ARC);
        short damageTypeID = damageTypeManager.getDamageTypeIDFromDamageTypeName(
                ArpgUtility.getString(attackArpg, "damage_type", EngineSetting.DAMAGE_TYPE_BLUNT));

        if (windup <= 0f || recovery <= 0f)
            throwException(owner + " needs a positive \"windup\" and \"recovery\". File: " + file.getName());

        if (damage < 0f || reach <= 0f)
            throwException(owner + " needs a \"damage\" of at least 0 and a positive \"reach\". File: "
                    + file.getName());

        if (arc < 0f || arc > EngineSetting.ATTACK_ARC_MAX)
            throwException(owner + " sweeps an \"arc\" outside 0 to " + EngineSetting.ATTACK_ARC_MAX
                    + " degrees. File: " + file.getName());

        return new AttackStruct(windup, recovery, damage, reach, arc, damageTypeID);
    }

    // Guard Parsing \\

    // Which sides the guard can be held toward, each named once
    private boolean[] parseGuardSides(ArpgArrayStruct directionsArpg, String weaponName, File file) {

        boolean[] guarded = new boolean[CombatDirection.VALUES.length];

        if (directionsArpg.size() == 0)
            throwException("Weapon \"" + weaponName + "\" guard has no \"directions\". File: " + file.getName());

        for (int i = 0; i < directionsArpg.size(); i++) {

            CombatDirection direction = ArpgUtility.toEnum(directionsArpg.get(i).getAsString(), CombatDirection.class);

            if (guarded[direction.ordinal()])
                throwException("Weapon \"" + weaponName + "\" guard names \"" + ArpgUtility.toEnumName(direction)
                        + "\" twice. File: " + file.getName());

            guarded[direction.ordinal()] = true;
        }

        return guarded;
    }

    // Rig Parsing \\

    private WeaponRigStruct[] parseRigs(
            ArpgObjectStruct rigsArpg,
            boolean[] attacked,
            boolean[] guarded,
            String weaponName,
            File file) {

        if (rigsArpg.size() == 0)
            throwException("Weapon \"" + weaponName + "\" names no rig that can wield it. File: " + file.getName());

        WeaponRigStruct[] rigs = new WeaponRigStruct[rigsArpg.size()];
        int index = 0;

        for (String rigName : rigsArpg.keySet())
            rigs[index++] = parseRig(
                    ArpgUtility.validateObject(rigsArpg, rigName),
                    rigManager.getRigHandleFromRigName(rigName),
                    attacked,
                    guarded,
                    weaponName + "\" rig \"" + rigName,
                    file);

        return rigs;
    }

    private WeaponRigStruct parseRig(
            ArpgObjectStruct rigArpg,
            RigHandle rigHandle,
            boolean[] attacked,
            boolean[] guarded,
            String owner,
            File file) {

        ArpgObjectStruct attacksArpg = ArpgUtility.validateObject(rigArpg, "attacks");
        ArpgObjectStruct guardArpg = ArpgUtility.validateObject(rigArpg, "guard");

        validateSides(attacksArpg, attacked, owner + "\" attacks", file);
        validateSides(guardArpg, guarded, owner + "\" guard", file);

        AnimationClipHandle[] attackClips = new AnimationClipHandle[CombatDirection.VALUES.length];
        float[] attackStrikes = new float[CombatDirection.VALUES.length];
        AnimationClipHandle[] guardClips = new AnimationClipHandle[CombatDirection.VALUES.length];

        for (CombatDirection direction : CombatDirection.VALUES) {

            String directionName = ArpgUtility.toEnumName(direction);

            if (attacked[direction.ordinal()]) {

                ArpgObjectStruct attackArpg = ArpgUtility.validateObject(attacksArpg, directionName);
                float strike = ArpgUtility.getFloat(attackArpg, "strike", EngineSetting.DEFAULT_ATTACK_STRIKE);

                if (strike <= 0f || strike >= 1f)
                    throwException("Weapon \"" + owner + "\" attack \"" + directionName
                            + "\" must land its \"strike\" between 0 and 1. File: " + file.getName());

                attackClips[direction.ordinal()] = resolveClip(
                        ArpgUtility.validateString(attackArpg, "clip"), rigHandle, owner, file);
                attackStrikes[direction.ordinal()] = strike;
            }

            if (guarded[direction.ordinal()])
                guardClips[direction.ordinal()] = resolveClip(
                        ArpgUtility.validateString(guardArpg, directionName), rigHandle, owner, file);
        }

        return new WeaponRigStruct(rigHandle, attackClips, attackStrikes, guardClips);
    }

    // Validation \\

    // Names every side the weapon has and nothing else
    private void validateSides(ArpgObjectStruct sidesArpg, boolean[] sides, String owner, File file) {

        for (String directionName : sidesArpg.keySet())
            if (!sides[ArpgUtility.toEnum(directionName, CombatDirection.class).ordinal()])
                throwException("Weapon \"" + owner + " animates \"" + directionName
                        + "\", a side the weapon does not have. File: " + file.getName());

        for (CombatDirection direction : CombatDirection.VALUES)
            if (sides[direction.ordinal()] && !sidesArpg.has(ArpgUtility.toEnumName(direction)))
                throwException("Weapon \"" + owner + " has nothing for \"" + ArpgUtility.toEnumName(direction)
                        + "\". File: " + file.getName());
    }

    // Every side resolved to itself when the weapon has it, else to the first of its fallbacks the weapon has
    private CombatDirection[] resolveSides(boolean[] sides, String owner, String weaponName, File file) {

        CombatDirection[] resolved = new CombatDirection[CombatDirection.VALUES.length];

        for (CombatDirection direction : CombatDirection.VALUES) {

            CombatDirection side = direction;

            for (int i = 0; i < CombatDirection.VALUES.length && !sides[side.ordinal()]; i++)
                side = side.fallback();

            if (!sides[side.ordinal()])
                throwException("Weapon \"" + weaponName + "\" " + owner + " has no side \""
                        + ArpgUtility.toEnumName(direction) + "\" falls back to. File: " + file.getName());

            resolved[direction.ordinal()] = side;
        }

        return resolved;
    }

    // Utility \\

    private AnimationClipHandle resolveClip(String clipName, RigHandle rigHandle, String owner, File file) {

        AnimationClipHandle clip = animationManager.getClipHandleFromClipName(clipName);

        if (clip.isLooping())
            throwException("Weapon \"" + owner + "\" clip \"" + clipName
                    + "\" loops — a move plays once. File: " + file.getName());

        if (clip.getRigHandle() != rigHandle)
            throwException("Weapon \"" + owner + "\" clip \"" + clipName
                    + "\" is made for a different rig. File: " + file.getName());

        return clip;
    }
}
