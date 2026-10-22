package application.bootstrap.progressionpipeline.progressionmanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.ProgressionHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.progressionpipeline.profession.ProfessionHandle;
import application.bootstrap.progressionpipeline.profession.RequirementStruct;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ProgressionManager extends ManagerPackage {

    /*
     * Runs the progression of every being — player and NPC alike, by the same
     * code — and is the one door to it. Beings enter and leave with the world
     * through addBeing() and removeBeing(); resetBeing() starts one over at
     * the first level with its creation points to spend, and raiseBeing()
     * grows one straight to a level in a profession, its points spent by the
     * profession's weights, as an NPC is made. Each frame every being
     * regenerates, any whose profession requirements may have moved is checked
     * once, and a few beings in turn have their trades and skills settled
     * against the clock, so decay costs the same however many beings there
     * are. Stats, experience, attributes, trades and skills, deeds and regard,
     * professions, vitality and gear each run in their own branch; this
     * manager only routes to them. Time is game seconds from the world clock,
     * which keeps running while a player is away; nothing regenerates or
     * settles while the game is paused.
     */

    // Internal
    private ClockManager clockManager;
    private ProfessionManager professionManager;

    // Branches
    private StatBranch statBranch;
    private ExperienceBranch experienceBranch;
    private AttributeBranch attributeBranch;
    private ProficiencyBranch proficiencyBranch;
    private ConductBranch conductBranch;
    private CareerBranch careerBranch;
    private VitalityBranch vitalityBranch;
    private GearBranch gearBranch;

    // Beings
    private ObjectArrayList<EntityInstance> beings;
    private int settleCursor;

    // Base \\

    @Override
    protected void create() {

        // Branches
        this.statBranch = create(StatBranch.class);
        this.experienceBranch = create(ExperienceBranch.class);
        this.attributeBranch = create(AttributeBranch.class);
        this.proficiencyBranch = create(ProficiencyBranch.class);
        this.conductBranch = create(ConductBranch.class);
        this.careerBranch = create(CareerBranch.class);
        this.vitalityBranch = create(VitalityBranch.class);
        this.gearBranch = create(GearBranch.class);

        // Beings
        this.beings = new ObjectArrayList<>();
    }

    @Override
    protected void get() {

        // Internal
        this.clockManager = get(ClockManager.class);
        this.professionManager = get(ProfessionManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        if (clockManager.isPaused())
            return;

        float deltaTime = internal.getDeltaTime();

        for (int i = 0; i < beings.size(); i++) {

            EntityInstance being = beings.get(i);

            vitalityBranch.regenerate(being, deltaTime);

            if (being.getCareerHandle().isPending())
                careerBranch.evaluate(being);
        }

        settleTurn();
    }

    // A few beings each frame have their trades and skills caught up with the clock, in turn
    private void settleTurn() {

        if (beings.isEmpty())
            return;

        double time = getTime();
        int count = Math.min(beings.size(), EngineSetting.PROFICIENCY_SETTLE_BEINGS_PER_FRAME);

        for (int i = 0; i < count; i++) {

            settleCursor = (settleCursor + 1) % beings.size();
            proficiencyBranch.settle(beings.get(settleCursor), time);
        }
    }

    // Management \\

    public void addBeing(EntityInstance entity) {

        if (!beings.contains(entity))
            beings.add(entity);
    }

    public void removeBeing(EntityInstance entity) {
        beings.remove(entity);
    }

    // Starts a being over at the first level, unknown to everyone, with its creation points unspent
    public void resetBeing(EntityInstance entity, long identity) {

        double time = getTime();
        ProgressionHandle progressionHandle = entity.getProgressionHandle();

        progressionHandle.reset();
        entity.getProficiencyHandle().reset(time);
        entity.getConductHandle().reset(identity, time);
        entity.getCareerHandle().reset();
        entity.getStatisticsHandle().resetVitality();

        experienceBranch.setLevel(entity, EngineSetting.LEVEL_START);
        progressionHandle.setUnspentPoints(EngineSetting.ATTRIBUTE_CREATION_POINTS);
    }

    public void identify(EntityInstance entity, long identity) {
        entity.getConductHandle().setIdentity(identity);
    }

    // Grows a being straight to a level in a profession — every point it would have earned spent by the profession's
    // attribute weights, and its starting trades grown with its level
    public void raiseBeing(EntityInstance entity, int level, short professionID) {

        ProgressionHandle progressionHandle = entity.getProgressionHandle();
        ProfessionHandle professionHandle = professionManager.getProfessionHandleFromProfessionID(professionID);
        ValueMapStruct startingTrades = professionHandle.getStartingTrades();
        int raisedLevel = Math.max(EngineSetting.LEVEL_START, level);

        experienceBranch.setLevel(entity, raisedLevel);
        progressionHandle.setUnspentPoints(EngineSetting.ATTRIBUTE_CREATION_POINTS
                + (raisedLevel - EngineSetting.LEVEL_START) * EngineSetting.ATTRIBUTE_POINTS_PER_LEVEL);
        attributeBranch.spendByWeights(entity, professionHandle.getAttributeWeights());
        careerBranch.choose(entity, professionID);

        for (int i = 0; i < startingTrades.size(); i++)
            proficiencyBranch.seedTrade(
                    entity,
                    startingTrades.getID(i),
                    startingTrades.getValue(i) * (1f + raisedLevel * EngineSetting.NPC_TRADE_GROWTH_PER_LEVEL));

        careerBranch.evaluate(entity);
    }

    // Stats \\

    public float[] resolveStats(EntityInstance entity) {
        return statBranch.resolve(entity);
    }

    public void previewStats(EntityInstance entity, float[] totals) {
        statBranch.preview(entity, totals);
    }

    // Experience \\

    public int getWindow(int level) {
        return experienceBranch.toWindow(level);
    }

    public int getWieldLevel(int level) {
        return experienceBranch.toWieldLevel(level);
    }

    public int getRequiredLevel(int itemLevel) {
        return experienceBranch.toRequiredLevel(itemLevel);
    }

    public double getLevelExperience(int level) {
        return experienceBranch.toLevelExperience(level);
    }

    public double getExperienceIntoLevel(EntityInstance entity) {

        ProgressionHandle progressionHandle = entity.getProgressionHandle();

        return progressionHandle.getProgress() * experienceBranch.toLevelExperience(progressionHandle.getLevel());
    }

    public void awardExperience(EntityInstance entity, double experience) {
        experienceBranch.award(entity, experience);
    }

    public void recordKill(EntityInstance killer, EntityInstance victim) {
        conductBranch.recordKill(killer, victim);
    }

    // Attributes \\

    public boolean stageAttribute(EntityInstance entity, short attributeID, int points) {
        return attributeBranch.stage(entity, attributeID, points, Integer.MAX_VALUE);
    }

    // Stages a point as character creation does, no attribute rising past the creation cap
    public boolean stageCreationAttribute(EntityInstance entity, short attributeID, int points) {
        return attributeBranch.stage(entity, attributeID, points, EngineSetting.ATTRIBUTE_CREATION_MAX);
    }

    public void clearStagedAttributes(EntityInstance entity) {
        attributeBranch.clear(entity);
    }

    public boolean confirmAttributes(EntityInstance entity) {
        return attributeBranch.confirm(entity);
    }

    // Proficiency \\

    public void practiceSkill(EntityInstance entity, short skillID, float amount) {
        proficiencyBranch.practiceSkill(entity, skillID, amount);
    }

    // Practices the skill of whatever the main hand holds, or the unarmed skill for bare hands
    public void practiceHeldSkill(EntityInstance entity, float amount) {
        proficiencyBranch.practiceSkill(entity, proficiencyBranch.resolveHeldSkillID(entity), amount);
    }

    public void practiceTrade(EntityInstance entity, short tradeID, float amount) {
        proficiencyBranch.learnTrade(entity, tradeID, amount, true);
    }

    public float getEffectiveSkill(EntityInstance entity, short skillID) {
        return proficiencyBranch.resolveEffectiveSkill(entity, skillID);
    }

    // Conduct \\

    public void performDeed(EntityInstance entity, short deedID, float magnitude) {
        conductBranch.perform(entity, deedID, magnitude, entity.getLevel());
    }

    public void shiftAffinity(EntityInstance entity, EntityInstance other, float amount) {
        conductBranch.shiftAffinity(entity, other, amount);
    }

    public float getOpinion(EntityInstance entity, EntityInstance other) {
        return conductBranch.resolveOpinion(entity, other);
    }

    public float getMoralityShade(EntityInstance entity) {
        return conductBranch.resolveMoralityShade(entity);
    }

    // Career \\

    public void chooseProfession(EntityInstance entity, short professionID) {
        careerBranch.choose(entity, professionID);
    }

    public boolean switchProfession(EntityInstance entity, short professionID) {
        return careerBranch.switchTo(entity, professionID);
    }

    public void grantProfession(EntityInstance entity, short professionID) {
        careerBranch.grant(entity, professionID);
    }

    public boolean isRequirementMet(EntityInstance entity, short professionID, RequirementStruct requirement) {
        return careerBranch.isMet(entity, professionID, requirement);
    }

    public float getRequirementValue(EntityInstance entity, RequirementStruct requirement) {
        return careerBranch.resolveRequirementValue(entity, requirement);
    }

    // Vitality \\

    // Takes a share of a being's health — true when it leaves the being with none
    public boolean wound(EntityInstance entity, float share, boolean bleeding) {
        return vitalityBranch.wound(entity, share, bleeding);
    }

    public void restore(EntityInstance entity) {
        vitalityBranch.restore(entity);
    }

    // Gear \\

    public void forgeGear(ItemInstance itemInstance, int level, float luck, float bias, long seed) {
        gearBranch.forge(itemInstance, level, luck, bias, seed);
    }

    public float getItemStat(ItemInstance itemInstance, short statID, int level) {
        return gearBranch.resolveItemStat(itemInstance, statID, level);
    }

    public float getConditionFactor(ItemInstance itemInstance) {
        return gearBranch.resolveConditionFactor(itemInstance);
    }

    public boolean canWield(EntityInstance entity, ItemInstance itemInstance) {
        return gearBranch.canWield(entity, itemInstance);
    }

    public void wearGear(EntityInstance owner, ItemInstance itemInstance, float amount) {
        gearBranch.wear(owner, itemInstance, amount);
    }

    public void wearArmor(EntityInstance entity, float amount) {
        gearBranch.wearArmor(entity, amount);
    }

    // Time \\

    // Game seconds on the world clock, zero until a world's clock is running
    public double getTime() {

        ClockHandle clockHandle = clockManager.getClockHandle();

        return clockHandle.getClockData() != null ? clockHandle.getGameSecondsElapsed() : 0.0;
    }
}
