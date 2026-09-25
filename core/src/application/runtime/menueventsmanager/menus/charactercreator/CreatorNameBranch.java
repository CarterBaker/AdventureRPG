package application.runtime.menueventsmanager.menus.charactercreator;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.runtime.RuntimeSetting;
import engine.input.Input;
import engine.input.Keys;
import engine.root.BranchPackage;

public class CreatorNameBranch extends BranchPackage {

    /*
     * Types the character's name. Keys only ever produce letters, digits,
     * single inner spaces, hyphens, and apostrophes — every name is also a
     * valid save file name. Every key pressed in a frame is taken, so fast
     * typing never drops a letter. The name field shows a blinking caret,
     * and the status line under it reports why a name was refused until
     * typing resumes.
     */

    // Name Input \\

    void updateNameInput(CreatorSessionStruct session, Input rawInput) {

        if (rawInput.isKeyClicked(Keys.BACKSPACE))
            removeNameCharacter(session);

        appendTypedCharacters(session, rawInput);
        updateCaret(session);
    }

    private void appendTypedCharacters(CreatorSessionStruct session, Input rawInput) {

        boolean shift = rawInput.isKeyDown(Keys.SHIFT_LEFT) || rawInput.isKeyDown(Keys.SHIFT_RIGHT);

        for (int key = Keys.A; key <= Keys.Z; key++)
            if (rawInput.isKeyClicked(key))
                appendNameCharacter(session, shift ? (char) key : Character.toLowerCase((char) key));

        for (int key = Keys.NUM_0; key <= Keys.NUM_9; key++)
            if (rawInput.isKeyClicked(key))
                appendNameCharacter(session, (char) key);

        if (rawInput.isKeyClicked(Keys.MINUS))
            appendNameCharacter(session, '-');

        if (rawInput.isKeyClicked(Keys.APOSTROPHE))
            appendNameCharacter(session, '\'');

        if (rawInput.isKeyClicked(Keys.SPACE) && canAppendSpace(session.getNameBuffer()))
            appendNameCharacter(session, ' ');
    }

    private boolean canAppendSpace(StringBuilder nameBuffer) {
        return nameBuffer.length() > 0 && nameBuffer.charAt(nameBuffer.length() - 1) != ' ';
    }

    private void appendNameCharacter(CreatorSessionStruct session, char character) {

        StringBuilder nameBuffer = session.getNameBuffer();

        if (nameBuffer.length() >= RuntimeSetting.CREATOR_NAME_MAX_LENGTH)
            return;

        nameBuffer.append(character);
        showStatus(session, "");
        refreshName(session);
    }

    private void removeNameCharacter(CreatorSessionStruct session) {

        StringBuilder nameBuffer = session.getNameBuffer();

        if (nameBuffer.length() == 0)
            return;

        nameBuffer.deleteCharAt(nameBuffer.length() - 1);
        showStatus(session, "");
        refreshName(session);
    }

    // Caret \\

    private void updateCaret(CreatorSessionStruct session) {

        float elapsed = session.getCreatorMenu().getElapsed();
        boolean caretVisible = (int) (elapsed / RuntimeSetting.CREATOR_CARET_BLINK_SECONDS) % 2 == 0;

        if (caretVisible == session.isCaretVisible())
            return;

        session.setCaretVisible(caretVisible);
        refreshName(session);
    }

    // Display \\

    void refreshName(CreatorSessionStruct session) {
        setEntryText(
                session,
                RuntimeSetting.ENTRY_CREATOR_NAME,
                session.isCaretVisible()
                        ? session.getNameBuffer() + RuntimeSetting.CREATOR_NAME_CARET
                        : session.getNameBuffer().toString());
    }

    void showStatus(CreatorSessionStruct session, String status) {
        setEntryText(session, RuntimeSetting.ENTRY_CREATOR_STATUS, status);
    }

    private void setEntryText(CreatorSessionStruct session, int entryPoint, String text) {

        ElementInstance label = session.getCreatorMenu().getEntryPoint(entryPoint);

        if (label != null)
            label.setFontText(text);
    }

    // Accessible \\

    String resolveName(CreatorSessionStruct session) {
        return session.getNameBuffer().toString().trim();
    }
}
