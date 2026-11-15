package application.trainer;

import java.io.BufferedReader;
import java.io.PrintStream;

import application.trainer.talk.TrainerTalkSystem;
import engine.root.ContextPackage;

public class TrainerContext extends ContextPackage {

    /*
     * Context paired with the trainer engine's hidden window. Owns the one
     * system that holds practice talks over the console, and keeps the
     * console the engine hands it, since the engine's log takes over the
     * standard streams while it runs. Nothing else runs — no menu, world or
     * player.
     */

    // Runtime
    private TrainerTalkSystem trainerTalkSystem;

    // Console
    private PrintStream console;
    private BufferedReader input;

    // Internal \\

    @Override
    protected void create() {

        // Runtime
        this.trainerTalkSystem = create(TrainerTalkSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Console \\

    public void bindConsole(PrintStream console, BufferedReader input) {
        this.console = console;
        this.input = input;
    }

    // Accessible \\

    public PrintStream getConsole() {
        return console;
    }

    public BufferedReader getInput() {
        return input;
    }
}
