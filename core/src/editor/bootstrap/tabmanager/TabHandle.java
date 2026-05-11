package editor.bootstrap.tabmanager;

import editor.bootstrap.tab.TabContext;
import engine.root.ContextPackage;
import engine.root.HandlePackage;

public class TabHandle extends HandlePackage {

    /*
     * Runtime handle for one registered/open tab. Wraps immutable TabData and
     * carries live context references after TabManager opens the shell and child
     * content contexts.
     */

    // Data
    private TabData tabData;

    // Active
    private TabContext tabContext;
    private ContextPackage contentContext;

    // Internal \\

    public void constructor(TabData tabData) {

        // Data
        this.tabData = tabData;
    }

    // Management \\

    void mount(TabContext tabContext, ContextPackage contentContext) {

        // Active
        this.tabContext = tabContext;
        this.contentContext = contentContext;
    }

    void unmount() {

        // Active
        this.tabContext = null;
        this.contentContext = null;
    }

    // Accessible \\

    public TabData getTabData() {
        return tabData;
    }

    public String getTabTitle() {
        return tabData.getTabTitle();
    }

    public Class<? extends ContextPackage> getContentContextClass() {
        return tabData.getContentContextClass();
    }

    public TabContext getTabContext() {
        return tabContext;
    }

    public ContextPackage getContentContext() {
        return contentContext;
    }

    public boolean isOpen() {
        return tabContext != null && contentContext != null;
    }
}
