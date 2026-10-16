package application.bootstrap.entitypipeline.progression;

import application.bootstrap.progressionpipeline.deedmanager.DeedManager;
import application.bootstrap.progressionpipeline.util.ProficiencyStruct;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;

public class ConductHandle extends HandlePackage {

    /*
     * Per-being conscience and regard. The identity is the stable number other
     * beings remember this one by: a character's for a player, its world
     * spawn's for an NPC, so one that leaves and returns with its chunk is
     * known again. Morality follows the same ranged rules as a trade but never
     * decays. Affinities are how much this being likes each other being it
     * knows, by identity. Deed counts tally every counted deed it has done,
     * indexed by deed ID. Only ProgressionManager changes it. Lives on
     * EntityInstance.
     */

    // Internal
    private DeedManager deedManager;

    // Identity
    private long identity;

    // Morality
    private ProficiencyStruct morality;

    // Affinity
    private Long2FloatOpenHashMap identity2Affinity;

    // Deeds
    private int[] deedCounts;

    // Internal \\

    @Override
    protected void create() {

        // Morality
        this.morality = new ProficiencyStruct();

        // Affinity
        this.identity2Affinity = new Long2FloatOpenHashMap();
    }

    @Override
    protected void get() {

        // Internal
        this.deedManager = get(DeedManager.class);
    }

    @Override
    protected void awake() {

        // Deeds
        this.deedCounts = new int[deedManager.getDeedIDCount()];
    }

    // Management \\

    public void reset(long identity, double time) {

        // Identity
        this.identity = identity;

        // Morality
        morality.reset(0f, time);

        // Affinity
        identity2Affinity.clear();

        // Deeds
        for (int deedID = EngineSetting.REGISTRY_RESERVED_ID + 1; deedID < deedCounts.length; deedID++)
            deedCounts[deedID] = 0;
    }

    public void countDeed(short deedID) {
        deedCounts[deedID]++;
    }

    // Accessible \\

    public long getIdentity() {
        return identity;
    }

    public void setIdentity(long identity) {
        this.identity = identity;
    }

    public ProficiencyStruct getMorality() {
        return morality;
    }

    public float getAffinity(long otherIdentity) {
        return identity2Affinity.get(otherIdentity);
    }

    public void setAffinity(long otherIdentity, float affinity) {

        if (affinity == 0f) {
            identity2Affinity.remove(otherIdentity);
            return;
        }

        identity2Affinity.put(otherIdentity, affinity);
    }

    public Long2FloatOpenHashMap getAffinities() {
        return identity2Affinity;
    }

    public int getDeedCount(short deedID) {
        return deedCounts[deedID];
    }

    public void setDeedCount(short deedID, int count) {
        deedCounts[deedID] = count;
    }
}
