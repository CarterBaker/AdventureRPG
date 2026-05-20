package editor.runtime.editor.menueventsmanager;

import editor.runtime.editor.menueventsmanager.menus.EditorBranch;
import editor.runtime.editor.menueventsmanager.menus.TabBranch;
import engine.root.ManagerPackage;

public class EditorMenuEventsManager extends ManagerPackage {

    @Override
    protected void create() {
        create(EditorBranch.class);
        create(TabBranch.class);
    }
}
