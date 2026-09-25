package engine.editor.menueventsmanager;

import engine.editor.menueventsmanager.menus.ConsoleBranch;
import engine.editor.menueventsmanager.menus.EditorBranch;
import engine.editor.menueventsmanager.menus.InfoPanelBranch;
import engine.editor.menueventsmanager.menus.ItemEditorBranch;
import engine.editor.menueventsmanager.menus.NameDialogBranch;
import engine.editor.menueventsmanager.menus.TabBranch;
import engine.root.ManagerPackage;

public class EditorMenuEventsManager extends ManagerPackage {

    @Override
    protected void create() {
        create(NameDialogBranch.class);
        create(EditorBranch.class);
        create(TabBranch.class);
        create(ItemEditorBranch.class);
        create(InfoPanelBranch.class);
        create(ConsoleBranch.class);
    }
}
