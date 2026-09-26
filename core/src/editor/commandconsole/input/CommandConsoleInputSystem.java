package editor.commandconsole.input;

import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.commandconsole.CommandConsoleSetting;
import editor.commandconsole.panel.CommandConsolePanelSystem;
import engine.editor.EditorInputSystem;
import engine.editor.EditorSetting;
import engine.input.Input;
import engine.input.InputListener;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class CommandConsoleInputSystem extends SystemPackage implements InputListener {

    /*
     * The command console's command line. While its window holds focus it
     * takes every character the editor font can draw, Backspace removes the
     * last one, Escape clears the line, and Enter hands it to CommandManager,
     * which routes it to every open Dev window. Typed characters are read from
     * whichever OS window the console currently lives in, so the line keeps
     * working after its tab is moved, and a caret blinks at its end while the
     * window is focused.
     */

    // Internal
    private InputManager inputManager;
    private WindowManager windowManager;
    private CommandManager commandManager;
    private EditorInputSystem editorInputSystem;
    private CommandConsolePanelSystem commandConsolePanelSystem;

    // Text Input
    private Input textInput;
    private StringBuilder commandBuffer;

    // Caret
    private float caretElapsed;
    private boolean caretVisible;

    // Base \\

    @Override
    protected void create() {

        // Text Input
        this.commandBuffer = new StringBuilder();
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
        this.commandManager = get(CommandManager.class);
        this.editorInputSystem = get(EditorInputSystem.class);
        this.commandConsolePanelSystem = get(CommandConsolePanelSystem.class);
    }

    @Override
    protected void dispose() {
        releaseTextInput();
    }

    // Update \\

    @Override
    protected void update() {
        bindTextInput();
        updateCommandKeys();
        updateCaret();
    }

    private void updateCommandKeys() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (rawInput.isKeyClicked(EditorSetting.KEY_ENTER)
                || rawInput.isKeyClicked(EditorSetting.KEY_ENTER_NUMPAD)) {
            submitCommand();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)) {
            clearCommand();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_BACKSPACE))
            removeCommandCharacter();
    }

    private void updateCaret() {

        boolean focused = isFocused();

        this.caretElapsed = focused ? caretElapsed + internal.getDeltaTime() : 0f;
        boolean visible = focused && (int) (caretElapsed / CommandConsoleSetting.COMMAND_CARET_BLINK_SECONDS) % 2 == 0;

        if (visible == caretVisible)
            return;

        this.caretVisible = visible;
        refreshCommandLabel();
    }

    // Text Input \\

    private void bindTextInput() {

        Input rawInput = inputManager.getRawInput(context.getWindow());

        if (rawInput == textInput)
            return;

        releaseTextInput();
        this.textInput = rawInput;

        if (textInput != null)
            textInput.addListener(this);
    }

    private void releaseTextInput() {

        if (textInput == null)
            return;

        textInput.removeListener(this);
        this.textInput = null;
    }

    @Override
    public void onChar(char character) {

        if (isFocused() && EngineSetting.FONT_DEFAULT_CHARSET.indexOf(character) != EngineSetting.INDEX_NOT_FOUND)
            appendCommandCharacter(character);
    }

    // Command Line \\

    private void submitCommand() {

        String commandLine = commandBuffer.toString();

        clearCommand();
        commandManager.executeCommand(commandLine);
    }

    private void clearCommand() {

        commandBuffer.setLength(0);
        restartCaret();
    }

    private void appendCommandCharacter(char character) {

        if (commandBuffer.length() >= CommandConsoleSetting.COMMAND_MAX_LENGTH)
            return;

        commandBuffer.append(character);
        restartCaret();
    }

    private void removeCommandCharacter() {

        if (commandBuffer.length() == 0)
            return;

        commandBuffer.deleteCharAt(commandBuffer.length() - 1);
        restartCaret();
    }

    private void restartCaret() {

        this.caretElapsed = 0f;
        this.caretVisible = isFocused();
        refreshCommandLabel();
    }

    private void refreshCommandLabel() {
        commandConsolePanelSystem.setCommandText(caretVisible
                ? commandBuffer + CommandConsoleSetting.COMMAND_CARET
                : commandBuffer.toString());
    }

    // Utility \\

    private boolean isFocused() {
        return windowManager.getFocusedWindow() == context.getWindow();
    }
}
