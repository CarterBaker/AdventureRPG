package application.bootstrap.entitypipeline.statistics;

import application.bootstrap.itempipeline.itemdefinition.ItemStat;
import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class StatisticsHandle extends HandlePackage {

    /*
     * Per-entity runtime statistics. Holds movement speeds, jump height,
     * reach, and the entity's own base value for every item statistic —
     * what it has before anything it wears is counted. No manager owns this —
     * it lives directly on EntityInstance and is initialized to engine
     * defaults on creation.
     */

    // Movement
    private float walkSpeed;
    private float movementSpeed;
    private float sprintSpeed;
    private float swimSpeed;

    // Physics
    private float jumpHeight;

    // Interaction
    private float reach;

    // Attributes
    private float[] baseStats;

    // Internal \\

    @Override
    protected void create() {

        // Movement
        this.walkSpeed = EngineSetting.DEFAULT_WALK_SPEED;
        this.movementSpeed = EngineSetting.DEFAULT_MOVEMENT_SPEED;
        this.sprintSpeed = EngineSetting.DEFAULT_SPRINT_SPEED;
        this.swimSpeed = EngineSetting.DEFAULT_SWIM_SPEED;

        // Physics
        this.jumpHeight = EngineSetting.DEFAULT_JUMP_HEIGHT;

        // Interaction
        this.reach = EngineSetting.DEFAULT_REACH;

        // Attributes
        this.baseStats = new float[ItemStat.values().length];
        resetBaseStats();
    }

    // Attributes \\

    public void resetBaseStats() {

        for (ItemStat itemStat : ItemStat.values())
            baseStats[itemStat.ordinal()] = EngineSetting.DEFAULT_ATTRIBUTE_VALUE;

        baseStats[ItemStat.ARMOR.ordinal()] = EngineSetting.DEFAULT_ARMOR;
        baseStats[ItemStat.DAMAGE.ordinal()] = EngineSetting.DEFAULT_DAMAGE;
        baseStats[ItemStat.HEALTH.ordinal()] = EngineSetting.DEFAULT_HEALTH;
        baseStats[ItemStat.STAMINA.ordinal()] = EngineSetting.DEFAULT_STAMINA;
    }

    public float getBaseStat(ItemStat itemStat) {
        return baseStats[itemStat.ordinal()];
    }

    public void setBaseStat(ItemStat itemStat, float value) {
        baseStats[itemStat.ordinal()] = value;
    }

    // Accessible \\

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public void setWalkSpeed(float walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }

    public void setMovementSpeed(float movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    public float getSprintSpeed() {
        return sprintSpeed;
    }

    public void setSprintSpeed(float sprintSpeed) {
        this.sprintSpeed = sprintSpeed;
    }

    public float getJumpHeight() {
        return jumpHeight;
    }

    public void setJumpHeight(float jumpHeight) {
        this.jumpHeight = jumpHeight;
    }

    public float getSwimSpeed() {
        return swimSpeed;
    }

    public void setSwimSpeed(float swimSpeed) {
        this.swimSpeed = swimSpeed;
    }

    public float getReach() {
        return reach;
    }

    public void setReach(float reach) {
        this.reach = reach;
    }
}