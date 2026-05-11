package editor.bootstrap.tabmanager;

import engine.root.ContextPackage;
import engine.root.DataPackage;

public class TabData extends DataPackage {

    /*
     * Immutable tab definition. Holds the user-facing tab title and the child
     * ContextPackage class that the tab manager will mount inside a TabContext
     * shell when the tab is opened.
     */

    // Identity
    private final String tabTitle;

    // Content
    private final Class<? extends ContextPackage> contentContextClass;

    // Internal \\

    public TabData(
            String tabTitle,
            Class<? extends ContextPackage> contentContextClass) {

        // Identity
        this.tabTitle = tabTitle;

        // Content
        this.contentContextClass = contentContextClass;
    }

    // Accessible \\

    public String getTabTitle() {
        return tabTitle;
    }

    public Class<? extends ContextPackage> getContentContextClass() {
        return contentContextClass;
    }
}
