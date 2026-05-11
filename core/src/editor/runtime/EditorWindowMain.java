// core/src/editor/runtime/EditorWindowMain.java
package editor.runtime;

import editor.runtime.menueventsmanager.EditorMenuEventsManager;
import engine.root.ContextPackage;

public class EditorWindowMain extends ContextPackage {

    @Override
    protected void create() {
        create(EditorMenuEventsManager.class);
        create(EditorMenuSystem.class);
    }
}
