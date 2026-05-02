package editor.runtime.menueventsmanager;

import application.bootstrap.menupipeline.menueventsmanager.menus.EditorBranch;
import engine.root.ManagerPackage;

public class EditorMenuEventsManager extends ManagerPackage {

    @Override
    protected void create() {
        create(EditorBranch.class);
    }
}
