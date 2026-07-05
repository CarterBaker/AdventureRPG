package editor.bootstrap.tabpipeline.tab;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ContextPackage;
import engine.root.HandlePackage;
import engine.util.registry.RegistryUtility;

public class TabHandle extends HandlePackage {

    /*
     * Runtime handle for one registered open tab. Wraps immutable TabData and
     * carries the live TabContext, which owns both chrome and content.
     *
     * All access to content goes through getTabContext().getContentContext()
     * so the tab remains the single unit of ownership. getWindow() is a
     * convenience shortcut to the content window for InputSystem's authority
     * resolver — the only place direct content window access is appropriate.
     *
     * isOpen() gates all close-path logic in TabManager.
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

    /*
     * Stable numeric identity for this tab, derived from its title. Titles
     * are guaranteed unique among currently open tabs — TabManager.hasTab()
     * enforces this at open time — and never change afterward, so this
     * value is stable for the tab's entire lifetime with no extra state:
     * it's the exact same identity TabManager's own tabName2TabID table
     * already keys on internally, just exposed here as a formula instead
     * of a second cached copy. LayoutManager persists this so a BSP leaf
     * can reference a specific tab directly, independent of array position
     * — the one thing that actually needs to survive a restore where some
     * other tab among several might fail to reopen.
     */
    public int getTabId() {
        return RegistryUtility.toIntID(getTabTitle());
    }

    public Class<? extends ContextPackage> getContentContextClass() {
        return tabData.getContentContextClass();
    }

    public TabContext getTabContext() {
        return tabContext;
    }

    /*
     * Convenience for InputSystem's authority resolver. Content window is the
     * input authority — accessed through the TabContext ownership chain.
     */
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