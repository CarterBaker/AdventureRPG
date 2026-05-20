// core/src/editor/runtime/EditorWindowSecondary.java
package editor.runtime.editor;

import engine.root.ContextPackage;

public class EditorWindowSecondary extends ContextPackage {

    @Override
    protected void create() {
        create(MenuTargetFboSystem.class);
    }
}
