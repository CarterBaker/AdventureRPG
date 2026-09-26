package editor.bootstrap.tabpipeline.tab;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ContextPackage;
import engine.root.HandlePackage;
import engine.util.registry.RegistryUtility;

public class TabHandle extends HandlePackage {

    /*
     * Handle for one open tab. Wraps TabData and the live TabContext that owns
     * chrome and content; content is reached through the tab context, and
     * getWindow() is the input-authority shortcut to the content window.
     */

    // Data
    private TabData tabData;

    // Active — TabContext owns both chrome and content
    private TabContext tabContext;

    // Internal \\

    public void constructor(TabData tabData) {
        this.tabData = tabData;
    }

    // Management \\

    public void mount(TabContext tabContext) {
        this.tabContext = tabContext;
    }

    // Accessible \\

    public TabData getTabData() {
        return tabData;
    }

    public String getTabTitle() {
        return tabData.getTabTitle();
    }

    public int getTabId() {
        return RegistryUtility.toIntID(getTabTitle());
    }

    public Class<? extends ContextPackage> getContentContextClass() {
        return tabData.getContentContextClass();
    }

    public TabContext getTabContext() {
        return tabContext;
    }

    public WindowInstance getWindow() {

        if (tabContext == null)
            return null;

        ContextPackage content = tabContext.getContentContext();
        return content != null ? content.getWindow() : null;
    }

    public boolean isOpen() {
        return tabContext != null;
    }
}