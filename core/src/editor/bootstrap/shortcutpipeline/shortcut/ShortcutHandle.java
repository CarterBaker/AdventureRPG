package editor.bootstrap.shortcutpipeline.shortcut;

import engine.input.Input;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class ShortcutHandle extends HandlePackage {

    /*
     * Persistent reference to one loaded editor shortcut. Registered and
     * owned by ShortcutManager. Delegates all accessors through ShortcutData,
     * which a rebind replaces in place, so every reference keeps up.
     * A shortcut with an action is pressed on the frame its key goes down
     * with exactly its modifiers held, so Ctrl+S never also runs S; one
     * without is held for as long as its key and at least its modifiers are.
     */

    // Internal
    private ShortcutData shortcutData;

    // Constructor \\

    public void constructor(ShortcutData shortcutData) {

        // Internal
        this.shortcutData = shortcutData;
    }

    // Input \\

    public boolean isPressed(Input input, int heldModifiers) {
        return heldModifiers == shortcutData.getModifiers() && input.isKeyClicked(shortcutData.getKey());
    }

    public boolean isHeld(Input input, int heldModifiers) {

        int modifiers = shortcutData.getModifiers();

        return (heldModifiers & modifiers) == modifiers && (!hasKey() || input.isKeyDown(shortcutData.getKey()));
    }

    // Accessible \\

    public ShortcutData getShortcutData() {
        return shortcutData;
    }

    public String getShortcutName() {
        return shortcutData.getShortcutName();
    }

    public int getShortcutID() {
        return shortcutData.getShortcutID();
    }

    public Class<? extends ContextPackage> getContextClass() {
        return shortcutData.getContextClass();
    }

    public int getKey() {
        return shortcutData.getKey();
    }

    public int getModifiers() {
        return shortcutData.getModifiers();
    }

    public String getActionClass() {
        return shortcutData.getActionClass();
    }

    public String getActionMethod() {
        return shortcutData.getActionMethod();
    }

    public String getActionArg() {
        return shortcutData.getActionArg();
    }

    public boolean hasKey() {
        return shortcutData.getKey() != EngineSetting.INDEX_NOT_FOUND;
    }

    public boolean hasAction() {
        return shortcutData.getActionClass() != null;
    }
}
