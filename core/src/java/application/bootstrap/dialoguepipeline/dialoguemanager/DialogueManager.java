package application.bootstrap.dialoguepipeline.dialoguemanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.intentmanager.IntentManager;
import application.bootstrap.dialoguepipeline.lesson.LessonStruct;
import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.replymanager.ReplyManager;
import application.bootstrap.dialoguepipeline.util.DialogueExchangeStruct;
import application.bootstrap.dialoguepipeline.util.DialogueWeightStruct;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DialogueManager extends ManagerPackage {

    /*
     * The one way a being is talked to. converse() takes one exchange through
     * a turn: DialogueUnderstandBranch works out what its heard sentence
     * meant, DialogueRespondBranch lets that land on the being and chooses
     * what it answers with, and DialogueWordingBranch puts the answer into
     * words. Each is led by a small learned model, a table of weights over
     * hashed features, held here. Nothing of a model is ever stored: every
     * boot DialogueLearnBranch builds all three from the content and the
     * lessons, the judged turns kept under the lesson directory, so the
     * models always match the dictionary and the replies as they stand.
     * judge() records a verdict on the turn an exchange just held as a
     * lesson and teaches it at once, saveLessons() writes the lessons the
     * trainer recorded back to its file, and retrain() rebuilds the models
     * from everything. A talk with a living being goes through the
     * converse() that names it and whoever speaks: DialogueBeingBranch copies
     * the being into the exchange, and what the turn moved in it is written
     * back. disposeBeing() gives a being its traits, name and resting moods
     * as it enters the world, and relateBeings() binds two of them. Turns are
     * answered on the main thread; a turn costs microseconds and allocates
     * only the words it reads.
     */

    // Internal
    private IntentManager intentManager;
    private ReplyManager replyManager;
    private MoodManager moodManager;

    // Branches
    private DialogueUnderstandBranch dialogueUnderstandBranch;
    private DialogueRespondBranch dialogueRespondBranch;
    private DialogueWordingBranch dialogueWordingBranch;
    private DialogueLearnBranch dialogueLearnBranch;
    private DialogueBeingBranch dialogueBeingBranch;

    // Models
    private DialogueWeightStruct understandingWeights;
    private DialogueWeightStruct responseWeights;
    private DialogueWeightStruct wordingWeights;

    // Lessons
    private ObjectArrayList<LessonStruct> lessons;

    // Base \\

    @Override
    protected void create() {

        // Lessons
        this.lessons = new ObjectArrayList<>();
        create(DialogueLoader.class);

        // Branches
        this.dialogueUnderstandBranch = create(DialogueUnderstandBranch.class);
        this.dialogueRespondBranch = create(DialogueRespondBranch.class);
        this.dialogueWordingBranch = create(DialogueWordingBranch.class);
        this.dialogueLearnBranch = create(DialogueLearnBranch.class);
        this.dialogueBeingBranch = create(DialogueBeingBranch.class);
    }

    @Override
    protected void get() {

        // Internal
        this.intentManager = get(IntentManager.class);
        this.replyManager = get(ReplyManager.class);
        this.moodManager = get(MoodManager.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        // Models
        this.understandingWeights = new DialogueWeightStruct(
                EngineSetting.DIALOGUE_UNDERSTANDING_BUCKETS, intentManager.getIntentIDCount());
        this.responseWeights = new DialogueWeightStruct(
                EngineSetting.DIALOGUE_RESPONSE_BUCKETS, replyManager.getReplyIDCount());
        this.wordingWeights = new DialogueWeightStruct(EngineSetting.DIALOGUE_WORDING_BUCKETS, 1);
    }

    // Management \\

    void addLesson(LessonStruct lesson) {
        lessons.add(lesson);
    }

    // Talk \\

    // An exchange sized for the content as it loaded, for whoever holds a talk to keep
    public DialogueExchangeStruct createExchange() {
        return new DialogueExchangeStruct(moodManager.getMoodIDCount(), replyManager.getReplyIDCount());
    }

    // One turn: what the exchange heard is understood, answered and worded, and its profile is left as the turn
    // moved it
    public void converse(DialogueExchangeStruct exchange) {

        exchange.beginTurn();
        dialogueUnderstandBranch.hear(exchange);
        dialogueRespondBranch.respond(exchange);
        dialogueWordingBranch.word(exchange);
    }

    // One turn with a living being: the listener is read as it stands toward the speaker, the turn is held, and
    // the regard and moods it moved are written back to the listener
    public void converse(DialogueExchangeStruct exchange, EntityInstance listener, EntityInstance speaker) {
        dialogueBeingBranch.read(exchange, listener, speaker);
        converse(exchange);
        dialogueBeingBranch.write(exchange, listener, speaker);
    }

    // Beings \\

    // A being given its nature as it enters the world: the traits it always has, as many more drawn from the pool
    // by its identity, a name drawn the same way and its moods at rest
    public void disposeBeing(
            EntityInstance entity,
            short[] traitIDs,
            short[] traitPoolIDs,
            int traitDraws,
            long identity) {
        dialogueBeingBranch.dispose(entity, traitIDs, traitPoolIDs, traitDraws, identity);
    }

    // The name a being goes by in talk, a player's being its character's
    public void nameBeing(EntityInstance entity, String name) {
        entity.getDispositionHandle().setName(name);
    }

    // The other becomes the relation to the being, and the being the relation's inverse to the other
    public void relateBeings(EntityInstance entity, EntityInstance other, short relationID) {
        dialogueBeingBranch.relate(entity, other, relationID);
    }

    // Lessons \\

    // A verdict on the turn the exchange just held, worth what it says to each model, kept as a lesson and taught
    // at once. The intent is what the sentence truly meant, the better reply the one that should have been given,
    // the reserved ID when none is named
    public LessonStruct judge(
            DialogueExchangeStruct exchange,
            String verdictName,
            float understanding,
            float response,
            float wording,
            short intentID,
            short betterReplyID) {

        LessonStruct lesson = dialogueLearnBranch.record(
                exchange, verdictName, understanding, response, wording, intentID, betterReplyID);

        lessons.add(lesson);
        dialogueLearnBranch.learn(lesson);

        return lesson;
    }

    // The newest lesson taken back, the models rebuilt without it — false when no recorded lesson is left
    public boolean forgetLesson() {

        if (lessons.isEmpty() || !lessons.get(lessons.size() - 1).isRecorded())
            return false;

        lessons.remove(lessons.size() - 1);
        retrain();

        return true;
    }

    // Every lesson the trainer recorded written to its file, true when the file was written
    public boolean saveLessons() {

        ArpgObjectStruct rootArpg = new ArpgObjectStruct();
        ArpgArrayStruct lessonArray = new ArpgArrayStruct();

        for (int i = 0; i < lessons.size(); i++)
            if (lessons.get(i).isRecorded())
                lessonArray.add(lessons.get(i).toArpg());

        rootArpg.add("lessons", lessonArray);

        return ArpgUtility.tryWriteObject(
                ArpgUtility.resolveFile(new File(EngineSetting.LESSON_PATH), EngineSetting.DIALOGUE_LESSON_FILE),
                rootArpg);
    }

    public void retrain() {
        dialogueLearnBranch.train();
    }

    // Models \\

    DialogueWeightStruct getUnderstandingWeights() {
        return understandingWeights;
    }

    DialogueWeightStruct getResponseWeights() {
        return responseWeights;
    }

    DialogueWeightStruct getWordingWeights() {
        return wordingWeights;
    }

    // The stable number the models weigh a mood by, 0 for the reserved ID — calm
    int resolveMoodSeed(short moodID) {
        return moodID == EngineSetting.REGISTRY_RESERVED_ID
                ? 0
                : moodManager.getMoodHandleFromMoodID(moodID).getSeed();
    }

    // Accessible \\

    ObjectArrayList<LessonStruct> getLessons() {
        return lessons;
    }

    public int getLessonCount() {
        return lessons.size();
    }
}
