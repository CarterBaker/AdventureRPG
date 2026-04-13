package editor.runtime.input;

import editor.runtime.PreviewSystem;
import engine.root.SystemPackage;
import engine.util.input.Bindings;

public class InputSystem extends SystemPackage {

    /*
     * Handles editor keyboard shortcuts each frame.
     * All editor hotkeys live here — never scattered across individual systems.
     */

    // Internal
    private application.bootstrap.inputpipeline.inputsystem.InputSystem inputSystem;
    private PreviewSystem previewSystem;

    // Internal \\

    @Override
    protected void get() {
        this.inputSystem = get(application.bootstrap.inputpipeline.inputsystem.InputSystem.class);
        this.previewSystem = get(PreviewSystem.class);
    }

    @Override
    protected void update() {
        if (inputSystem.bindingClicked(Bindings.OPEN_PREVIEW))
            previewSystem.openPreview();
    }
}