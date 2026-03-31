package com.internal.editor.runtime;

import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.inputpipeline.inputsystem.KeyCode;
import com.internal.core.engine.SystemPackage;

public class EditorInputSystem extends SystemPackage {

    /*
     * Handles editor keyboard shortcuts each frame. Key 1 opens a new
     * game preview window. All editor hotkeys live here — never scattered
     * across individual systems.
     */

    // Internal
    private InputSystem inputSystem;
    private PreviewSystem previewSystem;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.inputSystem = get(InputSystem.class);
        this.previewSystem = get(PreviewSystem.class);
    }

    @Override
    protected void update() {

        if (inputSystem.keyJustPressed(KeyCode.NUM_1))
            previewSystem.openPreview();
    }
}
