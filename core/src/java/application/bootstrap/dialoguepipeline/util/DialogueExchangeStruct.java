package application.bootstrap.dialoguepipeline.util;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class DialogueExchangeStruct extends StructPackage {

    /*
     * One talk between a speaker and the being it speaks to, a turn at a
     * time: the being's profile, what was last heard and what was last said
     * in answer, and nothing earlier — a talk keeps no history. Around those
     * it carries everything the turn worked out, so the turn can be judged
     * and learned from afterwards: what was understood and how surely, the
     * topic raised and where the being stands on it, the profile as it stood
     * when the reply was chosen, the replies that were open and the share
     * each was given, the reply chosen, what it did, and the lines and
     * dictionary words its wording was built from. Each exchange draws from
     * its own stream of rolls, so one seeded alike answers alike. Created
     * through DialogueManager, which sizes it from the registries.
     */

    // Talk
    private final DialogueProfileStruct profile;
    private final StringBuilder heard;
    private final StringBuilder said;
    private long rollState;

    // Understanding
    private final int[] heardFeatures;
    private int heardFeatureCount;
    private int knownWordCount;
    private short intentID;
    private float confidence;
    private float tone;
    private short topicID;
    private int subjectWordID;
    private DialogueTopicState topicState;

    // Response
    private final DialogueProfileStruct decided;
    private final int[] responseFeatures;
    private int responseFeatureCount;
    private final short[] candidateIDs;
    private final float[] candidateShares;
    private int candidateCount;
    private short replyID;
    private DialogueAction action;
    private float affinityShift;

    // Wording
    private final IntArrayList lineKeys;
    private final IntArrayList wordIDs;
    private int saidHash;

    // Constructor \\

    public DialogueExchangeStruct(int moodIDCount, int replyIDCount) {

        // Talk
        this.profile = new DialogueProfileStruct(moodIDCount);
        this.heard = new StringBuilder();
        this.said = new StringBuilder();
        this.rollState = EngineSetting.DIALOGUE_ROLL_SEED;

        // Understanding
        this.heardFeatures = new int[EngineSetting.DIALOGUE_HEARD_FEATURE_CAPACITY];
        this.topicState = DialogueTopicState.NONE;

        // Response
        this.decided = new DialogueProfileStruct(moodIDCount);
        this.responseFeatures = new int[EngineSetting.DIALOGUE_RESPONSE_FEATURE_CAPACITY];
        this.candidateIDs = new short[replyIDCount];
        this.candidateShares = new float[replyIDCount];
        this.action = DialogueAction.NONE;

        // Wording
        this.lineKeys = new IntArrayList();
        this.wordIDs = new IntArrayList();
    }

    // Talk \\

    // A new talk: nothing heard, nothing said and nothing remembered of the last one
    public void begin(long seed) {

        heard.setLength(0);
        said.setLength(0);
        this.rollState = seed == 0L ? EngineSetting.DIALOGUE_ROLL_SEED : seed;
        this.saidHash = 0;

        beginTurn();
    }

    // A new turn forgets everything the last one worked out but what was said, which the next wording steers off
    public void beginTurn() {

        // Understanding
        this.heardFeatureCount = 0;
        this.knownWordCount = 0;
        this.intentID = EngineSetting.REGISTRY_RESERVED_ID;
        this.confidence = 0f;
        this.tone = 0f;
        this.topicID = EngineSetting.REGISTRY_RESERVED_ID;
        this.subjectWordID = EngineSetting.REGISTRY_RESERVED_ID;
        this.topicState = DialogueTopicState.NONE;

        // Response
        this.responseFeatureCount = 0;
        this.candidateCount = 0;
        this.replyID = EngineSetting.REGISTRY_RESERVED_ID;
        this.action = DialogueAction.NONE;
        this.affinityShift = 0f;

        // Wording
        lineKeys.clear();
        wordIDs.clear();
    }

    // The next roll from 0 up to but never reaching 1
    public float roll() {

        rollState ^= rollState >>> 12;
        rollState ^= rollState << 25;
        rollState ^= rollState >>> 27;

        return ((rollState * EngineSetting.DIALOGUE_ROLL_MULTIPLIER) >>> 40) * EngineSetting.DIALOGUE_ROLL_SCALE;
    }

    // Understanding \\

    // A feature past the most a sentence may raise is left off
    public void addHeardFeature(int feature) {

        if (heardFeatureCount < heardFeatures.length)
            heardFeatures[heardFeatureCount++] = feature;
    }

    public void addResponseFeature(int feature) {

        if (responseFeatureCount < responseFeatures.length)
            responseFeatures[responseFeatureCount++] = feature;
    }

    public void countKnownWord() {
        knownWordCount++;
    }

    public void setUnderstanding(short intentID, float confidence) {
        this.intentID = intentID;
        this.confidence = confidence;
    }

    public void setTone(float tone) {
        this.tone = tone;
    }

    public void setTopic(short topicID, int subjectWordID) {
        this.topicID = topicID;
        this.subjectWordID = subjectWordID;
    }

    public void setTopicState(DialogueTopicState topicState) {
        this.topicState = topicState;
    }

    // Response \\

    public void clearCandidates() {
        candidateCount = 0;
    }

    public void addCandidate(short replyID) {
        candidateIDs[candidateCount++] = replyID;
    }

    // The place of a reply among the candidates, not found when it was never open
    public int findCandidate(short replyID) {

        for (int i = 0; i < candidateCount; i++)
            if (candidateIDs[i] == replyID)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    public void setReply(short replyID, DialogueAction action) {
        this.replyID = replyID;
        this.action = action;
    }

    public void addAffinityShift(float amount) {
        affinityShift += amount;
    }

    // Wording \\

    public void setSaidHash(int saidHash) {
        this.saidHash = saidHash;
    }

    // Accessible \\

    public DialogueProfileStruct getProfile() {
        return profile;
    }

    public StringBuilder getHeard() {
        return heard;
    }

    public StringBuilder getSaid() {
        return said;
    }

    public int[] getHeardFeatures() {
        return heardFeatures;
    }

    public int getHeardFeatureCount() {
        return heardFeatureCount;
    }

    public int getKnownWordCount() {
        return knownWordCount;
    }

    public short getIntentID() {
        return intentID;
    }

    public float getConfidence() {
        return confidence;
    }

    public float getTone() {
        return tone;
    }

    // -1 for harsh words, 1 for kind ones and 0 between
    public int getToneBand() {

        if (tone <= -EngineSetting.DIALOGUE_TONE_BAND)
            return -1;

        return tone >= EngineSetting.DIALOGUE_TONE_BAND ? 1 : 0;
    }

    public short getTopicID() {
        return topicID;
    }

    public int getSubjectWordID() {
        return subjectWordID;
    }

    public DialogueTopicState getTopicState() {
        return topicState;
    }

    public DialogueProfileStruct getDecided() {
        return decided;
    }

    public int[] getResponseFeatures() {
        return responseFeatures;
    }

    public int getResponseFeatureCount() {
        return responseFeatureCount;
    }

    public void clearResponseFeatures() {
        responseFeatureCount = 0;
    }

    public short[] getCandidateIDs() {
        return candidateIDs;
    }

    public float[] getCandidateShares() {
        return candidateShares;
    }

    public int getCandidateCount() {
        return candidateCount;
    }

    public short getReplyID() {
        return replyID;
    }

    public boolean hasReply() {
        return replyID != EngineSetting.REGISTRY_RESERVED_ID;
    }

    public DialogueAction getAction() {
        return action;
    }

    public float getAffinityShift() {
        return affinityShift;
    }

    public IntArrayList getLineKeys() {
        return lineKeys;
    }

    public IntArrayList getWordIDs() {
        return wordIDs;
    }

    public int getSaidHash() {
        return saidHash;
    }
}
