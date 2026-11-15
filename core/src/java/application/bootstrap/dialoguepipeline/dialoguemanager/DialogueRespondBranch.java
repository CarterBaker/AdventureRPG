package application.bootstrap.dialoguepipeline.dialoguemanager;

import application.bootstrap.dialoguepipeline.intent.IntentHandle;
import application.bootstrap.dialoguepipeline.intentmanager.IntentManager;
import application.bootstrap.dialoguepipeline.relationmanager.RelationManager;
import application.bootstrap.dialoguepipeline.reply.ReplyHandle;
import application.bootstrap.dialoguepipeline.replymanager.ReplyManager;
import application.bootstrap.dialoguepipeline.topicmanager.TopicManager;
import application.bootstrap.dialoguepipeline.trait.TraitHandle;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.dialoguepipeline.util.DialogueExchangeStruct;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.dialoguepipeline.util.DialogueMathUtility;
import application.bootstrap.dialoguepipeline.util.DialogueProfileStruct;
import application.bootstrap.dialoguepipeline.util.DialogueTopicState;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class DialogueRespondBranch extends BranchPackage {

    /*
     * Chooses what a being answers with, for DialogueManager. respond() first
     * lets what was meant land: the intent moves the being's regard for the
     * speaker and its moods, more for harsher or kinder words and by as much
     * as its traits allow. The being as it then stands is what the choice is
     * made on. weigh() gathers the replies the intent can be answered with
     * that the being may give, falling back on the confused intent's when
     * none is open, raises a feature for everything about the being and the
     * turn — alone and paired with the intent — and gives each reply a share
     * from what its leans say of the being plus what the response model has
     * learned. One reply is drawn by those shares, and it moves the being's
     * regard and moods in turn. LearnBranch calls weigh() on a lesson's being
     * to teach the model, so a turn is weighed one way live and relearned.
     */

    // Internal
    private DialogueManager dialogueManager;
    private IntentManager intentManager;
    private ReplyManager replyManager;
    private TopicManager topicManager;
    private TraitManager traitManager;
    private RelationManager relationManager;
    private ProfessionManager professionManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.dialogueManager = get(DialogueManager.class);
        this.intentManager = get(IntentManager.class);
        this.replyManager = get(ReplyManager.class);
        this.topicManager = get(TopicManager.class);
        this.traitManager = get(TraitManager.class);
        this.relationManager = get(RelationManager.class);
        this.professionManager = get(ProfessionManager.class);
    }

    // Respond \\

    void respond(DialogueExchangeStruct exchange) {

        IntentHandle intentHandle = intentManager.getIntentHandleFromIntentID(exchange.getIntentID());
        float tone = exchange.getTone();

        move(
                exchange,
                intentHandle.getAffinity() * (1f + Math.abs(tone)) + tone * EngineSetting.DIALOGUE_TONE_AFFINITY,
                intentHandle.getMoodShifts());
        exchange.getDecided().copy(exchange.getProfile());
        weigh(exchange);

        if (exchange.getCandidateCount() == 0)
            return;

        int chosen = DialogueMathUtility.pick(
                exchange.getCandidateShares(), exchange.getCandidateCount(), exchange.roll());
        ReplyHandle replyHandle = replyManager.getReplyHandleFromReplyID(exchange.getCandidateIDs()[chosen]);

        exchange.setReply(replyHandle.getReplyID(), replyHandle.getAction());
        move(exchange, replyHandle.getAffinity(), replyHandle.getMoodShifts());
    }

    // Move \\

    // The being's regard for the speaker and its moods moved, as far as its traits scale them
    private void move(DialogueExchangeStruct exchange, float affinity, ValueMapStruct moodShifts) {

        DialogueProfileStruct profile = exchange.getProfile();
        float scale = 1f;

        for (int i = 0; i < profile.getTraitCount(); i++) {

            TraitHandle traitHandle = traitManager.getTraitHandleFromTraitID(profile.getTraitID(i));

            scale *= affinity > 0f ? traitHandle.getAffinityGain() : traitHandle.getAffinityLoss();
        }

        exchange.addAffinityShift(profile.shiftAffinity(affinity * scale));

        for (int i = 0; i < moodShifts.size(); i++)
            profile.shiftMood(
                    moodShifts.getID(i),
                    moodShifts.getValue(i) * (1f + resolveMoodLean(profile, moodShifts.getID(i))));
    }

    // How far the being's traits lean it toward a mood
    private float resolveMoodLean(DialogueProfileStruct profile, short moodID) {

        float lean = 0f;

        for (int i = 0; i < profile.getTraitCount(); i++)
            lean += traitManager.getTraitHandleFromTraitID(profile.getTraitID(i)).getMoodLeans().find(moodID, 0f);

        return lean;
    }

    // Weigh \\

    // The replies open to the being as the exchange decided it, each given its share
    void weigh(DialogueExchangeStruct exchange) {

        resolveTopicState(exchange);
        gatherCandidates(exchange, exchange.getIntentID());

        if (exchange.getCandidateCount() == 0)
            gatherCandidates(exchange, intentManager.getConfusedIntentID());

        raiseFeatures(exchange);
        shareCandidates(exchange);
    }

    private void resolveTopicState(DialogueExchangeStruct exchange) {

        if (exchange.getTopicID() == EngineSetting.REGISTRY_RESERVED_ID) {
            exchange.setTopicState(DialogueTopicState.NONE);
            return;
        }

        boolean known = topicManager.getTopicHandleFromTopicID(exchange.getTopicID()).getCondition()
                .permits(exchange.getDecided(), DialogueTopicState.KNOWN);

        exchange.setTopicState(known ? DialogueTopicState.KNOWN : DialogueTopicState.UNKNOWN);
    }

    private void gatherCandidates(DialogueExchangeStruct exchange, short intentID) {

        short[] replyIDs = intentManager.getIntentHandleFromIntentID(intentID).getReplyIDs();

        exchange.clearCandidates();

        for (int i = 0; i < replyIDs.length; i++)
            if (replyManager.getReplyHandleFromReplyID(replyIDs[i]).getCondition()
                    .permits(exchange.getDecided(), exchange.getTopicState()))
                exchange.addCandidate(replyIDs[i]);
    }

    private void raiseFeatures(DialogueExchangeStruct exchange) {

        DialogueProfileStruct profile = exchange.getDecided();
        int intentSeed = intentManager.getIntentHandleFromIntentID(exchange.getIntentID()).getSeed();
        short moodID = profile.getDominantMoodID();

        exchange.clearResponseFeatures();
        raise(exchange, intentSeed, DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_BIAS, 0));

        for (int i = 0; i < profile.getTraitCount(); i++)
            raise(exchange, intentSeed, DialogueFeatureUtility.mix(
                    EngineSetting.DIALOGUE_SALT_TRAIT,
                    traitManager.getTraitHandleFromTraitID(profile.getTraitID(i)).getSeed()));

        raise(exchange, intentSeed, DialogueFeatureUtility.mix(
                EngineSetting.DIALOGUE_SALT_MOOD, dialogueManager.resolveMoodSeed(moodID)));
        raise(exchange, intentSeed, DialogueFeatureUtility.mix(
                EngineSetting.DIALOGUE_SALT_AFFINITY, profile.getAffinityBand()));
        raise(exchange, intentSeed, DialogueFeatureUtility.mix(
                EngineSetting.DIALOGUE_SALT_MORALITY, profile.getMoralityBand()));
        raise(exchange, intentSeed, DialogueFeatureUtility.mix(
                EngineSetting.DIALOGUE_SALT_RELATION, resolveRelationSeed(profile.getRelationID())));
        raise(exchange, intentSeed, DialogueFeatureUtility.mix(
                EngineSetting.DIALOGUE_SALT_PROFESSION, resolveProfessionSeed(profile.getProfessionID())));
        raise(exchange, intentSeed, DialogueFeatureUtility.mix(
                EngineSetting.DIALOGUE_SALT_TOPIC, exchange.getTopicState().ordinal()));
        raise(exchange, intentSeed, DialogueFeatureUtility.mix(
                EngineSetting.DIALOGUE_SALT_TONE, exchange.getToneBand()));
    }

    // A feature raised alone and again paired with the intent, so the model learns both what suits a being always
    // and what suits it when told this
    private void raise(DialogueExchangeStruct exchange, int intentSeed, int feature) {
        exchange.addResponseFeature(feature);
        exchange.addResponseFeature(
                DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_INTENT, intentSeed, feature));
    }

    private int resolveRelationSeed(short relationID) {
        return relationID == EngineSetting.REGISTRY_RESERVED_ID
                ? 0
                : relationManager.getRelationHandleFromRelationID(relationID).getSeed();
    }

    private int resolveProfessionSeed(short professionID) {
        return professionID == EngineSetting.REGISTRY_RESERVED_ID
                ? 0
                : DialogueFeatureUtility.hashName(
                        professionManager.getProfessionHandleFromProfessionID(professionID).getProfessionName());
    }

    private void shareCandidates(DialogueExchangeStruct exchange) {

        short[] candidateIDs = exchange.getCandidateIDs();
        float[] shares = exchange.getCandidateShares();
        float scale = toScale(exchange.getResponseFeatureCount());

        for (int i = 0; i < exchange.getCandidateCount(); i++)
            shares[i] = replyManager.getReplyHandleFromReplyID(candidateIDs[i]).getLean()
                    .resolve(exchange.getDecided(), exchange.getTone())
                    + dialogueManager.getResponseWeights().score(
                            exchange.getResponseFeatures(), exchange.getResponseFeatureCount(), candidateIDs[i])
                            * scale;

        if (exchange.getCandidateCount() > 0)
            DialogueMathUtility.softmax(
                    shares, exchange.getCandidateCount(), EngineSetting.DIALOGUE_RESPONSE_TEMPERATURE);
    }

    float toScale(int featureCount) {
        return 1f / (float) Math.sqrt(Math.max(1, featureCount));
    }
}
