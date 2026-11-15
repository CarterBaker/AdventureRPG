package application.bootstrap.dialoguepipeline.dialoguemanager;

import application.bootstrap.dialoguepipeline.intentmanager.IntentManager;
import application.bootstrap.dialoguepipeline.topicmanager.TopicManager;
import application.bootstrap.dialoguepipeline.util.DialogueExchangeStruct;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.dialoguepipeline.util.DialogueMathUtility;
import application.bootstrap.dialoguepipeline.util.DialogueTextUtility;
import application.bootstrap.dialoguepipeline.word.WordHandle;
import application.bootstrap.dialoguepipeline.wordmanager.WordManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class DialogueUnderstandBranch extends BranchPackage {

    /*
     * Works out what was meant by what was heard, for DialogueManager. read()
     * walks the sentence word by word through the dictionary and raises a
     * feature for every word it knows, every tag that word carries, every
     * word it does not know, the word the sentence opens with, each pair of
     * neighbouring words and each mark it ends on; it also takes the
     * sentence's tone from the words that carry one and the topic from the
     * first word that raises one. weigh() scores those features against every
     * intent with the understanding model and turns the scores into shares.
     * hear() does both and settles on the intent with the largest share, or
     * on the confused intent when no word was known or no intent is sure
     * enough.
     */

    // Internal
    private DialogueManager dialogueManager;
    private WordManager wordManager;
    private TopicManager topicManager;
    private IntentManager intentManager;

    // Scratch
    private StringBuilder tokenScratch;
    private float[] shareScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.tokenScratch = new StringBuilder();
    }

    @Override
    protected void get() {

        // Internal
        this.dialogueManager = get(DialogueManager.class);
        this.wordManager = get(WordManager.class);
        this.topicManager = get(TopicManager.class);
        this.intentManager = get(IntentManager.class);
    }

    @Override
    protected void awake() {

        // Scratch
        this.shareScratch = new float[intentManager.getIntentIDCount()];
    }

    // Hear \\

    void hear(DialogueExchangeStruct exchange) {

        read(exchange);

        float[] shares = weigh(exchange.getHeardFeatures(), exchange.getHeardFeatureCount());
        int likeliest = DialogueMathUtility.findHighest(shares, shares.length);
        boolean sure = exchange.getKnownWordCount() > 0
                && shares[likeliest] >= EngineSetting.DIALOGUE_CONFIDENCE_MIN;

        exchange.setUnderstanding(
                sure ? (short) likeliest : intentManager.getConfusedIntentID(),
                shares[likeliest]);
    }

    // Read \\

    // Every feature, the tone and the topic of what the exchange heard
    void read(DialogueExchangeStruct exchange) {

        CharSequence heard = exchange.getHeard();
        float toneTotal = 0f;
        int tonedCount = 0;
        int previousSeed = 0;
        int wordIndex = 0;

        exchange.addHeardFeature(DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_BIAS, 0));

        for (int start = DialogueTextUtility.findWordStart(heard, 0); start < heard.length();) {

            int end = DialogueTextUtility.findWordEnd(heard, start);
            String token = toToken(heard, start, end);
            int wordID = wordManager.findWordID(token);
            int seed = DialogueFeatureUtility.hashName(token);

            if (wordID != EngineSetting.REGISTRY_RESERVED_ID) {

                WordHandle wordHandle = wordManager.getWordHandleFromWordID(wordID);

                seed = wordHandle.getSeed();
                raiseWord(exchange, wordHandle);

                if (wordHandle.getTone() != 0f) {
                    toneTotal += wordHandle.getTone();
                    tonedCount++;
                }
            } else
                exchange.addHeardFeature(DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_UNKNOWN, seed));

            exchange.addHeardFeature(wordIndex == 0
                    ? DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_FIRST, seed)
                    : DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_PAIR, previousSeed, seed));

            previousSeed = seed;
            wordIndex++;
            start = DialogueTextUtility.findWordStart(heard, end);
        }

        raiseMarks(exchange, heard);
        exchange.setTone(tonedCount > 0 ? toneTotal / tonedCount : 0f);
        exchange.addHeardFeature(
                DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_TONE, exchange.getToneBand()));
    }

    private String toToken(CharSequence heard, int start, int end) {

        tokenScratch.setLength(0);

        for (int i = start; i < end; i++)
            tokenScratch.append(Character.toLowerCase(heard.charAt(i)));

        return tokenScratch.toString();
    }

    // A known word raises itself and each of its tags, and the first to raise a topic sets the talk's
    private void raiseWord(DialogueExchangeStruct exchange, WordHandle wordHandle) {

        int[] tagIDs = wordHandle.getTagIDs();

        exchange.countKnownWord();
        exchange.addHeardFeature(
                DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_WORD, wordHandle.getSeed()));

        for (int i = 0; i < tagIDs.length; i++)
            exchange.addHeardFeature(
                    DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_TAG, wordManager.getTagSeed(tagIDs[i])));

        if (exchange.getTopicID() != EngineSetting.REGISTRY_RESERVED_ID)
            return;

        short topicID = topicManager.getTopicIDFromWordID(wordHandle.getWordID());

        if (topicID != EngineSetting.REGISTRY_RESERVED_ID)
            exchange.setTopic(topicID, wordHandle.getWordID());
    }

    private void raiseMarks(DialogueExchangeStruct exchange, CharSequence heard) {

        for (int i = 0; i < EngineSetting.DIALOGUE_SENTENCE_MARKS.length(); i++) {

            char mark = EngineSetting.DIALOGUE_SENTENCE_MARKS.charAt(i);

            if (contains(heard, mark))
                exchange.addHeardFeature(DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_MARK, mark));
        }
    }

    private boolean contains(CharSequence text, char character) {

        for (int i = 0; i < text.length(); i++)
            if (text.charAt(i) == character)
                return true;

        return false;
    }

    // Weigh \\

    // The share of belief each intent is given for the features, indexed by intent ID — neither the reserved ID nor
    // the confused intent ever takes a share. The array is this branch's scratch, read before the next call
    float[] weigh(int[] features, int featureCount) {

        for (int intentID = 0; intentID < shareScratch.length; intentID++)
            shareScratch[intentID] = 0f;

        dialogueManager.getUnderstandingWeights().score(
                features, featureCount, toScale(featureCount), shareScratch);

        shareScratch[EngineSetting.REGISTRY_RESERVED_ID] = Float.NEGATIVE_INFINITY;
        shareScratch[intentManager.getConfusedIntentID()] = Float.NEGATIVE_INFINITY;
        DialogueMathUtility.softmax(shareScratch, shareScratch.length, 1f);

        return shareScratch;
    }

    // What every feature's weight counts for, so a long sentence weighs no more than a short one
    float toScale(int featureCount) {
        return 1f / (float) Math.sqrt(Math.max(1, featureCount));
    }
}
