package application.bootstrap.dialoguepipeline.intentmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.intent.IntentData;
import application.bootstrap.dialoguepipeline.intent.IntentHandle;
import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.replymanager.ReplyManager;
import application.bootstrap.dialoguepipeline.util.DialogueArpgUtility;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.progressionpipeline.util.ProgressionArpgUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class IntentBuilder extends BuilderPackage {

    /*
     * Parses intent ARPG files and builds IntentHandle instances. Each file
     * lists its intents under an "intents" array, each named by its "name"
     * field: its "description", the "examples" of how it is said that the
     * understanding model first learns it from, the "affinity" being told it
     * moves the listener's regard for the speaker by, the "moods" it shifts
     * in the listener, and the "replies" it can be answered with. Every
     * intent needs a reply, and every one but the intent the engine falls
     * back on when it cannot tell what was meant needs an example.
     * Bootstrap-only.
     */

    // Internal
    private IntentManager intentManager;
    private ReplyManager replyManager;
    private MoodManager moodManager;

    // Base \\

    @Override
    protected void get() {
        this.intentManager = get(IntentManager.class);
        this.replyManager = get(ReplyManager.class);
        this.moodManager = get(MoodManager.class);
    }

    // Build \\

    ObjectArrayList<IntentHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct intentArray = ArpgUtility.validateArray(rootArpg, "intents");
        ObjectArrayList<IntentHandle> intents = new ObjectArrayList<>();

        for (int i = 0; i < intentArray.size(); i++)
            intents.add(parseIntent(intentArray.get(i).getAsObject(), arpgFile));

        return intents;
    }

    // Parse \\

    private IntentHandle parseIntent(ArpgObjectStruct intentArpg, File arpgFile) {

        String intentName = ArpgUtility.validateString(intentArpg, "name");
        String owner = "Intent '" + intentName + "' in " + arpgFile.getPath();
        String[] examples = DialogueArpgUtility.parseStrings(intentArpg, "examples");
        short[] replyIDs = ProgressionArpgUtility.parseNames(
                intentArpg, "replies", replyManager::getReplyIDFromReplyName);
        float affinity = ArpgUtility.getFloat(intentArpg, "affinity", 0f);

        if (examples.length == 0 && !intentName.equals(EngineSetting.DIALOGUE_INTENT_CONFUSED))
            throwException(owner + " needs at least one example under \"examples\" to be learned from.");

        if (replyIDs.length == 0)
            throwException(owner + " needs at least one reply under \"replies\".");

        if (Math.abs(affinity) > EngineSetting.AFFINITY_MAX)
            throwException(owner + " moves affinity by " + affinity + " — regard runs from -"
                    + EngineSetting.AFFINITY_MAX + " to " + EngineSetting.AFFINITY_MAX + ".");

        IntentData intentData = new IntentData(
                intentName,
                intentManager.registerIntentName(intentName),
                DialogueFeatureUtility.hashName(intentName),
                ArpgUtility.getString(intentArpg, "description", EngineSetting.DIALOGUE_TEXT_NONE),
                examples,
                affinity,
                ProgressionArpgUtility.parseValueMap(intentArpg, "moods", moodManager::getMoodIDFromMoodName, owner),
                replyIDs);

        IntentHandle intentHandle = create(IntentHandle.class);
        intentHandle.constructor(intentData);

        return intentHandle;
    }
}
