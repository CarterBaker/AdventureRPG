package application.runtime.menueventsmanager.menus.charactercreator;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.runtime.RuntimeSetting;
import application.runtime.menueventsmanager.util.NameInputUtility;
import engine.input.Input;
import engine.root.BranchPackage;

public class CreatorNameBranch extends BranchPackage {

    /*
     * Types the character's name through NameInputUtility, so every name is
     * also a valid save file name. The name field shows a blinking caret,
     * and the status line under it reports why a name was refused until
     * typing resumes.
     */

    // Name Input \\

    void updateNameInput(CreatorSessionStruct session, Input rawInput) {

        if (NameInputUtility.typeName(rawInput, session.getNameBuffer(), RuntimeSetting.CREATOR_NAME_MAX_LENGTH)) {
            showStatus(session, "");
            refreshName(session);
        }

        updateCaret(session);
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
