package application.bootstrap.progressionpipeline.progressionmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.CareerHandle;
import application.bootstrap.entitypipeline.progression.ProficiencyHandle;
import application.bootstrap.entitypipeline.progression.ProgressionHandle;
import application.bootstrap.entitypipeline.statistics.StatisticsHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.skillmanager.SkillManager;
import application.bootstrap.progressionpipeline.stat.StatHandle;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.trademanager.TradeManager;
import application.bootstrap.progressionpipeline.util.ProficiencyUtility;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class StatBranch extends BranchPackage {

    /*
     * Resolves every stat of a being for ProgressionManager, the one place a
     * total is built. A stat starts from its base plus everything the being's
     * wielded gear lends it, read at the being's level; each attribute point
     * above the starting value then adds its fraction of that, and every bonus
     * — each whole step of every trade, the active profession, and each whole
     * step of the effective skill of whatever either hand wields, bare hands
     * counting as the unarmed skill — adds its fraction on top. The total is
     * held between the stat's min and max. resolve() keeps the totals on the
     * being and rebuilds them only once its inventory, level or attributes,
     * trade or skill steps, or profession have moved since; preview() builds
     * them as they would stand with the staged attribute points confirmed, for
     * the character page to show before they are spent.
     */

    // Internal
    private StatManager statManager;
    private TradeManager tradeManager;
    private SkillManager skillManager;
    private ProfessionManager professionManager;
    private GearBranch gearBranch;
    private ProficiencyBranch proficiencyBranch;

    // Scratch
    private float[] gearScratch;
    private float[] bonusScratch;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.statManager = get(StatManager.class);
        this.tradeManager = get(TradeManager.class);
        this.skillManager = get(SkillManager.class);
        this.professionManager = get(ProfessionManager.class);
        this.gearBranch = get(GearBranch.class);
        this.proficiencyBranch = get(ProficiencyBranch.class);
    }

    @Override
    protected void awake() {

        // Scratch
        this.gearScratch = new float[statManager.getStatIDCount()];
        this.bonusScratch = new float[statManager.getStatIDCount()];
    }

    // Resolve \\

    float[] resolve(EntityInstance entity) {

        StatisticsHandle statisticsHandle = entity.getStatisticsHandle();
        int inventoryRevision = entity.getInventoryHandle().getRevision();
        int progressionRevision = entity.getProgressionHandle().getRevision();
        int proficiencyRevision = entity.getProficiencyHandle().getRevision();
        int careerRevision = entity.getCareerHandle().getRevision();

        if (statisticsHandle.isResolved(inventoryRevision, progressionRevision, proficiencyRevision, careerRevision))
            return statisticsHandle.getStats();

        statisticsHandle.markResolved(inventoryRevision, progressionRevision, proficiencyRevision, careerRevision);
        build(entity, statisticsHandle.getStats(), false);

        return statisticsHandle.getStats();
    }

    void preview(EntityInstance entity, float[] totals) {
        build(entity, totals, true);
    }

    // Build \\

    private void build(EntityInstance entity, float[] totals, boolean staged) {

        for (int statID = 0; statID < gearScratch.length; statID++) {
            gearScratch[statID] = 0f;
            bonusScratch[statID] = 0f;
        }

        gearBranch.accumulate(entity, gearScratch);
        accumulateTrades(entity.getProficiencyHandle());
        accumulateProfession(entity.getCareerHandle());
        accumulateHeldSkills(entity);

        ProgressionHandle progressionHandle = entity.getProgressionHandle();

        for (short statID = EngineSetting.REGISTRY_RESERVED_ID + 1; statID < gearScratch.length; statID++) {

            StatHandle statHandle = statManager.getStatHandleFromStatID(statID);
            float total = (statHandle.getBase() + gearScratch[statID])
                    * resolveAttributeFactor(statHandle, progressionHandle, staged)
                    * (1f + bonusScratch[statID]);

            totals[statID] = Math.max(statHandle.getMin(), Math.min(statHandle.getMax(), total));
        }
    }

    private float resolveAttributeFactor(StatHandle statHandle, ProgressionHandle progressionHandle, boolean staged) {

        ValueMapStruct attributeFeeds = statHandle.getAttributeFeeds();
        float factor = 1f;

        for (int i = 0; i < attributeFeeds.size(); i++) {

            short attributeID = attributeFeeds.getID(i);
            int value = progressionHandle.getAttribute(attributeID)
                    + (staged ? progressionHandle.getStaged(attributeID) : 0);

            factor += attributeFeeds.getValue(i) * (value - EngineSetting.ATTRIBUTE_START);
        }

        return factor;
    }

    private void accumulateTrades(ProficiencyHandle proficiencyHandle) {

        for (short tradeID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                tradeID < proficiencyHandle.getTradeCount();
                tradeID++) {

            int step = ProficiencyUtility.step(proficiencyHandle.getTrade(tradeID).getValue());

            if (step != 0)
                accumulateBonuses(tradeManager.getTradeHandleFromTradeID(tradeID).getStatBonuses(), step);
        }
    }

    private void accumulateProfession(CareerHandle careerHandle) {

        if (!careerHandle.hasActiveProfession())
            return;

        accumulateBonuses(
                professionManager.getProfessionHandleFromProfessionID(careerHandle.getActiveProfessionID())
                        .getStatBonuses(),
                1f);
    }

    // The skills of what both hands wield, bare hands counting as the unarmed skill and a skill held twice counting
    // once
    private void accumulateHeldSkills(EntityInstance entity) {

        ItemInstance offHand = entity.getInventoryHandle().getOffHand();
        short mainSkillID = proficiencyBranch.resolveHeldSkillID(entity);

        accumulateSkill(entity, entity.getInventoryHandle().getMainHand(), mainSkillID);

        if (offHand != null && offHand.getItemDefinitionHandle().getSkillID() != mainSkillID)
            accumulateSkill(entity, offHand, offHand.getItemDefinitionHandle().getSkillID());
    }

    private void accumulateSkill(EntityInstance entity, ItemInstance held, short skillID) {

        if (skillID == EngineSetting.SKILL_NONE || held != null && !gearBranch.canWield(entity, held))
            return;

        int step = ProficiencyUtility.step(proficiencyBranch.resolveEffectiveSkill(entity, skillID));

        if (step != 0)
            accumulateBonuses(skillManager.getSkillHandleFromSkillID(skillID).getStatBonuses(), step);
    }

    private void accumulateBonuses(ValueMapStruct statBonuses, float scale) {
        for (int i = 0; i < statBonuses.size(); i++)
            bonusScratch[statBonuses.getID(i)] += statBonuses.getValue(i) * scale;
    }
}
