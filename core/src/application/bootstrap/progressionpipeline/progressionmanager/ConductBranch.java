package application.bootstrap.progressionpipeline.progressionmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.ConductHandle;
import application.bootstrap.progressionpipeline.deed.DeedHandle;
import application.bootstrap.progressionpipeline.deedmanager.DeedManager;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.util.ProficiencyUtility;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class ConductBranch extends BranchPackage {

    /*
     * Deeds, morality and regard for ProgressionManager. perform() is the one
     * path a deed counts: its experience is a share of one kill at the level
     * it is reckoned at — the doer's own, or for a kill the level the kill
     * counts at, nothing at all for a victim below the killer — its morality
     * moves the doer's, its trade gains practice those trades, and a counted
     * deed is tallied. A kill also weighs the victim's own morality against
     * the killer: slaying the good darkens, slaying the wicked lightens.
     * Affinity is what one being thinks of another by identity; gains are
     * multiplied by the persuasion of whoever is liked, and an opinion adds a
     * sway toward those whose morality lies near one's own.
     */

    // Internal
    private DeedManager deedManager;
    private StatManager statManager;
    private ExperienceBranch experienceBranch;
    private ProficiencyBranch proficiencyBranch;
    private CareerBranch careerBranch;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.deedManager = get(DeedManager.class);
        this.statManager = get(StatManager.class);
        this.experienceBranch = get(ExperienceBranch.class);
        this.proficiencyBranch = get(ProficiencyBranch.class);
        this.careerBranch = get(CareerBranch.class);
    }

    // Deeds \\

    void perform(EntityInstance entity, short deedID, float magnitude, int referenceLevel) {

        DeedHandle deedHandle = deedManager.getDeedHandleFromDeedID(deedID);

        if (referenceLevel > EngineSetting.LEVEL_NONE)
            experienceBranch.award(
                    entity,
                    deedHandle.getExperience() * magnitude * experienceBranch.toKillExperience(referenceLevel));

        if (deedHandle.getMorality() != 0f)
            shiftMorality(entity, deedHandle.getMorality() * magnitude);

        ValueMapStruct tradeGains = deedHandle.getTradeGains();

        for (int i = 0; i < tradeGains.size(); i++)
            proficiencyBranch.learnTrade(entity, tradeGains.getID(i), tradeGains.getValue(i) * magnitude, false);

        if (!deedHandle.isCounted())
            return;

        entity.getConductHandle().countDeed(deedID);
        careerBranch.markPending(entity);
    }

    void recordKill(EntityInstance killer, EntityInstance victim) {

        perform(
                killer,
                deedManager.getKillDeedID(),
                1f,
                experienceBranch.toKillLevel(killer.getLevel(), victim.getLevel()));

        shiftMorality(
                killer,
                -victim.getConductHandle().getMorality().getValue() * EngineSetting.KILL_MORALITY_SHARE);
    }

    // Morality \\

    void shiftMorality(EntityInstance entity, float amount) {

        if (amount == 0f)
            return;

        ProficiencyUtility.shift(
                entity.getConductHandle().getMorality(),
                amount,
                -EngineSetting.MORALITY_MAX,
                EngineSetting.MORALITY_MAX);
        careerBranch.markPending(entity);
    }

    // Where a being's morality stands, from -1 for the most wicked to 1 for the most virtuous
    float resolveMoralityShade(EntityInstance entity) {
        return entity.getConductHandle().getMorality().getValue() / EngineSetting.MORALITY_MAX;
    }

    // Affinity \\

    void shiftAffinity(EntityInstance entity, EntityInstance other, float amount) {

        ConductHandle conductHandle = entity.getConductHandle();
        long identity = other.getConductHandle().getIdentity();

        if (amount == 0f || identity == EngineSetting.IDENTITY_NONE || other == entity)
            return;

        float scaled = amount > 0f ? amount * other.getStat(statManager.getPersuasionStatID()) : amount;

        conductHandle.setAffinity(identity, clampAffinity(conductHandle.getAffinity(identity) + scaled));
    }

    // What one being thinks of another — its own regard plus a sway toward a morality like its own
    float resolveOpinion(EntityInstance entity, EntityInstance other) {

        float gap = Math.abs(entity.getConductHandle().getMorality().getValue()
                - other.getConductHandle().getMorality().getValue()) / EngineSetting.MORALITY_MAX;
        float sway = EngineSetting.AFFINITY_MORALITY_SWAY * (1f - gap);

        return clampAffinity(entity.getConductHandle().getAffinity(other.getConductHandle().getIdentity()) + sway);
    }

    private float clampAffinity(float affinity) {
        return Math.max(-EngineSetting.AFFINITY_MAX, Math.min(EngineSetting.AFFINITY_MAX, affinity));
    }
}
