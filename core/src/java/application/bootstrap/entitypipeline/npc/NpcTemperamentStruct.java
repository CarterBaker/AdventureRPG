package application.bootstrap.entitypipeline.npc;

import engine.root.StructPackage;

public class NpcTemperamentStruct extends StructPackage {

    /*
     * Immutable fighting temper of an NPC definition: how far it sees and how
     * far from home it will chase, the opinion of another being at or below
     * which it turns on it, the share of its health at or below which it runs
     * instead, and whether it takes the side of a being it likes when one it
     * likes less strikes it. In a fight, how many seconds it takes to answer
     * a blow it sees coming, the chance it reads which side that blow comes
     * from, how quickly it presses its attacks, the chance it carries one
     * attack into another, and the chance it feints one into a guard.
     */

    // Senses
    private final float sightBlocks;
    private final float leashBlocks;

    // Temper
    private final float hostility;
    private final float fleeHealth;
    private final boolean defends;

    // Fighting
    private final float reactionSeconds;
    private final float guardChance;
    private final float aggression;
    private final float comboChance;
    private final float feintChance;

    // Constructor \\

    public NpcTemperamentStruct(
            float sightBlocks,
            float leashBlocks,
            float hostility,
            float fleeHealth,
            boolean defends,
            float reactionSeconds,
            float guardChance,
            float aggression,
            float comboChance,
            float feintChance) {

        // Senses
        this.sightBlocks = sightBlocks;
        this.leashBlocks = leashBlocks;

        // Temper
        this.hostility = hostility;
        this.fleeHealth = fleeHealth;
        this.defends = defends;

        // Fighting
        this.reactionSeconds = reactionSeconds;
        this.guardChance = guardChance;
        this.aggression = aggression;
        this.comboChance = comboChance;
        this.feintChance = feintChance;
    }

    // Accessible \\

    public float getSightBlocks() {
        return sightBlocks;
    }

    public float getLeashBlocks() {
        return leashBlocks;
    }

    public float getHostility() {
        return hostility;
    }

    public float getFleeHealth() {
        return fleeHealth;
    }

    public boolean isDefends() {
        return defends;
    }

    public float getReactionSeconds() {
        return reactionSeconds;
    }

    public float getGuardChance() {
        return guardChance;
    }

    public float getAggression() {
        return aggression;
    }

    public float getComboChance() {
        return comboChance;
    }

    public float getFeintChance() {
        return feintChance;
    }
}
