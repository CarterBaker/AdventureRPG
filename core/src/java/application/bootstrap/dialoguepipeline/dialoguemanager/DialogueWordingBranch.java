package application.bootstrap.dialoguepipeline.dialoguemanager;

import application.bootstrap.dialoguepipeline.phrasemanager.PhraseManager;
import application.bootstrap.dialoguepipeline.replymanager.ReplyManager;
import application.bootstrap.dialoguepipeline.topicmanager.TopicManager;
import application.bootstrap.dialoguepipeline.util.DialogueExchangeStruct;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.dialoguepipeline.util.DialogueLineStruct;
import application.bootstrap.dialoguepipeline.util.DialogueMathUtility;
import application.bootstrap.dialoguepipeline.util.DialogueProfileStruct;
import application.bootstrap.dialoguepipeline.util.DialogueTextUtility;
import application.bootstrap.dialoguepipeline.util.DialogueValue;
import application.bootstrap.dialoguepipeline.util.DialogueWeightStruct;
import application.bootstrap.dialoguepipeline.word.WordHandle;
import application.bootstrap.dialoguepipeline.wordmanager.WordManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class DialogueWordingBranch extends BranchPackage {

    /*
     * Puts a chosen reply into words, for DialogueManager. A reply is worded
     * several times over, and each wording is built from the top down: one of
     * the reply's lines the being may say is drawn, then every phrase it
     * calls for is drawn the same way, every dictionary draw is filled by a
     * word carrying its tags that the being may say, and every value is taken
     * from the talk. Lines and words are drawn by their weight times what the
     * wording model has learned of them, so wording that was judged badly is
     * built less often. Each finished wording is polished and scored by the
     * model on the lines it used, the words it drew and every pair of
     * neighbouring words it ended up with; a wording that repeats what was
     * said last is marked down, and one is drawn by those scores.
     * raiseFeatures() names a wording to the model the same way for
     * LearnBranch, from a lesson's record of it.
     */

    // Internal
    private DialogueManager dialogueManager;
    private ReplyManager replyManager;
    private PhraseManager phraseManager;
    private TopicManager topicManager;
    private WordManager wordManager;

    // Candidates
    private StringBuilder[] candidateTexts;
    private IntArrayList[] candidateLineKeys;
    private IntArrayList[] candidateWordIDs;
    private float[] candidateShares;

    // Scratch
    private IntArrayList choiceScratch;
    private FloatArrayList weightScratch;
    private IntArrayList seedScratch;
    private int[] featureScratch;

    // Base \\

    @Override
    protected void create() {

        // Candidates
        this.candidateTexts = new StringBuilder[EngineSetting.DIALOGUE_WORDING_CANDIDATES];
        this.candidateLineKeys = new IntArrayList[EngineSetting.DIALOGUE_WORDING_CANDIDATES];
        this.candidateWordIDs = new IntArrayList[EngineSetting.DIALOGUE_WORDING_CANDIDATES];
        this.candidateShares = new float[EngineSetting.DIALOGUE_WORDING_CANDIDATES];

        for (int i = 0; i < EngineSetting.DIALOGUE_WORDING_CANDIDATES; i++) {
            candidateTexts[i] = new StringBuilder();
            candidateLineKeys[i] = new IntArrayList();
            candidateWordIDs[i] = new IntArrayList();
        }

        // Scratch
        this.choiceScratch = new IntArrayList();
        this.weightScratch = new FloatArrayList();
        this.seedScratch = new IntArrayList();
        this.featureScratch = new int[EngineSetting.DIALOGUE_WORDING_FEATURE_CAPACITY];
    }

    @Override
    protected void get() {

        // Internal
        this.dialogueManager = get(DialogueManager.class);
        this.replyManager = get(ReplyManager.class);
        this.phraseManager = get(PhraseManager.class);
        this.topicManager = get(TopicManager.class);
        this.wordManager = get(WordManager.class);
    }

    // Word \\

    void word(DialogueExchangeStruct exchange) {

        StringBuilder said = exchange.getSaid();

        said.setLength(0);

        if (!exchange.hasReply())
            return;

        DialogueLineStruct[] lines = replyManager.getReplyHandleFromReplyID(exchange.getReplyID()).getLines();

        for (int i = 0; i < EngineSetting.DIALOGUE_WORDING_CANDIDATES; i++)
            candidateShares[i] = compose(exchange, lines, i);

        DialogueMathUtility.softmax(
                candidateShares, EngineSetting.DIALOGUE_WORDING_CANDIDATES, EngineSetting.DIALOGUE_WORDING_TEMPERATURE);

        int chosen = DialogueMathUtility.pick(
                candidateShares, EngineSetting.DIALOGUE_WORDING_CANDIDATES, exchange.roll());

        said.append(candidateTexts[chosen]);
        exchange.getLineKeys().addAll(candidateLineKeys[chosen]);
        exchange.getWordIDs().addAll(candidateWordIDs[chosen]);
        exchange.setSaidHash(DialogueFeatureUtility.hashName(said));
    }

    // One wording built, polished and scored
    private float compose(DialogueExchangeStruct exchange, DialogueLineStruct[] lines, int candidate) {

        StringBuilder text = candidateTexts[candidate];

        text.setLength(0);
        candidateLineKeys[candidate].clear();
        candidateWordIDs[candidate].clear();
        seedScratch.clear();

        expand(exchange, lines, candidate, 0);
        DialogueTextUtility.polish(text);

        for (int i = 0; i < candidateWordIDs[candidate].size(); i++)
            seedScratch.add(wordManager.getWordHandleFromWordID(candidateWordIDs[candidate].getInt(i)).getSeed());

        int featureCount = raiseFeatures(
                candidateLineKeys[candidate],
                seedScratch,
                text,
                dialogueManager.resolveMoodSeed(exchange.getDecided().getDominantMoodID()),
                exchange.getDecided().getAffinityBand(),
                replyManager.getReplyHandleFromReplyID(exchange.getReplyID()).getSeed(),
                featureScratch);
        float score = dialogueManager.getWordingWeights().score(featureScratch, featureCount, 0)
                * toScale(featureCount);

        return DialogueFeatureUtility.hashName(text) == exchange.getSaidHash()
                ? score - EngineSetting.DIALOGUE_REPEAT_PENALTY
                : score;
    }

    // Expand \\

    // One of the lines drawn and written out, the phrases it calls for expanded no deeper than the limit
    private void expand(DialogueExchangeStruct exchange, DialogueLineStruct[] lines, int candidate, int depth) {

        DialogueLineStruct line = drawLine(exchange, lines);

        if (line == null)
            return;

        StringBuilder text = candidateTexts[candidate];

        candidateLineKeys[candidate].add(line.getKey());

        for (int i = 0; i < line.getSegmentCount(); i++)
            switch (line.getKind(i)) {
                case LITERAL -> text.append(line.getText(i));
                case PHRASE -> expandDeeper(
                        exchange,
                        phraseManager.getPhraseHandleFromPhraseID((short) line.getReference(i)).getLines(),
                        candidate,
                        depth);
                case TOPIC -> expandDeeper(
                        exchange,
                        topicManager.getTopicHandleFromTopicID(exchange.getTopicID()).getLines(),
                        candidate,
                        depth);
                case WORD -> appendWord(exchange, line.getTagIDs(i), candidate);
                case VALUE -> appendValue(exchange, DialogueValue.VALUES[line.getReference(i)], text);
            }
    }

    private void expandDeeper(DialogueExchangeStruct exchange, DialogueLineStruct[] lines, int candidate, int depth) {

        if (depth < EngineSetting.DIALOGUE_EXPANSION_DEPTH_MAX)
            expand(exchange, lines, candidate, depth + 1);
    }

    // One line the being may say, drawn by its weight times what the model has learned of it — none when it may say
    // none of them
    private DialogueLineStruct drawLine(DialogueExchangeStruct exchange, DialogueLineStruct[] lines) {

        DialogueProfileStruct profile = exchange.getDecided();
        DialogueWeightStruct weights = dialogueManager.getWordingWeights();
        int moodSeed = dialogueManager.resolveMoodSeed(profile.getDominantMoodID());
        int band = profile.getAffinityBand();

        choiceScratch.clear();
        weightScratch.clear();

        for (int i = 0; i < lines.length; i++) {

            if (!lines[i].matches(profile, exchange.getTopicState()))
                continue;

            int key = lines[i].getKey();
            float learned = weights.score(DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_LINE, key), 0)
                    + weights.score(DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_LINE, key, moodSeed), 0)
                    + weights.score(DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_LINE, key, band), 0);

            choiceScratch.add(i);
            weightScratch.add(lines[i].getWeight() * (float) Math.exp(learned));
        }

        return choiceScratch.isEmpty() ? null : lines[choiceScratch.getInt(drawChoice(exchange))];
    }

    // One dictionary word carrying every tag that the being may say, drawn by what the model has learned of it
    private void appendWord(DialogueExchangeStruct exchange, int[] tagIDs, int candidate) {

        IntArrayList wordIDs = wordManager.getWordIDsFromTagID(tagIDs[0]);
        DialogueWeightStruct weights = dialogueManager.getWordingWeights();

        choiceScratch.clear();
        weightScratch.clear();

        for (int i = 0; i < wordIDs.size(); i++) {

            WordHandle wordHandle = wordManager.getWordHandleFromWordID(wordIDs.getInt(i));

            if (!carriesTags(wordHandle, tagIDs)
                    || !wordHandle.getCondition().matches(exchange.getDecided(), exchange.getTopicState()))
                continue;

            choiceScratch.add(wordIDs.getInt(i));
            weightScratch.add((float) Math.exp(weights.score(
                    DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_DRAW, wordHandle.getSeed()), 0)));
        }

        if (choiceScratch.isEmpty())
            return;

        int wordID = choiceScratch.getInt(drawChoice(exchange));

        candidateWordIDs[candidate].add(wordID);
        candidateTexts[candidate].append(wordManager.getWordHandleFromWordID(wordID).getWordName());
    }

    private boolean carriesTags(WordHandle wordHandle, int[] tagIDs) {

        int[] carried = wordHandle.getTagIDs();

        for (int i = 1; i < tagIDs.length; i++) {

            boolean found = false;

            for (int j = 0; j < carried.length && !found; j++)
                found = carried[j] == tagIDs[i];

            if (!found)
                return false;
        }

        return true;
    }

    // The place in the scratch one roll lands on, each choice as wide as its weight
    private int drawChoice(DialogueExchangeStruct exchange) {

        float total = 0f;

        for (int i = 0; i < weightScratch.size(); i++)
            total += weightScratch.getFloat(i);

        float roll = exchange.roll() * total;

        for (int i = 0; i < weightScratch.size() - 1; i++) {

            roll -= weightScratch.getFloat(i);

            if (roll < 0f)
                return i;
        }

        return weightScratch.size() - 1;
    }

    private void appendValue(DialogueExchangeStruct exchange, DialogueValue value, StringBuilder text) {

        DialogueProfileStruct profile = exchange.getDecided();

        switch (value) {
            case PLAYER -> DialogueTextUtility.appendCapitalised(text, profile.getSpeakerName());
            case NAME -> DialogueTextUtility.appendCapitalised(text, profile.getName());
            case TITLE -> text.append(profile.getTitle());
            case SUBJECT -> text.append(
                    wordManager.getWordHandleFromWordID(exchange.getSubjectWordID()).getWordName());
        }
    }

    // Features \\

    // What the wording model weighs a wording by: each line alone, by the mood it was said in and by the regard it
    // was said with, each dictionary word alone and within its reply, and each pair of neighbouring words of the
    // finished text. Answers how many features were raised
    int raiseFeatures(
            IntArrayList lineKeys,
            IntArrayList wordSeeds,
            CharSequence text,
            int moodSeed,
            int affinityBand,
            int replySeed,
            int[] features) {

        int count = 0;

        for (int i = 0; i < lineKeys.size() && count + 3 <= features.length; i++) {

            int key = lineKeys.getInt(i);

            features[count++] = DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_LINE, key);
            features[count++] = DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_LINE, key, moodSeed);
            features[count++] = DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_LINE, key, affinityBand);
        }

        for (int i = 0; i < wordSeeds.size() && count + 2 <= features.length; i++) {

            int seed = wordSeeds.getInt(i);

            features[count++] = DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_DRAW, seed);
            features[count++] = DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_DRAW, seed, replySeed);
        }

        int previous = 0;
        int start = DialogueTextUtility.findWordStart(text, 0);

        while (start < text.length() && count < features.length) {

            int end = DialogueTextUtility.findWordEnd(text, start);
            int hash = DialogueTextUtility.hashWord(text, start, end);

            features[count++] = DialogueFeatureUtility.mix(EngineSetting.DIALOGUE_SALT_BIGRAM, previous, hash);
            previous = hash;
            start = DialogueTextUtility.findWordStart(text, end);
        }

        return count;
    }

    float toScale(int featureCount) {
        return 1f / (float) Math.sqrt(Math.max(1, featureCount));
    }
}
