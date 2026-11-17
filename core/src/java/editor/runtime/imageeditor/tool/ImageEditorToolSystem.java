package editor.runtime.imageeditor.tool;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.imageeditorpipeline.imageeditormanager.ImageEditorManager;
import editor.bootstrap.imageeditorpipeline.imageproject.ImageProjectInstance;
import editor.runtime.EditorSetting;
import editor.runtime.imageeditor.ImageEditorSetting;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.uveditor.view.UVEditorViewSystem;
import engine.root.SystemPackage;

public class ImageEditorToolSystem extends SystemPackage {

    /*
     * Reads the Image Editor's pointer for its tools: a press of the paint
     * button over the image begins the tool's work at the pixel under it,
     * dragging carries a stroke on and letting go ends it, unless the view
     * took the press to pan. Pixels count from the image's top row, as its
     * rows are held; the view spans the image's longer side as one unit.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private UVEditorViewSystem uvEditorViewSystem;
    private ImageEditorManager imageEditorManager;

    // Stroke
    private boolean stroking;

    // Base \\

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.uvEditorViewSystem = get(UVEditorViewSystem.class);
        this.imageEditorManager = get(ImageEditorManager.class);
    }

    @Override
    protected void dispose() {
        finish();
    }

    // Update \\

    @Override
    protected void update() {

        if (!imageEditorManager.hasActiveImage()) {
            this.stroking = false;
            return;
        }

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (stroking) {

            if (!rawInput.isButtonHeld(ImageEditorSetting.BUTTON_PAINT)) {
                finish();
                return;
            }

            imageEditorManager.continueStroke(toPixelX(mouseX), toPixelY(mouseY));
            return;
        }

        if (imageEditorManager.armsStamp() && editorInputSystem.isPointerActive()
                && rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)) {
            imageEditorManager.disarmStamp();
            return;
        }

        if (uvEditorViewSystem.isPanning() || !editorInputSystem.isClicked(ImageEditorSetting.BUTTON_PAINT))
            return;

        editorInputSystem.claim(ImageEditorSetting.BUTTON_PAINT);
        this.stroking = true;
        imageEditorManager.beginStroke(toPixelX(mouseX), toPixelY(mouseY));
    }

    private void finish() {

        if (!stroking)
            return;

        imageEditorManager.endStroke();
        this.stroking = false;
    }

    // Conversion — window pixels, y up, to image pixels, rows top first \\

    private int toPixelX(float screenX) {
        return (int) Math.floor(uvEditorViewSystem.screenToU(screenX) * getSide());
    }

    private int toPixelY(float screenY) {

        ImageProjectInstance project = imageEditorManager.getActiveProject();

        return (int) Math.floor(project.getHeight() - uvEditorViewSystem.screenToV(screenY) * getSide());
    }

    // The image's longer side, the one unit of the view spans
    private int getSide() {

        ImageProjectInstance project = imageEditorManager.getActiveProject();

        return Math.max(project.getWidth(), project.getHeight());
    }
}
