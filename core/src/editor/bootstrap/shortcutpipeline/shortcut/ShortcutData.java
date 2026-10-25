package editor.bootstrap.shortcutpipeline.shortcut;

import engine.root.ContextPackage;
import engine.root.DataPackage;

public class ShortcutData extends DataPackage {

    /*
     * Immutable definition of one editor shortcut: its name, qualified by the
     * shortcut file it is declared in, and its registry ID; the tool context
     * it works in; the key it is pressed with, if any, and the mask of
     * modifiers held with it; and the class, method and argument it runs,
     * resolved as a menu button's action is. A held shortcut has no action;
     * its tool reads it while a press or a drag is under way.
     */

    // Identity
    private final String shortcutName;
    private final int shortcutID;

    // Scope
    private final Class<? extends ContextPackage> contextClass;

    // Chord
    private final int key;
    private final int modifiers;

    // Action
    private final String actionClass;
    private final String actionMethod;
    private final String actionArg;

    // Constructor \\

    public ShortcutData(
            String shortcutName,
            int shortcutID,
            Class<? extends ContextPackage> contextClass,
            int key,
            int modifiers,
            String actionClass,
            String actionMethod,
            String actionArg) {

        // Identity
        this.shortcutName = shortcutName;
        this.shortcutID = shortcutID;

        // Scope
        this.contextClass = contextClass;

        // Chord
        this.key = key;
        this.modifiers = modifiers;

        // Action
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
    }

    // Accessible \\

    public String getShortcutName() {
        return shortcutName;
    }

    public int getShortcutID() {
        return shortcutID;
    }

    public Class<? extends ContextPackage> getContextClass() {
        return contextClass;
    }

    public int getKey() {
        return key;
    }

    public int getModifiers() {
        return modifiers;
    }

    public String getActionClass() {
        return actionClass;
    }

    public String getActionMethod() {
        return actionMethod;
    }

    public String getActionArg() {
        return actionArg;
    }
}
