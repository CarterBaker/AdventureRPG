package application.bootstrap.dialoguepipeline.wordmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.word.WordHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WordLoader extends LoaderPackage {

    /*
     * Scans the word ARPG directory and loads every word into WordManager. Each
     * word's name is mapped to its file during scan, so a name asked for before
     * the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private WordManager wordManager;
    private WordBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> wordName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.WORD_PATH);
        this.wordName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Word directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(WordBuilder.class);
    }

    @Override
    protected void get() {
        this.wordManager = get(WordManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct wordArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "words");

        for (int i = 0; i < wordArray.size(); i++) {
            ArpgObjectStruct wordArpg = wordArray.get(i).getAsObject();
            wordName2File.put(ArpgUtility.validateString(wordArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<WordHandle> words = internalBuilder.build(file);

        for (int i = 0; i < words.size(); i++)
            wordManager.addWordHandle(words.get(i));
    }

    // On-Demand \\

    void request(String wordName) {

        File file = wordName2File.get(wordName);

        if (file == null)
            throwException("On-demand word load failed — no file found for: \"" + wordName + "\"");

        request(file);
    }
}
