package editor.runtime;

import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import engine.root.SystemPackage;

public class EditorInputSystem extends SystemPackage {

    /*
     * Handles editor keyboard shortcuts each frame.
     * All editor hotkeys live here — never scattered across individual systems.
     */

    // Internal
    private InputSystem inputSystem;
    private PreviewSystem previewSystem;

    // Internal \\

    @Override
    protected void get() {
        this.inputSystem = get(InputSystem.class);
        this.previewSystem = get(PreviewSystem.class);
    }

    @Override
    protected void update() {
        if (inputSystem.bindingJustPressed(Bindings.OPEN_PREVIEW))
            previewSystem.openPreview();
    }
}