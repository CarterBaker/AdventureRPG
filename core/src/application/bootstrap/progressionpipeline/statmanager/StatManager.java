package application.bootstrap.progressionpipeline.statmanager;

import application.bootstrap.progressionpipeline.stat.StatHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StatManager extends ManagerPackage {

    /*
     * Owns the stat palette: every number a being is measured by, from health
     * and damage to learning and luck. IDs are assigned in registration order,
     * which is also the order stats are listed in, and a name declared twice
     * is rejected. Every stat is resolved in awake(), before any item
     * definition or being needs one, so a being's totals can be held in one
     * array indexed by ID. The stats the engine itself reads are resolved by
     * name once and cached.
     */

    // Palette
    private Object2IntOpenHashMap<String> statName2StatID;
    private ObjectArrayList<StatHandle> statID2StatHandle;

    // Engine Stats — the stats the engine itself reads, resolved once by name
    private short healthStatID;
    private short damageStatID;
    private short armorStatID;
    private short healthRegenStatID;
    private short attackSpeedStatID;
    private short moveSpeedStatID;
    private short carryCapacityStatID;
    private short critChanceStatID;
    private short luckStatID;
    private short learningStatID;
    private short retentionStatID;
    private short poiseStatID;
    private short persuasionStatID;
    private short throwPowerStatID;
    private short workPowerStatID;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.statName2StatID = RegistryUtility.createNameIndex();
        this.statID2StatHandle = RegistryUtility.createPalette();
        create(StatLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        // Engine Stats
        this.healthStatID = getStatIDFromStatName(EngineSetting.STAT_HEALTH);
        this.damageStatID = getStatIDFromStatName(EngineSetting.STAT_DAMAGE);
        this.armorStatID = getStatIDFromStatName(EngineSetting.STAT_ARMOR);
        this.healthRegenStatID = getStatIDFromStatName(EngineSetting.STAT_HEALTH_REGEN);
        this.attackSpeedStatID = getStatIDFromStatName(EngineSetting.STAT_ATTACK_SPEED);
        this.moveSpeedStatID = getStatIDFromStatName(EngineSetting.STAT_MOVE_SPEED);
        this.carryCapacityStatID = getStatIDFromStatName(EngineSetting.STAT_CARRY_CAPACITY);
        this.critChanceStatID = getStatIDFromStatName(EngineSetting.STAT_CRIT_CHANCE);
        this.luckStatID = getStatIDFromStatName(EngineSetting.STAT_LUCK);
        this.learningStatID = getStatIDFromStatName(EngineSetting.STAT_LEARNING);
        this.retentionStatID = getStatIDFromStatName(EngineSetting.STAT_RETENTION);
        this.poiseStatID = getStatIDFromStatName(EngineSetting.STAT_POISE);
        this.persuasionStatID = getStatIDFromStatName(EngineSetting.STAT_PERSUASION);
        this.throwPowerStatID = getStatIDFromStatName(EngineSetting.STAT_THROW_POWER);
        this.workPowerStatID = getStatIDFromStatName(EngineSetting.STAT_WORK_POWER);
    }

    // Management \\

    short registerStatName(String statName) {
        return (short) RegistryUtility.registerID(
                statName2StatID, statID2StatHandle, statName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addStatHandle(StatHandle statHandle) {

        short statID = statHandle.getStatID();

        if (statID2StatHandle.get(statID) != null)
            throwException("Duplicate stat name: '" + statHandle.getStatName()
                    + "' is declared more than once — every stat name must be unique");

        statID2StatHandle.set(statID, statHandle);
    }

    // Accessible \\

    public boolean hasStat(String statName) {
        return RegistryUtility.getHandle(statName2StatID, statID2StatHandle, statName) != null;
    }

    public short getStatIDFromStatName(String statName) {

        if (!hasStat(statName) && internalLoader != null)
            ((StatLoader) internalLoader).request(statName);

        if (!hasStat(statName))
            throwException("Stat not found after load: \"" + statName + "\"");

        return (short) statName2StatID.getInt(statName);
    }

    public StatHandle getStatHandleFromStatID(short statID) {

        StatHandle handle = RegistryUtility.getHandle(statID2StatHandle, statID);

        if (handle == null)
            throwException("Stat ID not found: " + statID);

        return handle;
    }

    public StatHandle getStatHandleFromStatName(String statName) {
        return getStatHandleFromStatID(getStatIDFromStatName(statName));
    }

    // Every stat ID lies below this — the length of an array indexed by stat ID
    public int getStatIDCount() {
        return statID2StatHandle.size();
    }

    // Engine Stats \\

    public short getHealthStatID() {
        return healthStatID;
    }

    public short getDamageStatID() {
        return damageStatID;
    }

    public short getArmorStatID() {
        return armorStatID;
    }

    public short getHealthRegenStatID() {
        return healthRegenStatID;
    }

    public short getAttackSpeedStatID() {
        return attackSpeedStatID;
    }

    public short getMoveSpeedStatID() {
        return moveSpeedStatID;
    }

    public short getCarryCapacityStatID() {
        return carryCapacityStatID;
    }

    public short getCritChanceStatID() {
        return critChanceStatID;
    }

    public short getLuckStatID() {
        return luckStatID;
    }

    public short getLearningStatID() {
        return learningStatID;
    }

    public short getRetentionStatID() {
        return retentionStatID;
    }

    public short getPoiseStatID() {
        return poiseStatID;
    }

    public short getPersuasionStatID() {
        return persuasionStatID;
    }

    public short getThrowPowerStatID() {
        return throwPowerStatID;
    }

    public short getWorkPowerStatID() {
        return workPowerStatID;
    }
}
