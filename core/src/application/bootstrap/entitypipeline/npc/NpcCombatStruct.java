package application.bootstrap.entitypipeline.npc;

import application.bootstrap.entitypipeline.entity.CombatDirection;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class NpcCombatStruct extends StructPackage {

    /*
     * The fight in progress of one NPC, kept by its combat mind. Offense is
     * the pause before it presses its next attack, how many more attacks it
     * means to carry that one into, and whether it means to feint it. Defense
     * is the last attack of its foe's it has seen coming, by that attack's
     * sequence, the seconds still to pass before it answers it, the side it
     * will raise its guard toward, and how long the guard stays up once the
     * blow is past. Footwork is the side it circles its foe toward, in its own
     * frame, and for how long.
     * Flight is the point it runs for, in the frame of the chunk it fled
     * from. reset() starts every fight afresh.
     */

    // Offense
    private float attackPause;
    private int comboLeft;
    private boolean feintPlanned;

    // Defense
    private int watchedSequence;
    private float reactionSeconds;
    private CombatDirection guardDirection;
    private float guardLinger;

    // Footwork
    private CombatDirection strafeSide;
    private float strafeSeconds;

    // Flight
    private boolean fleeing;
    private long fleeChunkCoordinate;
    private final Vector3 fleeTarget;

    // Constructor \\

    public NpcCombatStruct() {

        // Flight
        this.fleeTarget = new Vector3();

        reset();
    }

    // Management \\

    public void reset() {

        // Offense
        attackPause = 0f;
        comboLeft = 0;
        feintPlanned = false;

        // Defense
        watchedSequence = EngineSetting.NPC_SEQUENCE_NONE;
        reactionSeconds = 0f;
        guardDirection = CombatDirection.NEUTRAL;
        guardLinger = 0f;

        // Footwork
        strafeSide = CombatDirection.NEUTRAL;
        strafeSeconds = 0f;

        // Flight
        fleeing = false;
    }

    public void advance(float deltaTime) {
        attackPause -= deltaTime;
        reactionSeconds -= deltaTime;
        guardLinger -= deltaTime;
        strafeSeconds -= deltaTime;
    }

    // Offense \\

    // An attack pressed — the pause before the next, the attacks it carries into, and whether it is feinted
    public void beginExchange(float pause, int combo, boolean feint) {
        attackPause = pause;
        comboLeft = combo;
        feintPlanned = feint;
    }

    public void spendCombo() {
        comboLeft--;
    }

    // A feint thrown — the next attack follows it after the pause given
    public void feint(float pause) {
        feintPlanned = false;
        comboLeft = 0;
        attackPause = pause;
    }

    // Defense \\

    // An attack of the foe's seen coming, answered after the reaction given by a guard toward a side
    public void watch(int sequence, float reaction, CombatDirection direction) {
        watchedSequence = sequence;
        reactionSeconds = reaction;
        guardDirection = direction;
    }

    public void linger(float seconds) {
        guardLinger = seconds;
    }

    public void dropGuard() {
        guardLinger = 0f;
    }

    // Footwork \\

    // Circling toward the foe's left or right, or standing its ground for NEUTRAL, for some seconds
    public void strafe(CombatDirection side, float seconds) {
        strafeSide = side;
        strafeSeconds = seconds;
    }

    // Flight \\

    public void beginFlight(long chunkCoordinate, float x, float y, float z) {
        fleeing = true;
        fleeChunkCoordinate = chunkCoordinate;
        fleeTarget.set(x, y, z);
    }

    public void endFlight() {
        fleeing = false;
    }

    // Accessible \\

    public boolean isPaused() {
        return attackPause > 0f;
    }

    public int getComboLeft() {
        return comboLeft;
    }

    public boolean isFeintPlanned() {
        return feintPlanned;
    }

    public int getWatchedSequence() {
        return watchedSequence;
    }

    // True once the reaction to the attack last seen coming has passed
    public boolean isReactionDue() {
        return reactionSeconds <= 0f;
    }

    public CombatDirection getGuardDirection() {
        return guardDirection;
    }

    public boolean isLingering() {
        return guardLinger > 0f;
    }

    public CombatDirection getStrafeSide() {
        return strafeSide;
    }

    public boolean isStrafeDue() {
        return strafeSeconds <= 0f;
    }

    public boolean isFleeing() {
        return fleeing;
    }

    public long getFleeChunkCoordinate() {
        return fleeChunkCoordinate;
    }

    public Vector3 getFleeTarget() {
        return fleeTarget;
    }
}
