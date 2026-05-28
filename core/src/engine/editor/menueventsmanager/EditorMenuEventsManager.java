package engine.editor.menueventsmanager;

import engine.editor.menueventsmanager.menus.EditorBranch;
import engine.editor.menueventsmanager.menus.TabBranch;
import engine.root.ManagerPackage;

public class EditorMenuEventsManager extends ManagerPackage {

    @Override
    protected void create() {
        create(EditorBranch.class);
        create(TabBranch.class);
    }
}
