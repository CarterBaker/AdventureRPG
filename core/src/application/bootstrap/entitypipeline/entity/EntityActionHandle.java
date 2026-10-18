package application.bootstrap.entitypipeline.entity;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class EntityActionHandle extends HandlePackage {

    /*
     * Per-entity action timeline: the action under way, how far through it the
     * entity is, and the moment its effect lands — the strike of an attack or
     * the release of a throw. begin() starts a timed action and hold() a
     * stance, which rises over its duration and then stays up until it is
     * released; every start counts up the sequence, so an animation can tell a
     * second attack from the first one still fading. beginMove() and
     * holdMove() do the same for a weapon's attack or guard, carrying the side
     * it comes from, the weapon it was drawn from and the clip it plays, and
     * shift() swings a raised guard to another side, its pose already up but
     * not counted raised until the shift is done. getPhase() is where in its
     * clip the action stands: a move's strike frame always lands on the moment
     * its effect does. One attack asked for while another still plays waits in
     * the queue until it can cut in or goes stale. Advanced by CombatManager
     * and read by the entity's animation. Lives on EntityInstance.
     */

    // Action
    private EntityAction action;
    private float duration;
    private float impact;
    private float elapsed;
    private boolean resolved;
    private boolean held;

    // Move
    private CombatDirection direction;
    private short weaponID;
    private AnimationClipHandle clip;
    private float strike;
    private boolean settled;

    // Queue
    private boolean queued;
    private CombatDirection queuedDirection;
    private float queuedElapsed;

    // Sequence
    private int sequence;

    // Internal \\

    @Override
    protected void create() {

        // Action
        this.action = EntityAction.NONE;
        this.resolved = true;

        // Move
        this.direction = CombatDirection.NEUTRAL;
        this.weaponID = EngineSetting.WEAPON_NONE;

        // Queue
        this.queuedDirection = CombatDirection.NEUTRAL;
    }

    // Management \\

    // Starts an action lasting a duration in seconds, its effect landing at a fraction of it
    public void begin(EntityAction action, float duration, float impact) {

        if (action == EntityAction.NONE || duration <= 0f)
            throwException("An action needs a kind and a positive duration.");

        start(action, duration, impact);
        setMove(CombatDirection.NEUTRAL, EngineSetting.WEAPON_NONE, null, impact);
        this.resolved = false;
        this.held = false;
    }

    // Starts a weapon's move from a side, its strike frame at a fraction of its clip landing with its effect
    public void beginMove(
            EntityAction action,
            CombatDirection direction,
            short weaponID,
            AnimationClipHandle clip,
            float duration,
            float impact,
            float strike) {

        if (action == EntityAction.NONE || duration <= 0f)
            throwException("A move needs a kind and a positive duration.");

        start(action, duration, impact);
        setMove(direction, weaponID, clip, strike);
        this.resolved = false;
        this.held = false;
        this.queued = false;
    }

    // Raises a stance over a duration in seconds and keeps it up until released — it has no effect of its own
    public void hold(EntityAction action, float duration) {

        if (action == EntityAction.NONE || duration <= 0f)
            throwException("A stance needs a kind and a positive duration.");

        start(action, duration, 1f);
        setMove(CombatDirection.NEUTRAL, EngineSetting.WEAPON_NONE, null, 1f);
        this.resolved = true;
        this.held = true;
    }

    // Raises a weapon's stance toward a side over a duration in seconds and keeps it up until released
    public void holdMove(
            EntityAction action,
            CombatDirection direction,
            short weaponID,
            AnimationClipHandle clip,
            float duration) {

        if (action == EntityAction.NONE || duration <= 0f)
            throwException("A stance needs a kind and a positive duration.");

        start(action, duration, 1f);
        setMove(direction, weaponID, clip, 1f);
        this.resolved = true;
        this.held = true;
        this.queued = false;
    }

    // Swings a held stance to another side over a duration in seconds — its pose is already up, it counts once there
    public void shift(CombatDirection direction, AnimationClipHandle clip, float duration) {

        if (!isHolding() || duration <= 0f)
            throwException("Only a held stance shifts, and over a positive duration.");

        this.direction = direction;
        this.clip = clip;
        this.duration = duration;
        this.elapsed = 0f;
        this.settled = true;
        this.sequence++;
    }

    private void start(EntityAction action, float duration, float impact) {
        this.action = action;
        this.duration = duration;
        this.impact = impact;
        this.elapsed = 0f;
        this.sequence++;
    }

    private void setMove(CombatDirection direction, short weaponID, AnimationClipHandle clip, float strike) {
        this.direction = direction;
        this.weaponID = weaponID;
        this.clip = clip;
        this.strike = strike;
        this.settled = false;
    }

    // Moves the action on — true on the one frame its effect lands
    public boolean advance(float deltaTime) {

        advanceQueue(deltaTime);

        if (action == EntityAction.NONE)
            return false;

        elapsed += deltaTime;

        if (held)
            return false;

        boolean landed = !resolved && elapsed >= duration * impact;

        if (landed)
            resolved = true;

        if (elapsed >= duration)
            action = EntityAction.NONE;

        return landed;
    }

    public void release() {
        action = EntityAction.NONE;
        resolved = true;
        held = false;
        queued = false;
    }

    // Queue \\

    // Keeps the next attack asked for until it can start, the latest ask replacing any before it
    public void queue(CombatDirection direction) {
        this.queued = true;
        this.queuedDirection = direction;
        this.queuedElapsed = 0f;
    }

    public void clearQueue() {
        queued = false;
    }

    private void advanceQueue(float deltaTime) {

        if (!queued)
            return;

        queuedElapsed += deltaTime;

        if (queuedElapsed > EngineSetting.ATTACK_BUFFER_SECONDS)
            queued = false;
    }

    // Accessible \\

    public EntityAction getAction() {
        return action;
    }

    public boolean isActing() {
        return action != EntityAction.NONE;
    }

    public boolean isHolding() {
        return held && action != EntityAction.NONE;
    }

    public boolean isRaised() {
        return isHolding() && elapsed >= duration;
    }

    // True while a timed action has yet to land its effect
    public boolean isWindingUp() {
        return action != EntityAction.NONE && !held && !resolved;
    }

    // Seconds until a timed action lands its effect, zero once it has
    public float getTimeToImpact() {
        return isWindingUp() ? duration * impact - elapsed : 0f;
    }

    // How far a timed action has come back since its effect landed, from 0 to 1
    public float getRecoveryProgress() {

        if (isWindingUp() || held || action == EntityAction.NONE)
            return 0f;

        float recovery = duration * (1f - impact);

        return recovery > 0f ? Math.min(1f, (elapsed - duration * impact) / recovery) : 1f;
    }

    public float getProgress() {
        return action == EntityAction.NONE ? 0f : Math.min(1f, elapsed / duration);
    }

    // Where the action stands in its clip — the strike frame meets the impact, a shifted stance stays up
    public float getPhase() {

        if (action == EntityAction.NONE)
            return 0f;

        if (settled)
            return 1f;

        float progress = getProgress();

        if (progress <= impact)
            return impact > 0f ? progress / impact * strike : strike;

        return strike + (progress - impact) / (1f - impact) * (1f - strike);
    }

    public CombatDirection getDirection() {
        return direction;
    }

    public short getWeaponID() {
        return weaponID;
    }

    public AnimationClipHandle getClip() {
        return clip;
    }

    public boolean isQueued() {
        return queued;
    }

    public CombatDirection getQueuedDirection() {
        return queuedDirection;
    }

    public int getSequence() {
        return sequence;
    }
}
