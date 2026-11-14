package application.bootstrap.entitypipeline.statistics;

import application.bootstrap.progressionpipeline.statmanager.StatManager;
import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class StatisticsHandle extends HandlePackage {

    /*
     * Per-entity runtime statistics. Holds movement speeds, jump height and
     * reach; every stat's resolved total, indexed by stat ID — a scaled stat
     * relative to the entity's own level — with the revisions of everything it
     * was resolved from, so it is rebuilt only once one of them moves; and the
     * entity's vitality: the share of its health it has left, how freshly it
     * bleeds, how long until it regenerates again, and whether it has been
     * defeated. Health is kept as a share so a change in its maximum neither
     * heals nor wounds. Only ProgressionManager resolves the stats and
     * CombatManager and ProgressionManager move the vitality. Lives on
     * EntityInstance.
     */

    // Internal
    private StatManager statManager;

    // Movement
    private float walkSpeed;
    private float movementSpeed;
    private float sprintSpeed;
    private float swimSpeed;

    // Physics
    private float jumpHeight;

    // Interaction
    private float reach;

    // Stats
    private float[] stats;
    private boolean resolved;
    private int inventoryRevision;
    private int progressionRevision;
    private int proficiencyRevision;
    private int careerRevision;

    // Vitality
    private float health;
    private float bleed;
    private float regenerationDelay;
    private boolean defeated;

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

        resetVitality();
    }

    @Override
    protected void get() {

        // Internal
        this.statManager = get(StatManager.class);
    }

    @Override
    protected void awake() {

        // Stats
        this.stats = new float[statManager.getStatIDCount()];
    }

    // Stats \\

    // True when the stats were resolved from exactly these revisions
    public boolean isResolved(
            int inventoryRevision,
            int progressionRevision,
            int proficiencyRevision,
            int careerRevision) {
        return resolved
                && this.inventoryRevision == inventoryRevision
                && this.progressionRevision == progressionRevision
                && this.proficiencyRevision == proficiencyRevision
                && this.careerRevision == careerRevision;
    }

    public void markResolved(
            int inventoryRevision,
            int progressionRevision,
            int proficiencyRevision,
            int careerRevision) {

        this.resolved = true;
        this.inventoryRevision = inventoryRevision;
        this.progressionRevision = progressionRevision;
        this.proficiencyRevision = proficiencyRevision;
        this.careerRevision = careerRevision;
    }

    public void invalidate() {
        resolved = false;
    }

    public float[] getStats() {
        return stats;
    }

    // Vitality \\

    public void resetVitality() {
        this.health = EngineSetting.VITALITY_FULL;
        this.bleed = 0f;
        this.regenerationDelay = 0f;
        this.defeated = false;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public float getBleed() {
        return bleed;
    }

    public void setBleed(float bleed) {
        this.bleed = bleed;
    }

    public float getRegenerationDelay() {
        return regenerationDelay;
    }

    public void setRegenerationDelay(float regenerationDelay) {
        this.regenerationDelay = regenerationDelay;
    }

    public boolean isDefeated() {
        return defeated;
    }

    public void setDefeated(boolean defeated) {
        this.defeated = defeated;
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
