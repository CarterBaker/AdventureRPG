package editor.runtime.menueventsmanager.menus.search;

import engine.root.StructPackage;

public class EditorSearchEntryStruct extends StructPackage {

    /*
     * One item the editor's search finds: a toolbar menu's item as its menu
     * declares it, the name it shows, where it stands (the menu and the
     * section above it) and the action it runs, the action its menu item
     * runs.
     */

    // Identity
    private final String label;
    private final String path;

    // Action
    private final String actionClass;
    private final String actionMethod;
    private final String actionArg;

    // Constructor \\

    public EditorSearchEntryStruct(
            String label,
            String path,
            String actionClass,
            String actionMethod,
            String actionArg) {

        // Identity
        this.label = label;
        this.path = path;

        // Action
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.actionArg = actionArg;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public String getPath() {
        return path;
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
