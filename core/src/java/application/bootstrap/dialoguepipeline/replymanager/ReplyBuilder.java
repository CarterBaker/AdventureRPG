package application.bootstrap.dialoguepipeline.replymanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.phrasemanager.PhraseManager;
import application.bootstrap.dialoguepipeline.relationmanager.RelationManager;
import application.bootstrap.dialoguepipeline.reply.ReplyData;
import application.bootstrap.dialoguepipeline.reply.ReplyHandle;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.dialoguepipeline.util.DialogueAction;
import application.bootstrap.dialoguepipeline.util.DialogueArpgUtility;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.dialoguepipeline.util.DialogueLeanStruct;
import application.bootstrap.dialoguepipeline.util.DialogueResolverStruct;
import application.bootstrap.dialoguepipeline.wordmanager.WordManager;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.util.ProgressionArpgUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ReplyBuilder extends BuilderPackage {

    /*
     * Parses reply ARPG files and builds ReplyHandle instances. Each file
     * lists its replies under a "replies" array, each named by its "name"
     * field: its "description", under "when" who may give it, under "leans"
     * who it comes naturally to — a "weight" of its own, one for each trait
     * under "traits", each mood under "moods" and each relation under
     * "relations", and one each for "affinity", "morality" and "tone" — the
     * "lines" it can be worded as, the "affinity" giving it moves the
     * replier's regard for the speaker by, the "moods" it shifts in the
     * replier, and the "action" it takes in the world. Bootstrap-only.
     */

    // Internal
    private ReplyManager replyManager;
    private PhraseManager phraseManager;
    private DialogueResolverStruct resolver;

    // Base \\

    @Override
    protected void get() {

        this.replyManager = get(ReplyManager.class);
        this.phraseManager = get(PhraseManager.class);
        this.resolver = new DialogueResolverStruct(
                get(WordManager.class),
                get(TraitManager.class),
                get(MoodManager.class),
                get(RelationManager.class),
                get(ProfessionManager.class));
    }

    // Build \\

    ObjectArrayList<ReplyHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct replyArray = ArpgUtility.validateArray(rootArpg, "replies");
        ObjectArrayList<ReplyHandle> replies = new ObjectArrayList<>();

        for (int i = 0; i < replyArray.size(); i++)
            replies.add(parseReply(replyArray.get(i).getAsObject(), arpgFile));

        return replies;
    }

    // Parse \\

    private ReplyHandle parseReply(ArpgObjectStruct replyArpg, File arpgFile) {

        String replyName = ArpgUtility.validateString(replyArpg, "name");
        String owner = "Reply '" + replyName + "' in " + arpgFile.getPath();
        int seed = DialogueFeatureUtility.hashName(replyName);
        float affinity = ArpgUtility.getFloat(replyArpg, "affinity", 0f);

        if (Math.abs(affinity) > EngineSetting.AFFINITY_MAX)
            throwException(owner + " moves affinity by " + affinity + " — regard runs from -"
                    + EngineSetting.AFFINITY_MAX + " to " + EngineSetting.AFFINITY_MAX + ".");

        ReplyData replyData = new ReplyData(
                replyName,
                replyManager.registerReplyName(replyName),
                seed,
                ArpgUtility.getString(replyArpg, "description", EngineSetting.DIALOGUE_TEXT_NONE),
                DialogueArpgUtility.parseCondition(replyArpg, resolver, owner),
                parseLean(replyArpg, owner),
                DialogueArpgUtility.parseLines(
                        replyArpg, seed, resolver, phraseManager::getPhraseIDFromPhraseName, owner),
                affinity,
                ProgressionArpgUtility.parseValueMap(replyArpg, "moods", resolver::resolveMood, owner),
                ArpgUtility.getEnum(replyArpg, "action", DialogueAction.class, DialogueAction.NONE));

        ReplyHandle replyHandle = create(ReplyHandle.class);
        replyHandle.constructor(replyData);

        return replyHandle;
    }

    private DialogueLeanStruct parseLean(ArpgObjectStruct replyArpg, String owner) {

        if (!ArpgUtility.hasObject(replyArpg, "leans"))
            return DialogueLeanStruct.NONE;

        ArpgObjectStruct leanArpg = replyArpg.getAsObject("leans");

        return new DialogueLeanStruct(
                ArpgUtility.getFloat(leanArpg, "weight", 0f),
                ProgressionArpgUtility.parseValueMap(leanArpg, "traits", resolver::resolveTrait, owner),
                ProgressionArpgUtility.parseValueMap(leanArpg, "moods", resolver::resolveMood, owner),
                ProgressionArpgUtility.parseValueMap(leanArpg, "relations", resolver::resolveRelation, owner),
                ArpgUtility.getFloat(leanArpg, "affinity", 0f),
                ArpgUtility.getFloat(leanArpg, "morality", 0f),
                ArpgUtility.getFloat(leanArpg, "tone", 0f));
    }
}
