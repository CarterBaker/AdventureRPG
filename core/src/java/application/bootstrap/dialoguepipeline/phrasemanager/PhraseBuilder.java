package application.bootstrap.dialoguepipeline.phrasemanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.phrase.PhraseData;
import application.bootstrap.dialoguepipeline.phrase.PhraseHandle;
import application.bootstrap.dialoguepipeline.relationmanager.RelationManager;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.dialoguepipeline.util.DialogueArpgUtility;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.dialoguepipeline.util.DialogueResolverStruct;
import application.bootstrap.dialoguepipeline.wordmanager.WordManager;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class PhraseBuilder extends BuilderPackage {

    /*
     * Parses phrase ARPG files and builds PhraseHandle instances. Each file
     * lists its phrases under a "phrases" array, each named by its "name"
     * field and carrying the "lines" it can be said as. A phrase its lines
     * call for is registered by name as it is read, so phrases may call for
     * each other and for themselves. The name topic is reserved for the lines
     * of whatever topic was raised. Bootstrap-only.
     */

    // Internal
    private PhraseManager phraseManager;
    private DialogueResolverStruct resolver;

    // Base \\

    @Override
    protected void get() {

        this.phraseManager = get(PhraseManager.class);
        this.resolver = new DialogueResolverStruct(
                get(WordManager.class),
                get(TraitManager.class),
                get(MoodManager.class),
                get(RelationManager.class),
                get(ProfessionManager.class));
    }

    // Build \\

    ObjectArrayList<PhraseHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct phraseArray = ArpgUtility.validateArray(rootArpg, "phrases");
        ObjectArrayList<PhraseHandle> phrases = new ObjectArrayList<>();

        for (int i = 0; i < phraseArray.size(); i++)
            phrases.add(parsePhrase(phraseArray.get(i).getAsObject(), arpgFile));

        return phrases;
    }

    // Parse \\

    private PhraseHandle parsePhrase(ArpgObjectStruct phraseArpg, File arpgFile) {

        String phraseName = ArpgUtility.validateString(phraseArpg, "name");
        String owner = "Phrase '" + phraseName + "' in " + arpgFile.getPath();
        int seed = DialogueFeatureUtility.hashName(phraseName);

        if (phraseName.equals(EngineSetting.DIALOGUE_PHRASE_TOPIC))
            throwException(owner + " takes the reserved name \"" + EngineSetting.DIALOGUE_PHRASE_TOPIC
                    + "\", which expands the lines of the topic raised.");

        PhraseData phraseData = new PhraseData(
                phraseName,
                phraseManager.registerPhraseName(phraseName),
                seed,
                DialogueArpgUtility.parseLines(phraseArpg, seed, resolver, phraseManager::registerPhraseName, owner));

        PhraseHandle phraseHandle = create(PhraseHandle.class);
        phraseHandle.constructor(phraseData);

        return phraseHandle;
    }
}
