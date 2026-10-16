package application.bootstrap.entitypipeline.npc;

public enum NpcMind {

    /*
     * What an NPC is minded to do, settled each frame by its perception. CALM
     * wanders about home, FIGHT closes on its foe and fights it, FLEE runs
     * from it, and RETURN walks home once a fight is over or given up,
     * heeding nothing on the way but a fresh blow.
     */

    CALM,
    FIGHT,
    FLEE,
    RETURN;

    // Values
    public static final NpcMind[] VALUES = values();
}
