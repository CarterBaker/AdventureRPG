package application.trainer;

public class TrainerSetting {

    /*
     * Constants only the dialogue trainer uses: the commands it takes, the
     * verdict names it records for a corrected meaning and a corrected reply,
     * and every line it prints.
     */

    // Commands
    public static final String COMMAND_PREFIX = "/";
    public static final String COMMAND_BETTER = "better";
    public static final String COMMAND_HELP = "help";
    public static final String COMMAND_INTENTS = "intents";
    public static final String COMMAND_MEANT = "meant";
    public static final String COMMAND_QUIT = "quit";
    public static final String COMMAND_REPLIES = "replies";
    public static final String COMMAND_RESET = "reset";
    public static final String COMMAND_SAVE = "save";
    public static final String COMMAND_SCENARIO = "scenario";
    public static final String COMMAND_SCENARIOS = "scenarios";
    public static final String COMMAND_UNDO = "undo";
    public static final String COMMAND_VERDICTS = "verdicts";
    public static final String COMMAND_WHY = "why";

    // Verdicts
    public static final String VERDICT_BETTER = "better";
    public static final String VERDICT_MEANT = "meant";
    public static final float VERDICT_WORTH_FULL = 1f;
    public static final float VERDICT_WORTH_NONE = 0f;

    // Text
    public static final String TEXT_HELP = """
            Dialogue trainer. Type what you would say and press Enter; the being answers.
            Judge the last answer with a verdict, and it is learned at once:
              /verdicts            list the verdicts (/good, /nonsense, /off_topic ...)
              /meant <intent>      the last sentence meant this intent
              /better <reply>      the last answer should have been this reply
              /why                 how the last turn was understood and chosen
              /undo                take the last lesson back
            Practise against different beings:
              /scenarios           list the scenarios
              /scenario <name>     start a talk with one
              /reset               start the same talk over
            /intents and /replies list the names, /save writes the lessons, /quit saves and leaves.
            """;
    public static final String TEXT_ENTRY_FORMAT = "  %-24s %s%n";
    public static final String TEXT_FORGOTTEN = "The last lesson was taken back.";
    public static final String TEXT_LEARNED_FORMAT = "Learned (%s). %d lessons.%n";
    public static final String TEXT_NOTHING_TO_FORGET = "There is no lesson of this session to take back.";
    public static final String TEXT_NOTHING_TO_JUDGE = "Say something first; there is no answer to judge yet.";
    public static final String TEXT_PROMPT = "> ";
    public static final String TEXT_SAID_FORMAT = "%s%n";
    public static final String TEXT_SAID_NOTHING = "(no answer)";
    public static final String TEXT_SAVE_FAILED = "The lessons could not be written; see the log.";
    public static final String TEXT_SAVED_FORMAT = "Saved %d lessons.%n";
    public static final String TEXT_SCENARIO_FORMAT = "Talking with: %s. %s%n";
    public static final String TEXT_TURN_FORMAT =
            "  [heard as %s, %.0f%% sure | reply %s | regard %+.1f to %.1f | %s]%n";
    public static final String TEXT_UNKNOWN_FORMAT = "Unknown %s: %s%n";
    public static final String TEXT_UNKNOWN_COMMAND = "command or verdict";
    public static final String TEXT_UNKNOWN_INTENT = "intent";
    public static final String TEXT_UNKNOWN_REPLY = "reply";
    public static final String TEXT_UNKNOWN_SCENARIO = "scenario";
    public static final String TEXT_WHY_FORMAT = "  %-24s %3.0f%%%n";

    // Percent
    public static final float PERCENT = 100f;
}
