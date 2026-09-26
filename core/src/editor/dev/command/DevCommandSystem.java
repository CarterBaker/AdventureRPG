package editor.dev.command;

import java.util.function.Consumer;

import editor.bootstrap.commandpipeline.command.CommandStruct;
import editor.dev.freecamera.FreeCameraSystem;
import engine.editor.EditorSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DevCommandSystem extends SystemPackage {

    /*
     * Receives the commands the command console routes to this Dev window and
     * runs them during this window's own update, inside its crash boundary.
     * Every command this window can act on is mapped to its action once;
     * a command it has no action for is reported against the window.
     */

    // Internal
    private FreeCameraSystem freeCameraSystem;

    // Palette
    private Object2ObjectOpenHashMap<String, Consumer<CommandStruct>> commandName2Action;

    // Commands
    private ObjectArrayList<CommandStruct> pendingCommands;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.commandName2Action = new Object2ObjectOpenHashMap<>();
        registerActions();

        // Commands
        this.pendingCommands = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.freeCameraSystem = get(FreeCameraSystem.class);
    }

    private void registerActions() {
        commandName2Action.put(EditorSetting.COMMAND_FLY, command -> freeCameraSystem.toggleFreeCamera());
    }

    // Update \\

    @Override
    protected void update() {

        for (int i = 0; i < pendingCommands.size(); i++)
            executeCommand(pendingCommands.get(i));

        pendingCommands.clear();
    }

    private void executeCommand(CommandStruct command) {

        Consumer<CommandStruct> action = commandName2Action.get(command.getCommandName());

        if (action == null) {
            errorLog(context.getWindow().getTitle() + EditorSetting.COMMAND_MESSAGE_NO_ACTION
                    + command.getCommandName());
            return;
        }

        action.accept(command);
    }

    // Management \\

    public void queueCommand(CommandStruct command) {
        pendingCommands.add(command);
    }
}
