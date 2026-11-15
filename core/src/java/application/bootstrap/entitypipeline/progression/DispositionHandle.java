package application.bootstrap.entitypipeline.progression;

import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap;

public class DispositionHandle extends HandlePackage {

    /*
     * Per-being nature and temper, which every talk with it is held on: the
     * name it goes by, the traits it was made with, how strongly it feels
     * each mood and the game time those levels were last settled at, and
     * what each other being it is bound to is to it, by identity, with the
     * regard that bond starts from. Mood levels are indexed by mood ID. Only
     * DialogueManager changes it. Lives on EntityInstance.
     */

    // Internal
    private MoodManager moodManager;

    // Identity
    private String name;

    // Traits
    private short[] traitIDs;
    private int traitCount;

    // Moods
    private float[] moodLevels;
    private double moodTime;

    // Relations
    private Long2ShortOpenHashMap identity2RelationID;
    private Long2FloatOpenHashMap identity2Bond;

    // Internal \\

    @Override
    protected void create() {

        // Identity
        this.name = EngineSetting.DIALOGUE_TEXT_NONE;

        // Traits
        this.traitIDs = new short[EngineSetting.DIALOGUE_TRAIT_MAX];

        // Relations
        this.identity2RelationID = new Long2ShortOpenHashMap();
        this.identity2Bond = new Long2FloatOpenHashMap();
    }

    @Override
    protected void get() {

        // Internal
        this.moodManager = get(MoodManager.class);
    }

    @Override
    protected void awake() {

        // Moods
        this.moodLevels = new float[moodManager.getMoodIDCount()];
    }

    // Management \\

    public void reset(double time) {

        // Identity
        this.name = EngineSetting.DIALOGUE_TEXT_NONE;

        // Traits
        this.traitCount = 0;

        // Moods
        for (int moodID = 0; moodID < moodLevels.length; moodID++)
            moodLevels[moodID] = 0f;

        this.moodTime = time;

        // Relations
        identity2RelationID.clear();
        identity2Bond.clear();
    }

    // A trait past the most a being may hold is left off, as is one it already has
    public void addTrait(short traitID) {

        if (traitCount >= traitIDs.length || hasTrait(traitID))
            return;

        traitIDs[traitCount++] = traitID;
    }

    public boolean hasTrait(short traitID) {

        for (int i = 0; i < traitCount; i++)
            if (traitIDs[i] == traitID)
                return true;

        return false;
    }

    public void setRelation(long otherIdentity, short relationID, float bond) {
        identity2RelationID.put(otherIdentity, relationID);
        identity2Bond.put(otherIdentity, bond);
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTraitCount() {
        return traitCount;
    }

    public short getTraitID(int index) {
        return traitIDs[index];
    }

    public int getMoodIDCount() {
        return moodLevels.length;
    }

    public float getMoodLevel(short moodID) {
        return moodLevels[moodID];
    }

    public void setMoodLevel(short moodID, float level) {
        moodLevels[moodID] = level;
    }

    public double getMoodTime() {
        return moodTime;
    }

    public void setMoodTime(double moodTime) {
        this.moodTime = moodTime;
    }

    // What another being is to this one, the reserved ID for a stranger
    public short getRelationID(long otherIdentity) {
        return identity2RelationID.get(otherIdentity);
    }

    // The regard this being starts from for another it is bound to, none for a stranger
    public float getBond(long otherIdentity) {
        return identity2Bond.get(otherIdentity);
    }
}
