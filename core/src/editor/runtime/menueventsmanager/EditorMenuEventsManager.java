package editor.runtime.menueventsmanager;

import editor.runtime.menueventsmanager.menus.CommandConsoleBranch;
import editor.runtime.menueventsmanager.menus.ConsoleBranch;
import editor.runtime.menueventsmanager.menus.EditorBranch;
import editor.runtime.menueventsmanager.menus.InfoPanelBranch;
import editor.runtime.menueventsmanager.menus.ItemEditorBranch;
import editor.runtime.menueventsmanager.menus.NameDialogBranch;
import editor.runtime.menueventsmanager.menus.TabBranch;
import engine.root.ManagerPackage;

public class EditorMenuEventsManager extends ManagerPackage {

    /*
     * Owns the editor's menu callback branches, shared by every editor window.
     */

    @Override
    protected void create() {
        create(NameDialogBranch.class);
        create(EditorBranch.class);
        create(TabBranch.class);
        create(ItemEditorBranch.class);
        create(InfoPanelBranch.class);
        create(ConsoleBranch.class);
        create(CommandConsoleBranch.class);
    }
}
