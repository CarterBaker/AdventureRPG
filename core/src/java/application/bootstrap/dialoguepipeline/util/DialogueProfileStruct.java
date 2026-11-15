package application.bootstrap.dialoguepipeline.util;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class DialogueProfileStruct extends StructPackage {

    /*
     * The one being a talk is held with, as the dialogue models see it: its
     * name, title and profession, the traits it was made with, how strongly
     * it feels each mood right now, what it thinks of whoever is speaking,
     * where its own morality stands and what the speaker is to it. A resident
     * is copied into one before every turn and what the turn changed is
     * written back; a trainer scenario is one outright. Mood levels are
     * indexed by mood ID, and the mood it feels most, once past the
     * threshold, is its dominant mood; none at all leaves it calm.
     */

    // Self
    private String name;
    private String title;
    private short professionID;

    // Traits
    private final short[] traitIDs;
    private int traitCount;
    private boolean wordless;

    // Mood
    private final float[] moodLevels;

    // Regard
    private float affinity;
    private float morality;
    private short relationID;

    // Speaker
    private String speakerName;

    // Constructor \\

    public DialogueProfileStruct(int moodIDCount) {

        // Self
        this.name = EngineSetting.DIALOGUE_TEXT_NONE;
        this.title = EngineSetting.DIALOGUE_TEXT_NONE;

        // Traits
        this.traitIDs = new short[EngineSetting.DIALOGUE_TRAIT_MAX];

        // Mood
        this.moodLevels = new float[moodIDCount];

        // Speaker
        this.speakerName = EngineSetting.DIALOGUE_TEXT_NONE;
    }

    // Management \\

    public void clear() {

        // Self
        this.name = EngineSetting.DIALOGUE_TEXT_NONE;
        this.title = EngineSetting.DIALOGUE_TEXT_NONE;
        this.professionID = EngineSetting.REGISTRY_RESERVED_ID;

        // Traits
        this.traitCount = 0;
        this.wordless = false;

        // Mood
        for (int moodID = 0; moodID < moodLevels.length; moodID++)
            moodLevels[moodID] = 0f;

        // Regard
        this.affinity = 0f;
        this.morality = 0f;
        this.relationID = EngineSetting.REGISTRY_RESERVED_ID;

        // Speaker
        this.speakerName = EngineSetting.DIALOGUE_TEXT_NONE;
    }

    public void copy(DialogueProfileStruct other) {

        // Self
        this.name = other.name;
        this.title = other.title;
        this.professionID = other.professionID;

        // Traits
        this.traitCount = other.traitCount;
        this.wordless = other.wordless;
        System.arraycopy(other.traitIDs, 0, traitIDs, 0, traitCount);

        // Mood
        System.arraycopy(other.moodLevels, 0, moodLevels, 0, moodLevels.length);

        // Regard
        this.affinity = other.affinity;
        this.morality = other.morality;
        this.relationID = other.relationID;

        // Speaker
        this.speakerName = other.speakerName;
    }

    // Traits \\

    // A trait past the most a being may hold is left off, as is one it already has
    public void addTrait(short traitID, boolean wordless) {

        if (traitCount >= traitIDs.length || hasTrait(traitID))
            return;

        traitIDs[traitCount++] = traitID;
        this.wordless |= wordless;
    }

    public boolean hasTrait(short traitID) {

        for (int i = 0; i < traitCount; i++)
            if (traitIDs[i] == traitID)
                return true;

        return false;
    }

    // Mood \\

    public void setMoodLevel(short moodID, float level) {
        moodLevels[moodID] = clampShare(level);
    }

    public void shiftMood(short moodID, float amount) {
        moodLevels[moodID] = clampShare(moodLevels[moodID] + amount);
    }

    // The mood felt most, the reserved ID when none passes the threshold
    public short getDominantMoodID() {

        short dominantMoodID = EngineSetting.REGISTRY_RESERVED_ID;
        float highest = EngineSetting.DIALOGUE_MOOD_THRESHOLD;

        for (short moodID = EngineSetting.REGISTRY_RESERVED_ID + 1; moodID < moodLevels.length; moodID++) {

            if (moodLevels[moodID] < highest)
                continue;

            highest = moodLevels[moodID];
            dominantMoodID = moodID;
        }

        return dominantMoodID;
    }

    private float clampShare(float share) {
        return Math.max(0f, Math.min(1f, share));
    }

    // Regard \\

    // The affinity moved by an amount within its range, answering how far it really moved
    public float shiftAffinity(float amount) {

        float shifted = Math.max(-EngineSetting.AFFINITY_MAX, Math.min(EngineSetting.AFFINITY_MAX, affinity + amount));
        float applied = shifted - affinity;

        this.affinity = shifted;

        return applied;
    }

    // How many band edges the affinity has passed, from 0 for the most disliked
    public int getAffinityBand() {

        int band = 0;

        for (int i = 0; i < EngineSetting.DIALOGUE_AFFINITY_BANDS.length; i++)
            if (affinity >= EngineSetting.DIALOGUE_AFFINITY_BANDS[i])
                band++;

        return band;
    }

    // -1 for a wicked being, 1 for a virtuous one and 0 between
    public int getMoralityBand() {

        if (morality <= -EngineSetting.DIALOGUE_MORALITY_BAND)
            return -1;

        return morality >= EngineSetting.DIALOGUE_MORALITY_BAND ? 1 : 0;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public short getProfessionID() {
        return professionID;
    }

    public void setProfessionID(short professionID) {
        this.professionID = professionID;
    }

    public int getTraitCount() {
        return traitCount;
    }

    public short getTraitID(int index) {
        return traitIDs[index];
    }

    public boolean isWordless() {
        return wordless;
    }

    public int getMoodIDCount() {
        return moodLevels.length;
    }

    public float getMoodLevel(short moodID) {
        return moodLevels[moodID];
    }

    public float getAffinity() {
        return affinity;
    }

    public void setAffinity(float affinity) {
        this.affinity = Math.max(-EngineSetting.AFFINITY_MAX, Math.min(EngineSetting.AFFINITY_MAX, affinity));
    }

    public float getMorality() {
        return morality;
    }

    public void setMorality(float morality) {
        this.morality = Math.max(-1f, Math.min(1f, morality));
    }

    public short getRelationID() {
        return relationID;
    }

    public void setRelationID(short relationID) {
        this.relationID = relationID;
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }
}
