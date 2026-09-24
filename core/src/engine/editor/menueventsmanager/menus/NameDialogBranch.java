package engine.editor.menueventsmanager.menus;

import java.util.function.Consumer;
import java.util.function.Predicate;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.editor.EditorSetting;
import engine.input.Input;
import engine.root.BranchPackage;

public class NameDialogBranch extends BranchPackage {

    /*
     * The editor's one name-entry dialog. Callers supply a title, a validation
     * rule, and a confirm action; typed keys only ever produce valid file-name
     * characters.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;

    // Dialog
    private MenuInstance dialog;
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

        if (dialog != null)
            return;

        this.nameValidator = nameValidator;
        this.confirmAction = confirmAction;
        nameBuffer.setLength(0);

        dialog = menuManager.openMenuWindow(EditorSetting.MENU_EDITOR_NAME_DIALOG, window.getGLWindow());
        setEntryText(EditorSetting.ENTRY_NAME_DIALOG_TITLE, title);
        refreshNameLabel();
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

        if (rawInput.isKeyClicked(EditorSetting.KEY_BACKSPACE)) {
            removeNameCharacter();
            return;
        }

        char typed = resolveTypedCharacter(rawInput);

        if (typed != 0)
            appendNameCharacter(typed);
    }

    private char resolveTypedCharacter(Input rawInput) {

        if (rawInput.isKeyClicked(EditorSetting.KEY_SPACE))
            return '_';

        if (rawInput.isKeyClicked(EditorSetting.KEY_MINUS))
            return '-';

        boolean shift = rawInput.isKeyDown(EditorSetting.KEY_LEFT_SHIFT)
                || rawInput.isKeyDown(EditorSetting.KEY_RIGHT_SHIFT);

        for (int key = EditorSetting.KEY_A; key <= EditorSetting.KEY_Z; key++)
            if (rawInput.isKeyClicked(key))
                return shift ? (char) key : Character.toLowerCase((char) key);

        for (int key = EditorSetting.KEY_0; key <= EditorSetting.KEY_9; key++)
            if (rawInput.isKeyClicked(key))
                return (char) key;

        return 0;
    }

    private void appendNameCharacter(char character) {

        if (nameBuffer.length() >= EditorSetting.NAME_INPUT_MAX_LENGTH)
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
