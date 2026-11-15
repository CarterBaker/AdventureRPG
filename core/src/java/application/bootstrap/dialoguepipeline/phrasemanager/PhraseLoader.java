package application.bootstrap.dialoguepipeline.phrasemanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.phrase.PhraseHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class PhraseLoader extends LoaderPackage {

    /*
     * Scans the phrase ARPG directory and loads every phrase into PhraseManager.
     * Each phrase's name is mapped to its file during scan, so a name asked for
     * before the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private PhraseManager phraseManager;
    private PhraseBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> phraseName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.PHRASE_PATH);
        this.phraseName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Phrase directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(PhraseBuilder.class);
    }

    @Override
    protected void get() {
        this.phraseManager = get(PhraseManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct phraseArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "phrases");

        for (int i = 0; i < phraseArray.size(); i++) {
            ArpgObjectStruct phraseArpg = phraseArray.get(i).getAsObject();
            phraseName2File.put(ArpgUtility.validateString(phraseArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<PhraseHandle> phrases = internalBuilder.build(file);

        for (int i = 0; i < phrases.size(); i++)
            phraseManager.addPhraseHandle(phrases.get(i));
    }

    // On-Demand \\

    void request(String phraseName) {

        File file = phraseName2File.get(phraseName);

        if (file == null)
            throwException("On-demand phrase load failed — no file found for: \"" + phraseName + "\"");

        request(file);
    }
}
