package editor.bootstrap.tab;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ContextPackage;
import engine.root.HandlePackage;

public class TabHandle extends HandlePackage {

    /*
     * Runtime handle for one registered open tab. Wraps immutable TabData and
     * carries live context references after TabManager creates both the shell
     * and content contexts. isOpen() gates all close-path logic in TabManager.
     *
     * getWindow() is a convenience shortcut to the content window used by
     * InputSystem's resolveInputAuthority — callers should prefer this over
     * getContentContext().getWindow() to keep the indirection in one place.
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

    public void mount(TabContext tabContext, ContextPackage contentContext) {
        // Active
        this.tabContext = tabContext;
        this.contentContext = contentContext;
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

    public WindowInstance getWindow() {
        return contentContext != null ? contentContext.getWindow() : null;
    }

    public boolean isOpen() {
        return tabContext != null && contentContext != null;
    }
}