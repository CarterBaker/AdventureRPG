package editor.runtime.commandconsole;

import editor.bootstrap.commandpipeline.command.CommandTarget;

public class CharacterCommandConsoleContext extends CommandConsoleContext {

    /*
     * Editor tab for the commands that run in the character tabs, on the
     * character open there: the command console's own window, its tree
     * listing only the groups whose commands target the character tabs, so
     * altering a character and driving the game stay apart.
     */

    // Accessible \\

    @Override
    public CommandTarget getTarget() {
        return CommandTarget.CHARACTER;
    }
}
