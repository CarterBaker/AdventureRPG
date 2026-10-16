package application.bootstrap.combatpipeline.movesetmanager;

import java.io.File;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.combatpipeline.moveset.AttackStruct;
import application.bootstrap.combatpipeline.moveset.MovesetData;
import application.bootstrap.combatpipeline.moveset.MovesetHandle;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.progressionpipeline.damagetypemanager.DamageTypeManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class MovesetBuilder extends BuilderPackage {

    /*
     * Parses one moveset ARPG file into a MovesetData and wraps it in a
     * MovesetHandle, under the short moveset ID MovesetManager assigns its
     * name. "attacks" and the guard's "clips" each name every CombatDirection
     * by its lower-case name and nothing else. An attack names its "clip", the
     * seconds it takes to "windup" and to "recovery", and optionally the
     * fraction of its clip its blow lands on ("strike"), its "damage" and
     * "reach" against the entity's plain strike, the "arc" in degrees it
     * sweeps, and the "damage_type" its blow lands as, blunt unless it names
     * one. The guard names how many seconds it takes to "raise" and to "shift"
     * between sides, and the share of a blow it will "catch". Every clip is
     * resolved here, plays once, and targets the same rig as the rest.
     * Bootstrap-only.
     */

    // Internal
    private MovesetManager movesetManager;
    private AnimationManager animationManager;
    private DamageTypeManager damageTypeManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.movesetManager = get(MovesetManager.class);
        this.animationManager = get(AnimationManager.class);
        this.damageTypeManager = get(DamageTypeManager.class);
    }

    // Build \\

    MovesetHandle build(File file, String movesetName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        short movesetID = movesetManager.registerMovesetName(movesetName);
        ArpgObjectStruct attacksArpg = ArpgUtility.validateObject(arpg, "attacks");
        ArpgObjectStruct guardArpg = ArpgUtility.validateObject(arpg, "guard");
        ArpgObjectStruct guardClipsArpg = ArpgUtility.validateObject(guardArpg, "clips");

        validateDirections(attacksArpg, "attacks", movesetName, file);
        validateDirections(guardClipsArpg, "guard clips", movesetName, file);

        AttackStruct[] attacks = new AttackStruct[CombatDirection.VALUES.length];
        AnimationClipHandle[] guardClips = new AnimationClipHandle[CombatDirection.VALUES.length];

        for (CombatDirection direction : CombatDirection.VALUES) {

            String directionName = ArpgUtility.toEnumName(direction);

            attacks[direction.ordinal()] = parseAttack(
                    ArpgUtility.validateObject(attacksArpg, directionName),
                    directionName,
                    movesetName,
                    file);
            guardClips[direction.ordinal()] = resolveClip(
                    ArpgUtility.validateString(guardClipsArpg, directionName),
                    movesetName,
                    file);
        }

        validateRig(attacks, guardClips, movesetName, file);

        float guardRaise = ArpgUtility.getFloat(guardArpg, "raise", EngineSetting.DEFAULT_GUARD_RAISE_SECONDS);
        float guardShift = ArpgUtility.getFloat(guardArpg, "shift", EngineSetting.DEFAULT_GUARD_SHIFT_SECONDS);
        float guardCatch = ArpgUtility.getFloat(guardArpg, "catch", EngineSetting.DEFAULT_GUARD_CATCH);

        if (guardRaise <= 0f || guardShift <= 0f)
            throwException("Moveset \"" + movesetName + "\" guard needs a positive \"raise\" and \"shift\". File: "
                    + file.getName());

        if (guardCatch < 0f || guardCatch > EngineSetting.BLOCK_MITIGATION_MAX)
            throwException("Moveset \"" + movesetName + "\" guard must \"catch\" between 0 and "
                    + EngineSetting.BLOCK_MITIGATION_MAX + ". File: " + file.getName());

        MovesetData movesetData = new MovesetData(
                movesetName,
                movesetID,
                attacks,
                guardClips,
                guardRaise,
                guardShift,
                guardCatch);

        MovesetHandle handle = create(MovesetHandle.class);
        handle.constructor(movesetData);

        return handle;
    }

    // Attack Parsing \\

    private AttackStruct parseAttack(
            ArpgObjectStruct attackArpg,
            String directionName,
            String movesetName,
            File file) {

        String owner = "Moveset \"" + movesetName + "\" attack \"" + directionName + "\"";
        AnimationClipHandle clip = resolveClip(ArpgUtility.validateString(attackArpg, "clip"), movesetName, file);
        float strike = ArpgUtility.getFloat(attackArpg, "strike", EngineSetting.DEFAULT_ATTACK_STRIKE);
        float windup = ArpgUtility.validateFloat(attackArpg, "windup");
        float recovery = ArpgUtility.validateFloat(attackArpg, "recovery");
        float damage = ArpgUtility.getFloat(attackArpg, "damage", EngineSetting.DEFAULT_ATTACK_DAMAGE);
        float reach = ArpgUtility.getFloat(attackArpg, "reach", EngineSetting.DEFAULT_ATTACK_REACH);
        float arc = ArpgUtility.getFloat(attackArpg, "arc", EngineSetting.DEFAULT_ATTACK_ARC);
        short damageTypeID = damageTypeManager.getDamageTypeIDFromDamageTypeName(
                ArpgUtility.getString(attackArpg, "damage_type", EngineSetting.DAMAGE_TYPE_BLUNT));

        if (strike <= 0f || strike >= 1f)
            throwException(owner + " must land its \"strike\" between 0 and 1. File: " + file.getName());

        if (windup <= 0f || recovery <= 0f)
            throwException(owner + " needs a positive \"windup\" and \"recovery\". File: " + file.getName());

        if (damage < 0f || reach <= 0f)
            throwException(owner + " needs a \"damage\" of at least 0 and a positive \"reach\". File: "
                    + file.getName());

        if (arc < 0f || arc > EngineSetting.ATTACK_ARC_MAX)
            throwException(owner + " sweeps an \"arc\" outside 0 to " + EngineSetting.ATTACK_ARC_MAX
                    + " degrees. File: " + file.getName());

        return new AttackStruct(clip, strike, windup, recovery, damage, reach, arc, damageTypeID);
    }

    // Validation \\

    // Names every direction and nothing else
    private void validateDirections(ArpgObjectStruct directionsArpg, String owner, String movesetName, File file) {

        for (String directionName : directionsArpg.keySet())
            ArpgUtility.toEnum(directionName, CombatDirection.class);

        for (CombatDirection direction : CombatDirection.VALUES)
            if (!directionsArpg.has(ArpgUtility.toEnumName(direction)))
                throwException("Moveset \"" + movesetName + "\" " + owner + " has nothing for \""
                        + ArpgUtility.toEnumName(direction) + "\". File: " + file.getName());
    }

    private void validateRig(
            AttackStruct[] attacks,
            AnimationClipHandle[] guardClips,
            String movesetName,
            File file) {

        RigHandle rigHandle = attacks[0].getClip().getRigHandle();

        for (CombatDirection direction : CombatDirection.VALUES)
            if (attacks[direction.ordinal()].getClip().getRigHandle() != rigHandle
                    || guardClips[direction.ordinal()].getRigHandle() != rigHandle)
                throwException("Moveset \"" + movesetName + "\" mixes clips made for different rigs. File: "
                        + file.getName());
    }

    // Utility \\

    private AnimationClipHandle resolveClip(String clipName, String movesetName, File file) {

        AnimationClipHandle clip = animationManager.getClipHandleFromClipName(clipName);

        if (clip.isLooping())
            throwException("Moveset \"" + movesetName + "\" clip \"" + clipName
                    + "\" loops — a move plays once. File: " + file.getName());

        return clip;
    }
}
