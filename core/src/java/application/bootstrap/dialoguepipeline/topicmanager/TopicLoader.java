package application.bootstrap.dialoguepipeline.topicmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.topic.TopicHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TopicLoader extends LoaderPackage {

    /*
     * Scans the topic ARPG directory and loads every topic into TopicManager. Each
     * topic's name is mapped to its file during scan, so a name asked for before
     * the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private TopicManager topicManager;
    private TopicBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> topicName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.TOPIC_PATH);
        this.topicName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Topic directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(TopicBuilder.class);
    }

    @Override
    protected void get() {
        this.topicManager = get(TopicManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct topicArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "topics");

        for (int i = 0; i < topicArray.size(); i++) {
            ArpgObjectStruct topicArpg = topicArray.get(i).getAsObject();
            topicName2File.put(ArpgUtility.validateString(topicArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<TopicHandle> topics = internalBuilder.build(file);

        for (int i = 0; i < topics.size(); i++)
            topicManager.addTopicHandle(topics.get(i));
    }

    // On-Demand \\

    void request(String topicName) {

        File file = topicName2File.get(topicName);

        if (file == null)
            throwException("On-demand topic load failed — no file found for: \"" + topicName + "\"");

        request(file);
    }
}
