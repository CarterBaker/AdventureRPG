package editor.commandconsole.chunkfield;

import application.bootstrap.menupipeline.element.ElementInstance;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.commandconsole.CommandConsoleSetting;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class CommandConsoleChunkFieldSystem extends SystemPackage {

    /*
     * The X and Y fields the command console's tree shows under every command
     * that takes a chunk. Clicking a field focuses it, and while one is
     * focused the command line's typing lands there instead: only whole
     * numbers, Backspace removes the last character, Escape lets go of it,
     * and Enter or the row's Go button runs the command with both fields
     * exactly as if it had been typed. What a field holds outlives the tree's
     * layout, so collapsing a group never loses it.
     */

    // Internal
    private CommandManager commandManager;

    // Fields
    private Object2ObjectOpenHashMap<String, StringBuilder[]> commandName2FieldTexts;
    private Object2ObjectOpenHashMap<String, ElementInstance[]> commandName2FieldLabels;

    // Focus
    private String focusedCommandName;
    private int focusedFieldIndex;
    private boolean caretVisible;

    // Base \\

    @Override
    protected void create() {

        // Fields
        this.commandName2FieldTexts = new Object2ObjectOpenHashMap<>();
        this.commandName2FieldLabels = new Object2ObjectOpenHashMap<>();

        // Focus
        this.focusedFieldIndex = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.commandManager = get(CommandManager.class);
    }

    // Layout \\

    public void clearFields() {
        commandName2FieldLabels.clear();
    }

    public void addChunkFields(ElementInstance entry, CommandHandle commandHandle) {

        String commandName = commandHandle.getCommandName();
        ElementInstance[] labels = new ElementInstance[CommandConsoleSetting.CHUNK_FIELD_COUNT];

        bindField(entry, CommandConsoleSetting.ELEMENT_CHUNK_FIELD_X, commandName, 0);
        bindField(entry, CommandConsoleSetting.ELEMENT_CHUNK_FIELD_Y, commandName, 1);
        labels[0] = entry.findChildById(CommandConsoleSetting.ELEMENT_CHUNK_FIELD_X_TEXT);
        labels[1] = entry.findChildById(CommandConsoleSetting.ELEMENT_CHUNK_FIELD_Y_TEXT);

        ElementInstance go = entry.findChildById(CommandConsoleSetting.ELEMENT_CHUNK_GO);

        if (go != null)
            go.setActionArgOverride(commandName);

        if (!commandName2FieldTexts.containsKey(commandName))
            commandName2FieldTexts.put(commandName, createFieldTexts());

        commandName2FieldLabels.put(commandName, labels);
        refreshLabels(commandName);
    }

    private void bindField(ElementInstance entry, String fieldId, String commandName, int fieldIndex) {

        ElementInstance field = entry.findChildById(fieldId);

        if (field != null)
            field.setActionArgOverride(commandName + CommandConsoleSetting.CHUNK_FIELD_KEY_SEPARATOR + fieldIndex);
    }

    private StringBuilder[] createFieldTexts() {

        StringBuilder[] fieldTexts = new StringBuilder[CommandConsoleSetting.CHUNK_FIELD_COUNT];

        for (int i = 0; i < fieldTexts.length; i++)
            fieldTexts[i] = new StringBuilder();

        return fieldTexts;
    }

    // Focus \\

    public void focusField(String fieldKey) {

        int separator = fieldKey.lastIndexOf(CommandConsoleSetting.CHUNK_FIELD_KEY_SEPARATOR);
        String previousCommandName = focusedCommandName;

        this.focusedCommandName = fieldKey.substring(0, separator);
        this.focusedFieldIndex = Integer.parseInt(fieldKey.substring(separator + 1));

        refreshLabels(previousCommandName);
        refreshLabels(focusedCommandName);
    }

    public void clearFocus() {

        String previousCommandName = focusedCommandName;

        this.focusedCommandName = null;
        this.focusedFieldIndex = EngineSetting.INDEX_NOT_FOUND;

        refreshLabels(previousCommandName);
    }

    public boolean hasFocus() {
        return focusedCommandName != null;
    }

    // Typing \\

    public void appendCharacter(char character) {

        if (!hasFocus()
                || CommandConsoleSetting.CHUNK_FIELD_CHARACTERS.indexOf(character) == EngineSetting.INDEX_NOT_FOUND)
            return;

        StringBuilder fieldText = commandName2FieldTexts.get(focusedCommandName)[focusedFieldIndex];

        if (fieldText.length() >= CommandConsoleSetting.CHUNK_FIELD_MAX_LENGTH)
            return;

        fieldText.append(character);
        refreshLabels(focusedCommandName);
    }

    public void removeCharacter() {

        if (!hasFocus())
            return;

        StringBuilder fieldText = commandName2FieldTexts.get(focusedCommandName)[focusedFieldIndex];

        if (fieldText.length() == 0)
            return;

        fieldText.deleteCharAt(fieldText.length() - 1);
        refreshLabels(focusedCommandName);
    }

    public void setCaretVisible(boolean caretVisible) {

        if (this.caretVisible == caretVisible)
            return;

        this.caretVisible = caretVisible;
        refreshLabels(focusedCommandName);
    }

    // Run \\

    public void submitFocusedField() {

        if (hasFocus())
            runCommand(focusedCommandName);
    }

    public void runCommand(String commandName) {

        StringBuilder[] fieldTexts = commandName2FieldTexts.get(commandName);

        if (fieldTexts == null)
            return;

        StringBuilder commandLine = new StringBuilder(commandName);

        for (int i = 0; i < fieldTexts.length; i++)
            commandLine.append(EditorSetting.COMMAND_TOKEN_SEPARATOR).append(fieldTexts[i]);

        clearFocus();
        commandManager.executeCommand(commandLine.toString());
    }

    // Labels \\

    private void refreshLabels(String commandName) {

        if (commandName == null)
            return;

        ElementInstance[] labels = commandName2FieldLabels.get(commandName);
        StringBuilder[] fieldTexts = commandName2FieldTexts.get(commandName);

        if (labels == null || fieldTexts == null)
            return;

        for (int i = 0; i < labels.length; i++) {

            if (labels[i] == null)
                continue;

            boolean focused = commandName.equals(focusedCommandName) && i == focusedFieldIndex;

            labels[i].setFontText(focused && caretVisible
                    ? fieldTexts[i] + CommandConsoleSetting.COMMAND_CARET
                    : fieldTexts[i].toString());
        }
    }
}
