package application.bootstrap.progressionpipeline.progressionmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.CareerHandle;
import application.bootstrap.entitypipeline.progression.ProficiencyHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.progressionpipeline.deedmanager.DeedManager;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.skill.SkillHandle;
import application.bootstrap.progressionpipeline.skillmanager.SkillManager;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.trade.TradeHandle;
import application.bootstrap.progressionpipeline.trademanager.TradeManager;
import application.bootstrap.progressionpipeline.util.ProficiencyStruct;
import application.bootstrap.progressionpipeline.util.ProficiencyUtility;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class ProficiencyBranch extends BranchPackage {

    /*
     * Trades and skills for ProgressionManager. practiceSkill() trains the one
     * skill an item names and feeds every trade that skill belongs to by its
     * share, each of those a practice of the trade; learnTrade() grows a trade
     * on its own, by practice or by a deed. A trade learns at its own rate,
     * times the being's learning stat and whatever its active profession
     * multiplies that trade by. Every practice first settles the neglect owed
     * since last time, so a value is always current when it grows. Each whole
     * step gained is a deed of advancement, and practicing a trade is a deed
     * of its own, so using a craft earns experience a little at a time and
     * advancing it earns more. A skill's effective value is its own plus a
     * share of the trades it feeds, so a veteran of a trade is never a
     * beginner at a new skill of it. settle() catches every trade and skill of
     * a being up with the clock, slowed by its retention stat, and only when a
     * whole step moves do its stats or profession requirements need another
     * look.
     */

    // Internal
    private ProgressionManager progressionManager;
    private TradeManager tradeManager;
    private SkillManager skillManager;
    private StatManager statManager;
    private DeedManager deedManager;
    private ProfessionManager professionManager;
    private ConductBranch conductBranch;
    private CareerBranch careerBranch;
    private ProgressionSoundBranch progressionSoundBranch;

    // Skills
    private short unarmedSkillID;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.progressionManager = get(ProgressionManager.class);
        this.tradeManager = get(TradeManager.class);
        this.skillManager = get(SkillManager.class);
        this.statManager = get(StatManager.class);
        this.deedManager = get(DeedManager.class);
        this.professionManager = get(ProfessionManager.class);
        this.conductBranch = get(ConductBranch.class);
        this.careerBranch = get(CareerBranch.class);
        this.progressionSoundBranch = get(ProgressionSoundBranch.class);
    }

    @Override
    protected void awake() {

        // Skills
        this.unarmedSkillID = skillManager.getSkillIDFromSkillName(EngineSetting.SKILL_UNARMED);
    }

    // Practice \\

    void practiceSkill(EntityInstance entity, short skillID, float amount) {

        if (amount <= 0f || skillID == EngineSetting.SKILL_NONE)
            return;

        SkillHandle skillHandle = skillManager.getSkillHandleFromSkillID(skillID);
        ProficiencyStruct skill = entity.getProficiencyHandle().getSkill(skillID);
        double time = progressionManager.getTime();

        settle(entity, skill, skillHandle.isDecaying(), time);

        int before = ProficiencyUtility.step(skill.getValue());

        ProficiencyUtility.learn(
                skill,
                amount,
                skillHandle.getLearnRate() * entity.getStat(statManager.getLearningStatID()),
                skillHandle.getMin(),
                skillHandle.getMax(),
                time);

        advance(entity, ProficiencyUtility.step(skill.getValue()) - before, deedManager.getSkillAdvanceDeedID());

        ValueMapStruct tradeShares = skillHandle.getTradeShares();

        for (int i = 0; i < tradeShares.size(); i++)
            learnTrade(entity, tradeShares.getID(i), amount * tradeShares.getValue(i), true);
    }

    // Grows a trade by an amount, or sets it back by a negative one — practiced when the being used the craft itself
    void learnTrade(EntityInstance entity, short tradeID, float amount, boolean practiced) {

        if (amount == 0f)
            return;

        TradeHandle tradeHandle = tradeManager.getTradeHandleFromTradeID(tradeID);
        ProficiencyStruct trade = entity.getProficiencyHandle().getTrade(tradeID);
        double time = progressionManager.getTime();

        settle(entity, trade, tradeHandle.isDecaying(), time);

        int before = ProficiencyUtility.step(trade.getValue());

        if (amount > 0f)
            ProficiencyUtility.learn(
                    trade,
                    amount,
                    tradeHandle.getLearnRate()
                            * entity.getStat(statManager.getLearningStatID())
                            * resolveProfessionLearning(entity, tradeID),
                    tradeHandle.getMin(),
                    tradeHandle.getMax(),
                    time);
        else
            ProficiencyUtility.shift(trade, amount, tradeHandle.getMin(), tradeHandle.getMax());

        if (practiced && amount > 0f)
            conductBranch.perform(entity, deedManager.getTradePracticeDeedID(), amount, entity.getLevel());

        advance(entity, ProficiencyUtility.step(trade.getValue()) - before, deedManager.getTradeAdvanceDeedID());
    }

    private float resolveProfessionLearning(EntityInstance entity, short tradeID) {

        CareerHandle careerHandle = entity.getCareerHandle();

        if (!careerHandle.hasActiveProfession())
            return 1f;

        return professionManager.getProfessionHandleFromProfessionID(careerHandle.getActiveProfessionID())
                .getTradeLearning()
                .find(tradeID, 1f);
    }

    // Marks the stats and requirements a step change moves, and makes each step gained a deed of advancement
    private void advance(EntityInstance entity, int steps, short advanceDeedID) {

        if (steps == 0)
            return;

        markChanged(entity);

        if (steps <= 0)
            return;

        conductBranch.perform(entity, advanceDeedID, steps, entity.getLevel());
        progressionSoundBranch.skill(entity);
    }

    // The skill whatever the main hand holds trains — the unarmed skill for bare hands, SKILL_NONE for an item that
    // trains none
    short resolveHeldSkillID(EntityInstance entity) {

        ItemInstance held = entity.getInventoryHandle().getMainHand();

        return held != null ? held.getItemDefinitionHandle().getSkillID() : unarmedSkillID;
    }

    // Seed \\

    // Starts a trade at least at a value, or at most at it when the value is below zero
    void seedTrade(EntityInstance entity, short tradeID, float value) {

        TradeHandle tradeHandle = tradeManager.getTradeHandleFromTradeID(tradeID);
        ProficiencyStruct trade = entity.getProficiencyHandle().getTrade(tradeID);
        float seeded = Math.max(tradeHandle.getMin(), Math.min(tradeHandle.getMax(), value));

        if (seeded >= 0f ? trade.getValue() >= seeded : trade.getValue() <= seeded)
            return;

        double time = progressionManager.getTime();

        trade.setValue(seeded);
        trade.setPeak(Math.max(trade.getPeak(), seeded));
        trade.setLastUsedTime(time);
        trade.setSettledTime(time);
        markChanged(entity);
    }

    // Effective \\

    // A skill's own whole steps plus its share of the steps of the trades it feeds, weighed by how much it feeds
    // each — whole steps only, so the bonuses it lends move only when a step does
    float resolveEffectiveSkill(EntityInstance entity, short skillID) {

        SkillHandle skillHandle = skillManager.getSkillHandleFromSkillID(skillID);
        ProficiencyHandle proficiencyHandle = entity.getProficiencyHandle();
        ValueMapStruct tradeShares = skillHandle.getTradeShares();
        float totalShare = tradeShares.getTotal();
        float carried = 0f;

        if (totalShare > 0f)
            for (int i = 0; i < tradeShares.size(); i++)
                carried += ProficiencyUtility.step(proficiencyHandle.getTrade(tradeShares.getID(i)).getValue())
                        * tradeShares.getValue(i) / totalShare;

        float effective = ProficiencyUtility.step(proficiencyHandle.getSkill(skillID).getValue())
                + carried * EngineSetting.SKILL_TRADE_CARRY;

        return Math.max(skillHandle.getMin(), Math.min(skillHandle.getMax(), effective));
    }

    // Decay \\

    void settle(EntityInstance entity, double time) {

        ProficiencyHandle proficiencyHandle = entity.getProficiencyHandle();
        boolean stepped = false;

        for (short tradeID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                tradeID < proficiencyHandle.getTradeCount();
                tradeID++)
            stepped |= settle(
                    entity,
                    proficiencyHandle.getTrade(tradeID),
                    tradeManager.getTradeHandleFromTradeID(tradeID).isDecaying(),
                    time);

        for (short skillID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                skillID < proficiencyHandle.getSkillCount();
                skillID++)
            stepped |= settle(
                    entity,
                    proficiencyHandle.getSkill(skillID),
                    skillManager.getSkillHandleFromSkillID(skillID).isDecaying(),
                    time);

        if (stepped)
            markChanged(entity);
    }

    // Settles one value up to a time — true when a whole step moved
    private boolean settle(EntityInstance entity, ProficiencyStruct proficiency, boolean decaying, double time) {

        if (!decaying) {
            proficiency.setSettledTime(Math.max(proficiency.getSettledTime(), time));
            return false;
        }

        int before = ProficiencyUtility.step(proficiency.getValue());

        ProficiencyUtility.decay(proficiency, time, entity.getStat(statManager.getRetentionStatID()));

        return ProficiencyUtility.step(proficiency.getValue()) != before;
    }

    private void markChanged(EntityInstance entity) {
        entity.getProficiencyHandle().markChanged();
        careerBranch.markPending(entity);
    }
}
