package editor.runtime;

import editor.runtime.input.InputSystem;
import engine.root.ContextPackage;

public class RuntimeContext extends ContextPackage {

    /*
     * Editor runtime entry point. Creates and owns all editor systems.
     * Paired with the main window by EditorEngine. Owns the PreviewSystem
     * which can spawn additional game preview contexts at runtime, each
     * paired with their own window.
     */

    // Editor
    private InputSystem editorInputSystem;
    private PreviewSystem previewSystem;

    // Internal \\

    @Override
    protected void create() {

        // Editor
        this.previewSystem = create(PreviewSystem.class);
        this.editorInputSystem = create(InputSystem.class);
    }
}