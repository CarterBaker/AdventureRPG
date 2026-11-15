package application.trainer.talk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import application.bootstrap.dialoguepipeline.dialoguemanager.DialogueManager;
import application.bootstrap.dialoguepipeline.intentmanager.IntentManager;
import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.replymanager.ReplyManager;
import application.bootstrap.dialoguepipeline.scenario.ScenarioHandle;
import application.bootstrap.dialoguepipeline.scenariomanager.ScenarioManager;
import application.bootstrap.dialoguepipeline.trait.TraitHandle;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.dialoguepipeline.util.DialogueExchangeStruct;
import application.bootstrap.dialoguepipeline.util.DialogueProfileStruct;
import application.bootstrap.dialoguepipeline.verdict.VerdictHandle;
import application.bootstrap.dialoguepipeline.verdictmanager.VerdictManager;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import application.trainer.TrainerContext;
import application.trainer.TrainerSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class TrainerTalkSystem extends SystemPackage {

    /*
     * Holds the trainer's practice talks over the console. Each line typed is
     * said to the being of the scenario in hand through DialogueManager, and
     * its answer is printed with how the line was understood, the reply
     * chosen and how the being's regard moved. A line opening with a slash is
     * a command: a verdict's name judges the last answer and teaches it at
     * once, meant and better correct what was understood and what was
     * answered, why shows how the last turn was weighed, undo takes the last
     * lesson back, scenario starts a talk with another being, and save and
     * quit write the lessons. It reads one line a frame, waiting on the
     * console, and closes the engine when the trainer quits or the console
     * ends.
     */

    // Internal
    private DialogueManager dialogueManager;
    private IntentManager intentManager;
    private ReplyManager replyManager;
    private TraitManager traitManager;
    private MoodManager moodManager;
    private ProfessionManager professionManager;
    private VerdictManager verdictManager;
    private ScenarioManager scenarioManager;

    // Console
    private PrintStream console;
    private BufferedReader input;

    // Talk
    private DialogueExchangeStruct exchange;
    private short scenarioID;
    private boolean answered;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.dialogueManager = get(DialogueManager.class);
        this.intentManager = get(IntentManager.class);
        this.replyManager = get(ReplyManager.class);
        this.traitManager = get(TraitManager.class);
        this.moodManager = get(MoodManager.class);
        this.professionManager = get(ProfessionManager.class);
        this.verdictManager = get(VerdictManager.class);
        this.scenarioManager = get(ScenarioManager.class);
    }

    @Override
    protected void update() {

        if (exchange == null)
            open();

        console.print(TrainerSetting.TEXT_PROMPT);
        console.flush();

        String line = readLine();

        if (line == null) {
            quit();
            return;
        }

        line = line.trim();

        if (line.startsWith(TrainerSetting.COMMAND_PREFIX))
            command(line.substring(TrainerSetting.COMMAND_PREFIX.length()));
        else if (!line.isEmpty())
            say(line);
    }

    // The console taken from the context, and a talk begun with the first scenario
    private void open() {

        TrainerContext trainerContext = (TrainerContext) context;

        this.console = trainerContext.getConsole();
        this.input = trainerContext.getInput();
        this.exchange = dialogueManager.createExchange();

        console.print(TrainerSetting.TEXT_HELP);
        begin((short) (EngineSetting.REGISTRY_RESERVED_ID + 1));
    }

    private String readLine() {

        try {
            return input.readLine();
        } catch (IOException exception) {
            return null;
        }
    }

    // Talk \\

    // A new talk with a scenario's being, as the scenario stands it toward the speaker
    private void begin(short scenarioID) {

        ScenarioHandle scenarioHandle = scenarioManager.getScenarioHandleFromScenarioID(scenarioID);
        DialogueProfileStruct profile = exchange.getProfile();
        short[] traitIDs = scenarioHandle.getTraitIDs();
        ValueMapStruct moodLevels = scenarioHandle.getMoodLevels();
        short professionID = scenarioHandle.getProfessionID();

        this.scenarioID = scenarioID;
        this.answered = false;

        exchange.begin(System.nanoTime());
        profile.clear();
        profile.setName(scenarioHandle.getListenerName());
        profile.setSpeakerName(scenarioHandle.getSpeakerName());
        profile.setProfessionID(professionID);
        profile.setAffinity(scenarioHandle.getAffinity());
        profile.setMorality(scenarioHandle.getMorality());
        profile.setRelationID(scenarioHandle.getRelationID());

        if (professionID != EngineSetting.REGISTRY_RESERVED_ID)
            profile.setTitle(professionManager.getProfessionHandleFromProfessionID(professionID).getDisplayName());

        for (int i = 0; i < traitIDs.length; i++) {

            TraitHandle traitHandle = traitManager.getTraitHandleFromTraitID(traitIDs[i]);

            profile.addTrait(traitHandle.getTraitID(), traitHandle.isWordless());
        }

        for (int i = 0; i < moodLevels.size(); i++)
            profile.setMoodLevel(moodLevels.getID(i), moodLevels.getValue(i));

        console.printf(
                TrainerSetting.TEXT_SCENARIO_FORMAT, scenarioHandle.getDisplayName(), scenarioHandle.getDescription());
    }

    private void say(String line) {

        DialogueProfileStruct profile = exchange.getProfile();

        exchange.getHeard().setLength(0);
        exchange.getHeard().append(line, 0, Math.min(line.length(), EngineSetting.DIALOGUE_HEARD_MAX_LENGTH));
        dialogueManager.converse(exchange);
        this.answered = true;

        console.printf(
                TrainerSetting.TEXT_SAID_FORMAT,
                exchange.getSaid().length() > 0 ? exchange.getSaid() : TrainerSetting.TEXT_SAID_NOTHING);
        console.printf(
                TrainerSetting.TEXT_TURN_FORMAT,
                intentManager.getIntentHandleFromIntentID(exchange.getIntentID()).getIntentName(),
                exchange.getConfidence() * TrainerSetting.PERCENT,
                resolveReplyName(exchange.getReplyID()),
                exchange.getAffinityShift(),
                profile.getAffinity(),
                resolveMoodName(profile.getDominantMoodID()));
    }

    private String resolveReplyName(short replyID) {
        return replyID == EngineSetting.REGISTRY_RESERVED_ID
                ? TrainerSetting.TEXT_SAID_NOTHING
                : replyManager.getReplyHandleFromReplyID(replyID).getReplyName();
    }

    private String resolveMoodName(short moodID) {
        return moodID == EngineSetting.REGISTRY_RESERVED_ID
                ? EngineSetting.DIALOGUE_MOOD_CALM
                : moodManager.getMoodHandleFromMoodID(moodID).getMoodName();
    }

    // Commands \\

    private void command(String line) {

        int space = line.indexOf(' ');
        String name = space == EngineSetting.INDEX_NOT_FOUND ? line : line.substring(0, space);
        String argument = space == EngineSetting.INDEX_NOT_FOUND ? EngineSetting.DIALOGUE_TEXT_NONE
                : line.substring(space + 1).trim();

        switch (name) {
            case TrainerSetting.COMMAND_HELP -> console.print(TrainerSetting.TEXT_HELP);
            case TrainerSetting.COMMAND_SCENARIOS -> listScenarios();
            case TrainerSetting.COMMAND_VERDICTS -> listVerdicts();
            case TrainerSetting.COMMAND_INTENTS -> listIntents();
            case TrainerSetting.COMMAND_REPLIES -> listReplies();
            case TrainerSetting.COMMAND_SCENARIO -> chooseScenario(argument);
            case TrainerSetting.COMMAND_RESET -> begin(scenarioID);
            case TrainerSetting.COMMAND_WHY -> explain();
            case TrainerSetting.COMMAND_MEANT -> correctIntent(argument);
            case TrainerSetting.COMMAND_BETTER -> correctReply(argument);
            case TrainerSetting.COMMAND_UNDO -> console.println(dialogueManager.forgetLesson()
                    ? TrainerSetting.TEXT_FORGOTTEN
                    : TrainerSetting.TEXT_NOTHING_TO_FORGET);
            case TrainerSetting.COMMAND_SAVE -> save();
            case TrainerSetting.COMMAND_QUIT -> quit();
            default -> judge(name);
        }
    }

    private void chooseScenario(String scenarioName) {

        if (!scenarioManager.hasScenario(scenarioName)) {
            console.printf(TrainerSetting.TEXT_UNKNOWN_FORMAT, TrainerSetting.TEXT_UNKNOWN_SCENARIO, scenarioName);
            return;
        }

        begin(scenarioManager.getScenarioIDFromScenarioName(scenarioName));
    }

    // Verdicts \\

    // A verdict passed on the last answer, worth what its definition says to each model
    private void judge(String verdictName) {

        if (!verdictManager.hasVerdict(verdictName)) {
            console.printf(TrainerSetting.TEXT_UNKNOWN_FORMAT, TrainerSetting.TEXT_UNKNOWN_COMMAND, verdictName);
            return;
        }

        VerdictHandle verdictHandle = verdictManager.getVerdictHandleFromVerdictName(verdictName);

        learn(
                verdictName,
                verdictHandle.getUnderstanding(),
                verdictHandle.getResponse(),
                verdictHandle.getWording(),
                exchange.getIntentID(),
                EngineSetting.REGISTRY_RESERVED_ID);
    }

    // The last sentence taught as meaning an intent, whatever it was heard as
    private void correctIntent(String intentName) {

        if (!intentManager.hasIntent(intentName)) {
            console.printf(TrainerSetting.TEXT_UNKNOWN_FORMAT, TrainerSetting.TEXT_UNKNOWN_INTENT, intentName);
            return;
        }

        learn(
                TrainerSetting.VERDICT_MEANT,
                TrainerSetting.VERDICT_WORTH_FULL,
                TrainerSetting.VERDICT_WORTH_NONE,
                TrainerSetting.VERDICT_WORTH_NONE,
                intentManager.getIntentIDFromIntentName(intentName),
                EngineSetting.REGISTRY_RESERVED_ID);
    }

    // The last answer taught as better given by another reply
    private void correctReply(String replyName) {

        if (!replyManager.hasReply(replyName)) {
            console.printf(TrainerSetting.TEXT_UNKNOWN_FORMAT, TrainerSetting.TEXT_UNKNOWN_REPLY, replyName);
            return;
        }

        learn(
                TrainerSetting.VERDICT_BETTER,
                TrainerSetting.VERDICT_WORTH_NONE,
                TrainerSetting.VERDICT_WORTH_NONE,
                TrainerSetting.VERDICT_WORTH_NONE,
                exchange.getIntentID(),
                replyManager.getReplyIDFromReplyName(replyName));
    }

    private void learn(
            String verdictName,
            float understanding,
            float response,
            float wording,
            short intentID,
            short betterReplyID) {

        if (!answered) {
            console.println(TrainerSetting.TEXT_NOTHING_TO_JUDGE);
            return;
        }

        dialogueManager.judge(exchange, verdictName, understanding, response, wording, intentID, betterReplyID);
        console.printf(TrainerSetting.TEXT_LEARNED_FORMAT, verdictName, dialogueManager.getLessonCount());
    }

    // Every reply that was open in the last turn and the share it was given
    private void explain() {

        if (!answered) {
            console.println(TrainerSetting.TEXT_NOTHING_TO_JUDGE);
            return;
        }

        for (int i = 0; i < exchange.getCandidateCount(); i++)
            console.printf(
                    TrainerSetting.TEXT_WHY_FORMAT,
                    resolveReplyName(exchange.getCandidateIDs()[i]),
                    exchange.getCandidateShares()[i] * TrainerSetting.PERCENT);
    }

    // Lists \\

    private void listScenarios() {

        for (short id = EngineSetting.REGISTRY_RESERVED_ID + 1; id < scenarioManager.getScenarioIDCount(); id++) {

            ScenarioHandle scenarioHandle = scenarioManager.getScenarioHandleFromScenarioID(id);

            console.printf(
                    TrainerSetting.TEXT_ENTRY_FORMAT,
                    scenarioHandle.getScenarioName(),
                    scenarioHandle.getDescription());
        }
    }

    private void listVerdicts() {

        for (short id = EngineSetting.REGISTRY_RESERVED_ID + 1; id < verdictManager.getVerdictIDCount(); id++) {

            VerdictHandle verdictHandle = verdictManager.getVerdictHandleFromVerdictID(id);

            console.printf(
                    TrainerSetting.TEXT_ENTRY_FORMAT,
                    TrainerSetting.COMMAND_PREFIX + verdictHandle.getVerdictName(),
                    verdictHandle.getDescription());
        }
    }

    private void listIntents() {

        for (short id = EngineSetting.REGISTRY_RESERVED_ID + 1; id < intentManager.getIntentIDCount(); id++)
            console.printf(
                    TrainerSetting.TEXT_ENTRY_FORMAT,
                    intentManager.getIntentHandleFromIntentID(id).getIntentName(),
                    intentManager.getIntentHandleFromIntentID(id).getDescription());
    }

    private void listReplies() {

        for (short id = EngineSetting.REGISTRY_RESERVED_ID + 1; id < replyManager.getReplyIDCount(); id++)
            console.printf(
                    TrainerSetting.TEXT_ENTRY_FORMAT,
                    replyManager.getReplyHandleFromReplyID(id).getReplyName(),
                    replyManager.getReplyHandleFromReplyID(id).getDescription());
    }

    // Save \\

    private void save() {

        if (dialogueManager.saveLessons())
            console.printf(TrainerSetting.TEXT_SAVED_FORMAT, dialogueManager.getLessonCount());
        else
            console.println(TrainerSetting.TEXT_SAVE_FAILED);
    }

    private void quit() {
        save();
        internal.windowPlatform.exit();
    }
}
