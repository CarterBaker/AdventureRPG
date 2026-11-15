package application.bootstrap.dialoguepipeline.dialoguemanager;

import java.util.Arrays;

import application.bootstrap.dialoguepipeline.intent.IntentHandle;
import application.bootstrap.dialoguepipeline.intentmanager.IntentManager;
import application.bootstrap.dialoguepipeline.lesson.LessonStruct;
import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.relationmanager.RelationManager;
import application.bootstrap.dialoguepipeline.replymanager.ReplyManager;
import application.bootstrap.dialoguepipeline.trait.TraitHandle;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.dialoguepipeline.util.DialogueExchangeStruct;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.dialoguepipeline.util.DialogueProfileStruct;
import application.bootstrap.dialoguepipeline.wordmanager.WordManager;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class DialogueLearnBranch extends BranchPackage {

    /*
     * Teaches the three dialogue models, for DialogueManager. train() builds
     * them from nothing, the same way on every machine: the understanding
     * model learns every intent's examples and every lesson that says what a
     * sentence meant, passed over a fixed number of times in a fixed shuffle,
     * and the response and wording models are then taught every lesson in the
     * order it was recorded. learn() teaches one new lesson at once, so a
     * verdict shows in the very next turn. record() turns a judged exchange
     * into the lesson both read. A lesson is replayed through the same
     * reading, weighing and feature code a live turn runs, on the being as
     * the lesson recorded it, and any name it holds that the content has
     * since lost is passed over. The understanding model moves toward the
     * true intent by its error on each example; the response model moves the
     * chosen reply's share up or down by the verdict, or straight toward the
     * better reply when one was named; the wording model raises or lowers
     * every feature of the wording that was judged.
     */

    // Internal
    private DialogueManager dialogueManager;
    private DialogueUnderstandBranch dialogueUnderstandBranch;
    private DialogueRespondBranch dialogueRespondBranch;
    private DialogueWordingBranch dialogueWordingBranch;
    private IntentManager intentManager;
    private ReplyManager replyManager;
    private TraitManager traitManager;
    private MoodManager moodManager;
    private RelationManager relationManager;
    private ProfessionManager professionManager;
    private WordManager wordManager;

    // Scratch
    private DialogueExchangeStruct lessonExchange;
    private float[] amountScratch;
    private IntArrayList keyScratch;
    private IntArrayList seedScratch;
    private int[] featureScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.keyScratch = new IntArrayList();
        this.seedScratch = new IntArrayList();
        this.featureScratch = new int[EngineSetting.DIALOGUE_WORDING_FEATURE_CAPACITY];
    }

    @Override
    protected void get() {

        // Internal
        this.dialogueManager = get(DialogueManager.class);
        this.dialogueUnderstandBranch = get(DialogueUnderstandBranch.class);
        this.dialogueRespondBranch = get(DialogueRespondBranch.class);
        this.dialogueWordingBranch = get(DialogueWordingBranch.class);
        this.intentManager = get(IntentManager.class);
        this.replyManager = get(ReplyManager.class);
        this.traitManager = get(TraitManager.class);
        this.moodManager = get(MoodManager.class);
        this.relationManager = get(RelationManager.class);
        this.professionManager = get(ProfessionManager.class);
        this.wordManager = get(WordManager.class);
    }

    @Override
    protected void awake() {

        // Scratch
        this.lessonExchange = dialogueManager.createExchange();
        this.amountScratch = new float[intentManager.getIntentIDCount()];

        train();
    }

    // Train \\

    void train() {

        ObjectArrayList<LessonStruct> lessons = dialogueManager.getLessons();

        dialogueManager.getUnderstandingWeights().clear();
        dialogueManager.getResponseWeights().clear();
        dialogueManager.getWordingWeights().clear();

        trainUnderstanding(lessons);

        for (int i = 0; i < lessons.size(); i++) {
            restore(lessons.get(i));
            teachResponse(lessons.get(i));
            teachWording(lessons.get(i));
        }
    }

    // Every example read once into its features, then passed over in a shuffle drawn afresh each time
    private void trainUnderstanding(ObjectArrayList<LessonStruct> lessons) {

        ObjectArrayList<int[]> examples = new ObjectArrayList<>();
        ShortArrayList labels = new ShortArrayList();

        for (short intentID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                intentID < intentManager.getIntentIDCount(); intentID++) {

            String[] sentences = intentManager.getIntentHandleFromIntentID(intentID).getExamples();

            for (int i = 0; i < sentences.length; i++)
                addExample(examples, labels, sentences[i], intentID);
        }

        for (int i = 0; i < lessons.size(); i++)
            if (teachesUnderstanding(lessons.get(i)))
                addExample(
                        examples, labels, lessons.get(i).getHeard(),
                        intentManager.getIntentIDFromIntentName(lessons.get(i).getIntentName()));

        int[] order = new int[examples.size()];

        for (int i = 0; i < order.length; i++)
            order[i] = i;

        lessonExchange.begin(EngineSetting.DIALOGUE_ROLL_SEED);

        for (int pass = 0; pass < EngineSetting.DIALOGUE_UNDERSTANDING_PASSES; pass++) {

            shuffle(order);

            for (int i = 0; i < order.length; i++) {
                int[] features = examples.get(order[i]);
                stepUnderstanding(features, features.length, labels.getShort(order[i]));
            }
        }
    }

    private void addExample(ObjectArrayList<int[]> examples, ShortArrayList labels, String sentence, short intentID) {

        read(sentence);
        examples.add(Arrays.copyOf(lessonExchange.getHeardFeatures(), lessonExchange.getHeardFeatureCount()));
        labels.add(intentID);
    }

    private void shuffle(int[] order) {

        for (int i = order.length - 1; i > 0; i--) {

            int other = (int) (lessonExchange.roll() * (i + 1));
            int held = order[i];

            order[i] = order[other];
            order[other] = held;
        }
    }

    // True for a lesson that says what its sentence meant, by an intent the content still holds
    private boolean teachesUnderstanding(LessonStruct lesson) {
        return lesson.getUnderstanding() > 0f
                && intentManager.hasIntent(lesson.getIntentName())
                && !lesson.getIntentName().equals(EngineSetting.DIALOGUE_INTENT_CONFUSED);
    }

    // Learn \\

    // One new lesson taught to all three models at once
    void learn(LessonStruct lesson) {

        if (teachesUnderstanding(lesson)) {

            short intentID = intentManager.getIntentIDFromIntentName(lesson.getIntentName());

            read(lesson.getHeard());

            for (int i = 0; i < EngineSetting.DIALOGUE_UNDERSTANDING_STEPS; i++)
                stepUnderstanding(
                        lessonExchange.getHeardFeatures(), lessonExchange.getHeardFeatureCount(), intentID);
        }

        restore(lesson);
        teachResponse(lesson);
        teachWording(lesson);
    }

    // Understanding \\

    private void read(String sentence) {

        lessonExchange.beginTurn();
        lessonExchange.getHeard().setLength(0);
        lessonExchange.getHeard().append(sentence);
        dialogueUnderstandBranch.read(lessonExchange);
    }

    // Every intent's weights moved by how far its share of belief was from the truth
    private void stepUnderstanding(int[] features, int featureCount, short intentID) {

        float[] shares = dialogueUnderstandBranch.weigh(features, featureCount);
        float rate = EngineSetting.DIALOGUE_UNDERSTANDING_RATE * dialogueUnderstandBranch.toScale(featureCount);

        for (int i = 0; i < amountScratch.length; i++)
            amountScratch[i] = rate * ((i == intentID ? 1f : 0f) - shares[i]);

        dialogueManager.getUnderstandingWeights().shift(features, featureCount, amountScratch);
    }

    // Restore \\

    // The lesson's sentence read and its being rebuilt as the exchange's decided profile, for the response and
    // wording models to be taught on
    private void restore(LessonStruct lesson) {

        DialogueProfileStruct profile = lessonExchange.getDecided();
        String[] traitNames = lesson.getTraitNames();
        String[] moodNames = lesson.getMoodNames();

        read(lesson.getHeard());
        profile.clear();
        profile.setAffinity(lesson.getAffinity());
        profile.setMorality(lesson.getMorality());

        for (int i = 0; i < traitNames.length; i++) {

            if (!traitManager.hasTrait(traitNames[i]))
                continue;

            TraitHandle traitHandle = traitManager.getTraitHandleFromTraitName(traitNames[i]);

            profile.addTrait(traitHandle.getTraitID(), traitHandle.isWordless());
        }

        for (int i = 0; i < moodNames.length; i++)
            if (moodManager.hasMood(moodNames[i]))
                profile.setMoodLevel(moodManager.getMoodIDFromMoodName(moodNames[i]), lesson.getMoodLevels()[i]);

        if (relationManager.hasRelation(lesson.getRelationName()))
            profile.setRelationID(relationManager.getRelationIDFromRelationName(lesson.getRelationName()));

        if (professionManager.hasProfession(lesson.getProfessionName()))
            profile.setProfessionID(professionManager.getProfessionIDFromProfessionName(lesson.getProfessionName()));
    }

    // Response \\

    // The reply the lesson's being gave made likelier or less likely by the verdict among the replies that were
    // open to it — or the better reply made likelier, when the lesson names one that was open
    private void teachResponse(LessonStruct lesson) {

        boolean corrected = replyManager.hasReply(lesson.getBetterReplyName());

        if ((lesson.getResponse() == 0f && !corrected) || !intentManager.hasIntent(lesson.getIntentName()))
            return;

        lessonExchange.setUnderstanding(intentManager.getIntentIDFromIntentName(lesson.getIntentName()), 1f);
        dialogueRespondBranch.weigh(lessonExchange);

        int better = corrected
                ? lessonExchange.findCandidate(replyManager.getReplyIDFromReplyName(lesson.getBetterReplyName()))
                : EngineSetting.INDEX_NOT_FOUND;
        int given = replyManager.hasReply(lesson.getReplyName())
                ? lessonExchange.findCandidate(replyManager.getReplyIDFromReplyName(lesson.getReplyName()))
                : EngineSetting.INDEX_NOT_FOUND;
        int target = better != EngineSetting.INDEX_NOT_FOUND ? better : given;
        float worth = better != EngineSetting.INDEX_NOT_FOUND ? 1f : lesson.getResponse();

        if (target == EngineSetting.INDEX_NOT_FOUND || worth == 0f)
            return;

        float rate = EngineSetting.DIALOGUE_RESPONSE_RATE * worth
                * dialogueRespondBranch.toScale(lessonExchange.getResponseFeatureCount());

        for (int i = 0; i < lessonExchange.getCandidateCount(); i++)
            dialogueManager.getResponseWeights().shift(
                    lessonExchange.getResponseFeatures(),
                    lessonExchange.getResponseFeatureCount(),
                    lessonExchange.getCandidateIDs()[i],
                    rate * ((i == target ? 1f : 0f) - lessonExchange.getCandidateShares()[i]));
    }

    // Wording \\

    // Every feature of the wording the lesson recorded raised or lowered by the verdict
    private void teachWording(LessonStruct lesson) {

        if (lesson.getWording() == 0f)
            return;

        DialogueProfileStruct profile = lessonExchange.getDecided();
        String[] wordNames = lesson.getWordNames();

        keyScratch.clear();
        seedScratch.clear();
        keyScratch.addElements(0, lesson.getLineKeys());

        for (int i = 0; i < wordNames.length; i++)
            seedScratch.add(DialogueFeatureUtility.hashName(wordNames[i]));

        int featureCount = dialogueWordingBranch.raiseFeatures(
                keyScratch,
                seedScratch,
                lesson.getSaid(),
                dialogueManager.resolveMoodSeed(profile.getDominantMoodID()),
                profile.getAffinityBand(),
                DialogueFeatureUtility.hashName(lesson.getReplyName()),
                featureScratch);

        dialogueManager.getWordingWeights().shift(
                featureScratch,
                featureCount,
                0,
                EngineSetting.DIALOGUE_WORDING_RATE * lesson.getWording()
                        * dialogueWordingBranch.toScale(featureCount));
    }

    // Record \\

    // A judged exchange as the lesson it teaches: the being as it stood when it chose its reply, and everything the
    // turn heard, chose and said, by name
    LessonStruct record(
            DialogueExchangeStruct exchange,
            String verdictName,
            float understanding,
            float response,
            float wording,
            short intentID,
            short betterReplyID) {

        DialogueProfileStruct profile = exchange.getDecided();
        String[] traitNames = new String[profile.getTraitCount()];
        ObjectArrayList<String> moodNames = new ObjectArrayList<>();
        IntArrayList wordIDs = exchange.getWordIDs();
        String[] wordNames = new String[wordIDs.size()];

        for (int i = 0; i < traitNames.length; i++)
            traitNames[i] = traitManager.getTraitHandleFromTraitID(profile.getTraitID(i)).getTraitName();

        for (short moodID = EngineSetting.REGISTRY_RESERVED_ID + 1; moodID < profile.getMoodIDCount(); moodID++)
            if (profile.getMoodLevel(moodID) > 0f)
                moodNames.add(moodManager.getMoodHandleFromMoodID(moodID).getMoodName());

        float[] moodLevels = new float[moodNames.size()];

        for (int i = 0; i < moodLevels.length; i++)
            moodLevels[i] = profile.getMoodLevel(moodManager.getMoodIDFromMoodName(moodNames.get(i)));

        for (int i = 0; i < wordNames.length; i++)
            wordNames[i] = wordManager.getWordHandleFromWordID(wordIDs.getInt(i)).getWordName();

        return new LessonStruct(
                exchange.getHeard().toString(),
                intentManager.getIntentHandleFromIntentID(intentID).getIntentName(),
                traitNames,
                moodNames.toArray(new String[0]),
                moodLevels,
                profile.getAffinity(),
                profile.getMorality(),
                profile.getRelationID() == EngineSetting.REGISTRY_RESERVED_ID
                        ? EngineSetting.DIALOGUE_TEXT_NONE
                        : relationManager.getRelationHandleFromRelationID(profile.getRelationID()).getRelationName(),
                profile.getProfessionID() == EngineSetting.REGISTRY_RESERVED_ID
                        ? EngineSetting.DIALOGUE_TEXT_NONE
                        : professionManager.getProfessionHandleFromProfessionID(profile.getProfessionID())
                                .getProfessionName(),
                exchange.hasReply()
                        ? replyManager.getReplyHandleFromReplyID(exchange.getReplyID()).getReplyName()
                        : EngineSetting.DIALOGUE_TEXT_NONE,
                exchange.getSaid().toString(),
                exchange.getLineKeys().toIntArray(),
                wordNames,
                verdictName,
                understanding,
                response,
                wording,
                betterReplyID == EngineSetting.REGISTRY_RESERVED_ID
                        ? EngineSetting.DIALOGUE_TEXT_NONE
                        : replyManager.getReplyHandleFromReplyID(betterReplyID).getReplyName(),
                true);
    }
}
