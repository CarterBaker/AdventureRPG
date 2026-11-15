package application.bootstrap.dialoguepipeline.wordmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.relationmanager.RelationManager;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.dialoguepipeline.util.DialogueArpgUtility;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.dialoguepipeline.util.DialogueResolverStruct;
import application.bootstrap.dialoguepipeline.word.WordData;
import application.bootstrap.dialoguepipeline.word.WordHandle;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WordBuilder extends BuilderPackage {

    /*
     * Parses dictionary ARPG files and builds WordHandle instances. Each file
     * lists its words under a "words" array, each named by its "name" field,
     * the word as it is said: the other "forms" heard as it, the "tags" it is
     * drawn by and understood through, its "tone" from -1 for the harshest to
     * 1 for the kindest, and under "when" who may say it. A name and every
     * form is one lower-case word with no space. Bootstrap-only.
     */

    // Internal
    private WordManager wordManager;
    private DialogueResolverStruct resolver;

    // Base \\

    @Override
    protected void get() {

        this.wordManager = get(WordManager.class);
        this.resolver = new DialogueResolverStruct(
                wordManager,
                get(TraitManager.class),
                get(MoodManager.class),
                get(RelationManager.class),
                get(ProfessionManager.class));
    }

    // Build \\

    ObjectArrayList<WordHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct wordArray = ArpgUtility.validateArray(rootArpg, "words");
        ObjectArrayList<WordHandle> words = new ObjectArrayList<>();

        for (int i = 0; i < wordArray.size(); i++)
            words.add(parseWord(wordArray.get(i).getAsObject(), arpgFile));

        return words;
    }

    // Parse \\

    private WordHandle parseWord(ArpgObjectStruct wordArpg, File arpgFile) {

        String wordName = ArpgUtility.validateString(wordArpg, "name");
        String owner = "Word '" + wordName + "' in " + arpgFile.getPath();
        String[] forms = DialogueArpgUtility.parseStrings(wordArpg, "forms");
        String[] tagNames = DialogueArpgUtility.parseStrings(wordArpg, "tags");
        int[] tagIDs = new int[tagNames.length];
        float tone = ArpgUtility.getFloat(wordArpg, "tone", 0f);

        if (!DialogueArpgUtility.isWordForm(wordName))
            throwException(owner + " must be named by one lower-case word with no space.");

        for (int i = 0; i < forms.length; i++)
            if (!DialogueArpgUtility.isWordForm(forms[i]))
                throwException(owner + " lists the form \"" + forms[i]
                        + "\" — a form is one lower-case word with no space.");

        if (Math.abs(tone) > 1f)
            throwException(owner + " has a \"tone\" of " + tone + " — tone runs from -1 to 1.");

        for (int i = 0; i < tagIDs.length; i++)
            tagIDs[i] = wordManager.registerTagName(tagNames[i]);

        WordData wordData = new WordData(
                wordName,
                wordManager.registerWordName(wordName),
                DialogueFeatureUtility.hashName(wordName),
                forms,
                tagIDs,
                tone,
                DialogueArpgUtility.parseCondition(wordArpg, resolver, owner));

        WordHandle wordHandle = create(WordHandle.class);
        wordHandle.constructor(wordData);

        return wordHandle;
    }
}
