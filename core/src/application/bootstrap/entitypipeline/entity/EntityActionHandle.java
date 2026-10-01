package application.bootstrap.entitypipeline.entity;

import engine.root.HandlePackage;

public class EntityActionHandle extends HandlePackage {

    /*
     * Per-entity action timeline: the action under way, how far through it
     * the entity is, and the moment its effect lands — the strike of a swing
     * or the release of a throw. begin() starts a timed action and hold() a
     * stance, which rises over its duration and then stays up until it is
     * released; every start counts up the sequence, so an animation can tell
     * a second swing from the first one still fading. Advanced by
     * CombatManager and read by the entity's animation, which plays the
     * action's node at exactly its progress. Lives on EntityInstance.
     */

    // Action
    private EntityAction action;
    private float duration;
    private float impact;
    private float elapsed;
    private boolean resolved;
    private boolean held;

    // Sequence
    private int sequence;

    // Internal \\

    @Override
    protected void create() {

        // Action
        this.action = EntityAction.NONE;
        this.resolved = true;
    }

    // Management \\

    // Starts an action lasting a duration in seconds, its effect landing at a fraction of it
    public void begin(EntityAction action, float duration, float impact) {

        if (action == EntityAction.NONE || duration <= 0f)
            throwException("An action needs a kind and a positive duration.");

        start(action, duration, impact);
        this.resolved = false;
        this.held = false;
    }

    // Raises a stance over a duration in seconds and keeps it up until released — it has no effect of its own
    public void hold(EntityAction action, float duration) {

        if (action == EntityAction.NONE || duration <= 0f)
            throwException("A stance needs a kind and a positive duration.");

        start(action, duration, 1f);
        this.resolved = true;
        this.held = true;
    }

    private void start(EntityAction action, float duration, float impact) {
        this.action = action;
        this.duration = duration;
        this.impact = impact;
        this.elapsed = 0f;
        this.sequence++;
    }

    // Moves the action on — true on the one frame its effect lands
    public boolean advance(float deltaTime) {

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

    public float getProgress() {
        return action == EntityAction.NONE ? 0f : Math.min(1f, elapsed / duration);
    }

    public int getSequence() {
        return sequence;
    }
}
