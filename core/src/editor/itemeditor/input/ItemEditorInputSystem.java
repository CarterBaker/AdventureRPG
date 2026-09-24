package editor.itemeditor.input;

import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.inputpipeline.inputmanager.InputManager;
import engine.root.SystemPackage;

public class ItemEditorInputSystem extends SystemPackage {

    /*
     * Owns and writes this context's RawInputHandle first each frame, so the
     * camera and tool systems read the same snapshot.
     */

    // Internal
    private InputManager inputManager;

    // Raw Input
    private RawInputHandle rawInputHandle;

    // Base \\

    @Override
    protected void create() {
        this.rawInputHandle = create(RawInputHandle.class);
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
    }

    // Update \\

    @Override
    protected void update() {
        inputManager.writeRawInput(rawInputHandle, context.getWindow());
    }

    // Accessible \\

    public RawInputHandle getRawInputHandle() {
        return rawInputHandle;
    }
}
