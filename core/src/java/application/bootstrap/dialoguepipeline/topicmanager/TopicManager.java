package application.bootstrap.dialoguepipeline.topicmanager;

import application.bootstrap.dialoguepipeline.topic.TopicHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TopicManager extends ManagerPackage {

    /*
     * Owns the topic palette and the index of the dictionary words that raise each
     * topic. IDs are assigned in registration order; a name declared twice, or a
     * word raising two topics, is rejected. Every topic is resolved in awake().
     */

    // Palette
    private Object2IntOpenHashMap<String> topicName2TopicID;
    private ObjectArrayList<TopicHandle> topicID2TopicHandle;

    // Words
    private Int2ShortOpenHashMap wordID2TopicID;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.topicName2TopicID = RegistryUtility.createNameIndex();
        this.topicID2TopicHandle = RegistryUtility.createPalette();
        create(TopicLoader.class);

        // Words
        this.wordID2TopicID = new Int2ShortOpenHashMap();
        this.wordID2TopicID.defaultReturnValue(EngineSetting.REGISTRY_RESERVED_ID);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
    }

    // Management \\

    short registerTopicName(String topicName) {
        return (short) RegistryUtility.registerID(
                topicName2TopicID, topicID2TopicHandle, topicName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addTopicHandle(TopicHandle topicHandle) {

        short topicID = topicHandle.getTopicID();

        if (topicID2TopicHandle.get(topicID) != null)
            throwException("Duplicate topic name: '" + topicHandle.getTopicName()
                    + "' is declared more than once — every topic name must be unique");

        topicID2TopicHandle.set(topicID, topicHandle);

        int[] wordIDs = topicHandle.getWordIDs();

        for (int i = 0; i < wordIDs.length; i++)
            if (wordID2TopicID.putIfAbsent(wordIDs[i], topicID) != EngineSetting.REGISTRY_RESERVED_ID)
                throwException("Topic '" + topicHandle.getTopicName()
                        + "' is raised by a word that already raises topic '"
                        + getTopicHandleFromTopicID(wordID2TopicID.get(wordIDs[i])).getTopicName()
                        + "' — a word raises one topic");
    }

    // Accessible \\

    public boolean hasTopic(String topicName) {
        return RegistryUtility.getHandle(topicName2TopicID, topicID2TopicHandle, topicName) != null;
    }

    public short getTopicIDFromTopicName(String topicName) {

        if (!hasTopic(topicName) && internalLoader != null)
            ((TopicLoader) internalLoader).request(topicName);

        if (!hasTopic(topicName))
            throwException("Topic not found after load: \"" + topicName + "\"");

        return (short) topicName2TopicID.getInt(topicName);
    }

    public TopicHandle getTopicHandleFromTopicID(short topicID) {

        TopicHandle handle = RegistryUtility.getHandle(topicID2TopicHandle, topicID);

        if (handle == null)
            throwException("Topic ID not found: " + topicID);

        return handle;
    }

    public TopicHandle getTopicHandleFromTopicName(String topicName) {
        return getTopicHandleFromTopicID(getTopicIDFromTopicName(topicName));
    }

    // Every topic ID lies below this — the length of an array indexed by topic ID
    public int getTopicIDCount() {
        return topicID2TopicHandle.size();
    }

    // Words \\

    // The topic a dictionary word raises, the reserved ID when it raises none
    public short getTopicIDFromWordID(int wordID) {
        return wordID2TopicID.get(wordID);
    }
}
