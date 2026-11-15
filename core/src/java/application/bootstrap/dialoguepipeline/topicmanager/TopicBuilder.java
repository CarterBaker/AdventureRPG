package application.bootstrap.dialoguepipeline.topicmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.phrasemanager.PhraseManager;
import application.bootstrap.dialoguepipeline.relationmanager.RelationManager;
import application.bootstrap.dialoguepipeline.topic.TopicData;
import application.bootstrap.dialoguepipeline.topic.TopicHandle;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.dialoguepipeline.util.DialogueArpgUtility;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.dialoguepipeline.util.DialogueResolverStruct;
import application.bootstrap.dialoguepipeline.wordmanager.WordManager;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TopicBuilder extends BuilderPackage {

    /*
     * Parses topic ARPG files and builds TopicHandle instances. Each file
     * lists its topics under a "topics" array, each named by its "name"
     * field: the "display_name" it is shown by, the dictionary "words" that
     * raise it, under "when" who knows enough to speak of it, and the "lines"
     * they can say. Every word and phrase is resolved here, so a topic naming
     * an unknown one fails at boot. Bootstrap-only.
     */

    // Internal
    private TopicManager topicManager;
    private WordManager wordManager;
    private PhraseManager phraseManager;
    private DialogueResolverStruct resolver;

    // Base \\

    @Override
    protected void get() {

        this.topicManager = get(TopicManager.class);
        this.wordManager = get(WordManager.class);
        this.phraseManager = get(PhraseManager.class);
        this.resolver = new DialogueResolverStruct(
                wordManager,
                get(TraitManager.class),
                get(MoodManager.class),
                get(RelationManager.class),
                get(ProfessionManager.class));
    }

    // Build \\

    ObjectArrayList<TopicHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct topicArray = ArpgUtility.validateArray(rootArpg, "topics");
        ObjectArrayList<TopicHandle> topics = new ObjectArrayList<>();

        for (int i = 0; i < topicArray.size(); i++)
            topics.add(parseTopic(topicArray.get(i).getAsObject(), arpgFile));

        return topics;
    }

    // Parse \\

    private TopicHandle parseTopic(ArpgObjectStruct topicArpg, File arpgFile) {

        String topicName = ArpgUtility.validateString(topicArpg, "name");
        String owner = "Topic '" + topicName + "' in " + arpgFile.getPath();
        String[] wordNames = DialogueArpgUtility.parseStrings(topicArpg, "words");
        int[] wordIDs = new int[wordNames.length];
        int seed = DialogueFeatureUtility.hashName(topicName);

        if (wordNames.length == 0)
            throwException(owner + " needs at least one dictionary word under \"words\" to raise it.");

        for (int i = 0; i < wordIDs.length; i++)
            wordIDs[i] = wordManager.getWordIDFromWordName(wordNames[i]);

        TopicData topicData = new TopicData(
                topicName,
                topicManager.registerTopicName(topicName),
                seed,
                ArpgUtility.getString(topicArpg, "display_name", topicName),
                wordIDs,
                DialogueArpgUtility.parseCondition(topicArpg, resolver, owner),
                DialogueArpgUtility.parseLines(
                        topicArpg, seed, resolver, phraseManager::getPhraseIDFromPhraseName, owner));

        TopicHandle topicHandle = create(TopicHandle.class);
        topicHandle.constructor(topicData);

        return topicHandle;
    }
}
