package editor.bootstrap.tabpipeline.tab;

import engine.root.ContextPackage;
import engine.root.DataPackage;

public class TabData extends DataPackage {

    /*
     * Immutable tab definition. Holds the user-facing tab title, the ID the tab
     * manager registered it under, and the child ContextPackage class that the
     * tab manager will mount inside a TabContext shell when the tab is opened.
     */

    // Identity
    private final String baseTitle;
    private final String tabTitle;
    private final int tabID;

    // Content
    private final Class<? extends ContextPackage> contentContextClass;

    // Internal \\

    public TabData(
            String baseTitle,
            String tabTitle,
            int tabID,
            Class<? extends ContextPackage> contentContextClass) {

        // Identity
        this.baseTitle = baseTitle;
        this.tabTitle = tabTitle;
        this.tabID = tabID;

        // Content
        this.contentContextClass = contentContextClass;
    }

    // Accessible \\

    public String getBaseTitle() {
        return baseTitle;
    }

    public String getTabTitle() {
        return tabTitle;
    }

    public int getTabID() {
        return tabID;
    }

    public Class<? extends ContextPackage> getContentContextClass() {
        return contentContextClass;
    }
}
