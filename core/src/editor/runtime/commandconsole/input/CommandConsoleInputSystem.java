package editor.runtime.commandconsole.input;

import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.runtime.commandconsole.CommandConsoleSetting;
import editor.runtime.commandconsole.chunkfield.CommandConsoleChunkFieldSystem;
import editor.runtime.commandconsole.panel.CommandConsolePanelSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.EditorSetting;
import engine.input.Input;
import engine.input.InputListener;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandConsoleInputSystem extends SystemPackage implements InputListener {

    /*
     * The command console's command line. While its window holds focus it
     * takes every character the editor font can draw, Backspace removes the
     * last one, Escape clears the line, and Enter hands it to CommandManager,
     * which routes it to every open Dev window. Up and Down step back and
     * forth through the lines sent before, and Tab completes the command name
     * being typed, or lists every command it could still be. While one of the
     * tree's chunk
     * fields is focused the same keys edit that field instead, and Escape
     * hands typing back to the line. Typed characters are read from whichever
     * OS window the console currently lives in, so the line keeps working
     * after its tab is moved, and a caret blinks at the end of whatever takes
     * the typing while the window is focused.
     */

    // Internal
    private InputManager inputManager;
    private WindowManager windowManager;
    private CommandManager commandManager;
    private EditorInputSystem editorInputSystem;
    private CommandConsolePanelSystem commandConsolePanelSystem;
    private CommandConsoleChunkFieldSystem commandConsoleChunkFieldSystem;

    // Text Input
    private Input textInput;
    private StringBuilder commandBuffer;

    // History
    private ObjectArrayList<String> commandHistory;
    private int historyIndex;

    // Caret
    private float caretElapsed;
    private boolean caretVisible;

    // Base \\

    @Override
    protected void create() {

        // Text Input
        this.commandBuffer = new StringBuilder();

        // History
        this.commandHistory = new ObjectArrayList<>();
        this.historyIndex = 0;
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
        this.commandManager = get(CommandManager.class);
        this.editorInputSystem = get(EditorInputSystem.class);
        this.commandConsolePanelSystem = get(CommandConsolePanelSystem.class);
        this.commandConsoleChunkFieldSystem = get(CommandConsoleChunkFieldSystem.class);
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
            submit();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)) {
            clear();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_BACKSPACE)) {
            removeCharacter();
            return;
        }

        if (commandConsoleChunkFieldSystem.hasFocus())
            return;

        if (rawInput.isKeyClicked(EditorSetting.KEY_TAB)) {
            completeCommand();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_UP)) {
            recallCommand(-1);
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_DOWN))
            recallCommand(1);
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

        if (!isFocused() || EngineSetting.FONT_DEFAULT_CHARSET.indexOf(character) == EngineSetting.INDEX_NOT_FOUND)
            return;

        if (commandConsoleChunkFieldSystem.hasFocus()) {
            commandConsoleChunkFieldSystem.appendCharacter(character);
            restartCaret();
            return;
        }

        appendCommandCharacter(character);
    }

    // Routing \\

    private void submit() {

        if (commandConsoleChunkFieldSystem.hasFocus()) {
            commandConsoleChunkFieldSystem.submitFocusedField();
            restartCaret();
            return;
        }

        submitCommand();
    }

    private void clear() {

        if (commandConsoleChunkFieldSystem.hasFocus()) {
            commandConsoleChunkFieldSystem.clearFocus();
            restartCaret();
            return;
        }

        clearCommand();
    }

    private void removeCharacter() {

        if (commandConsoleChunkFieldSystem.hasFocus()) {
            commandConsoleChunkFieldSystem.removeCharacter();
            restartCaret();
            return;
        }

        removeCommandCharacter();
    }

    // Command Line \\

    private void submitCommand() {

        String commandLine = commandBuffer.toString();

        recordCommand(commandLine.trim());
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

    private void replaceCommand(String commandLine) {

        commandBuffer.setLength(0);
        commandBuffer.append(commandLine, 0, Math.min(commandLine.length(), CommandConsoleSetting.COMMAND_MAX_LENGTH));
        restartCaret();
    }

    // Completes the command name while it is the only word on the line
    private void completeCommand() {

        if (commandBuffer.indexOf(EditorSetting.COMMAND_TOKEN_SEPARATOR) != EngineSetting.INDEX_NOT_FOUND)
            return;

        replaceCommand(commandManager.completeCommandName(commandBuffer.toString()));
    }

    private void refreshCommandLabel() {

        boolean lineCaretVisible = caretVisible && !commandConsoleChunkFieldSystem.hasFocus();

        commandConsolePanelSystem.setCommandText(lineCaretVisible
                ? commandBuffer + CommandConsoleSetting.COMMAND_CARET
                : commandBuffer.toString());
        commandConsoleChunkFieldSystem.setCaretVisible(caretVisible);
    }

    // History \\

    // Keeps a sent line, unless it repeats the last one, dropping the oldest past the limit
    private void recordCommand(String commandLine) {

        if (!commandLine.isEmpty()
                && (commandHistory.isEmpty() || !commandHistory.get(commandHistory.size() - 1).equals(commandLine)))
            commandHistory.add(commandLine);

        if (commandHistory.size() > CommandConsoleSetting.COMMAND_HISTORY_LIMIT)
            commandHistory.remove(0);

        this.historyIndex = commandHistory.size();
    }

    // Steps through the lines sent before; stepping past the newest leaves an empty line
    private void recallCommand(int direction) {

        if (commandHistory.isEmpty())
            return;

        this.historyIndex = Math.max(0, Math.min(commandHistory.size(), historyIndex + direction));

        if (historyIndex == commandHistory.size()) {
            clearCommand();
            return;
        }

        replaceCommand(commandHistory.get(historyIndex));
    }

    // Management \\

    // Shows the caret at once on whatever now takes the typing, and starts its blink over
    public void restartCaret() {

        this.caretElapsed = 0f;
        this.caretVisible = isFocused();
        refreshCommandLabel();
    }

    // Utility \\

    private boolean isFocused() {
        return windowManager.getFocusedWindow() == context.getWindow();
    }
}
