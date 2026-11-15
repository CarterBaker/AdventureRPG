package application.bootstrap.dialoguepipeline;

import application.bootstrap.dialoguepipeline.dialoguemanager.DialogueManager;
import application.bootstrap.dialoguepipeline.intentmanager.IntentManager;
import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.phrasemanager.PhraseManager;
import application.bootstrap.dialoguepipeline.relationmanager.RelationManager;
import application.bootstrap.dialoguepipeline.replymanager.ReplyManager;
import application.bootstrap.dialoguepipeline.scenariomanager.ScenarioManager;
import application.bootstrap.dialoguepipeline.topicmanager.TopicManager;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.dialoguepipeline.verdictmanager.VerdictManager;
import application.bootstrap.dialoguepipeline.wordmanager.WordManager;
import engine.root.PipelinePackage;

public class DialoguePipeline extends PipelinePackage {

    /*
     * Registers the dialogue pipeline in dependency order: the moods traits
     * lean toward, the traits and relations every condition names, the
     * dictionary whose words carry conditions, the phrases, topics and
     * replies whose lines draw on all of those, the intents that name their
     * replies, DialogueManager, which builds the three models from them and
     * holds every talk, and last the verdicts and scenarios the trainer
     * judges and practises with. The pipeline is created after the
     * progression pipeline, whose professions conditions name, and ahead of
     * the entity pipeline, whose NPC definitions name traits.
     */

    @Override
    protected void create() {
        create(MoodManager.class);
        create(TraitManager.class);
        create(RelationManager.class);
        create(WordManager.class);
        create(PhraseManager.class);
        create(TopicManager.class);
        create(ReplyManager.class);
        create(IntentManager.class);
        create(DialogueManager.class);
        create(VerdictManager.class);
        create(ScenarioManager.class);
    }
}
