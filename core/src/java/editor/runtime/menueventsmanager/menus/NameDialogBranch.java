package editor.runtime.menueventsmanager.menus;

import java.util.function.Consumer;
import java.util.function.Predicate;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import engine.input.Input;
import engine.input.InputListener;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;

public class NameDialogBranch extends BranchPackage implements InputListener {

    /*
     * The editor's one text-entry dialog. Callers supply a title, a validation
     * rule, and a confirm action. Both forms read the dialog window's typed
     * characters, so every key layout, Shift and dead key types what it
     * should: a name dialog starts empty and takes any character the editor
     * font can draw that a file name may hold (spaces and underscores
     * included), a text dialog starts from the given text and takes any
     * character the font can draw.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;

    // Dialog
    private MenuInstance dialog;
    private Input textInput;
    private boolean freeText;
    private int maxLength;
    private StringBuilder nameBuffer;
    private Predicate<String> nameValidator;
    private Consumer<String> confirmAction;

    // Base \\

    @Override
    protected void create() {
        this.nameBuffer = new StringBuilder();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
    }

    @Override
    protected void update() {

        if (dialog == null)
            return;

        if (!dialog.getWindow().hasCompositeTarget()) {
            close();
            return;
        }

        updateNameInput();
    }

    // Management \\

    public void open(
            WindowInstance window,
            String title,
            Predicate<String> nameValidator,
            Consumer<String> confirmAction) {
        openDialog(window, title, "", false, nameValidator, confirmAction);
    }

    public void openText(
            WindowInstance window,
            String title,
            String initialText,
            Predicate<String> textValidator,
            Consumer<String> confirmAction) {
        openDialog(window, title, initialText, true, textValidator, confirmAction);
    }

    private void openDialog(
            WindowInstance window,
            String title,
            String initialText,
            boolean freeText,
            Predicate<String> nameValidator,
            Consumer<String> confirmAction) {

        if (dialog != null)
            return;

        this.nameValidator = nameValidator;
        this.confirmAction = confirmAction;
        this.freeText = freeText;
        this.maxLength = freeText ? EditorSetting.TEXT_INPUT_MAX_LENGTH : EditorSetting.NAME_INPUT_MAX_LENGTH;
        nameBuffer.setLength(0);
        nameBuffer.append(initialText, 0, Math.min(initialText.length(), maxLength));

        dialog = menuManager.openMenuWindow(EditorSetting.MENU_EDITOR_NAME_DIALOG, window.getGLWindow());
        setEntryText(EditorSetting.ENTRY_NAME_DIALOG_TITLE, title);
        refreshNameLabel();

        this.textInput = inputManager.getRawInput(dialog.getWindow());
        textInput.addListener(this);
    }

    public boolean isOpen() {
        return dialog != null;
    }

    // Dialog Events \\

    public void confirm() {

        if (dialog == null)
            return;

        String name = nameBuffer.toString();

        if (!nameValidator.test(name))
            return;

        Consumer<String> action = confirmAction;
        close();
        action.accept(name);
    }

    public void cancel() {
        close();
    }

    private void close() {

        if (dialog == null)
            return;

        if (textInput != null) {
            textInput.removeListener(this);
            textInput = null;
        }

        menuManager.closeMenuWindow(dialog);
        dialog = null;
        nameValidator = null;
        confirmAction = null;
        nameBuffer.setLength(0);
    }

    // Name Input \\

    private void updateNameInput() {

        Input rawInput = inputManager.getRawInput(dialog.getWindow());

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)) {
            close();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_ENTER)
                || rawInput.isKeyClicked(EditorSetting.KEY_ENTER_NUMPAD)) {
            confirm();
            return;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_BACKSPACE))
            removeNameCharacter();
    }

    @Override
    public void onChar(char character) {

        if (textInput == null || EngineSetting.FONT_DEFAULT_CHARSET.indexOf(character) == EngineSetting.INDEX_NOT_FOUND)
            return;

        if (freeText || FileUtility.isFileNameCharacter(character))
            appendNameCharacter(character);
    }

    private void appendNameCharacter(char character) {

        if (nameBuffer.length() >= maxLength)
            return;

        nameBuffer.append(character);
        refreshNameLabel();
    }

    private void removeNameCharacter() {

        if (nameBuffer.length() == 0)
            return;

        nameBuffer.deleteCharAt(nameBuffer.length() - 1);
        refreshNameLabel();
    }

    private void refreshNameLabel() {
        setEntryText(EditorSetting.ENTRY_NAME_DIALOG_LABEL, nameBuffer.toString());
    }

    private void setEntryText(int entryPoint, String text) {

        ElementInstance label = dialog.getEntryPoint(entryPoint);

        if (label != null)
            label.setFontText(text);
    }
}
