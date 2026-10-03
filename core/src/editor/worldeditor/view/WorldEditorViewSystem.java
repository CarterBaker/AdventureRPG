package editor.worldeditor.view;

import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorInputSystem;
import editor.worldeditor.WorldEditorSetting;
import engine.root.SystemPackage;

public class WorldEditorViewSystem extends SystemPackage {

    /*
     * Where the world editor looks. The whole image fits the window when the
     * tab first opens; scrolling zooms about the pointer, from many window
     * pixels per image pixel out to the image shown small, and dragging the
     * pan button moves it, keeping the view's centre on the image. Image
     * positions are pixels with y down, window positions pixels with y up.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private WorldEditorManager worldEditorManager;

    // View
    private double centerX;
    private double centerY;
    private double imagePixelsPerScreenPixel;
    private boolean fitted;

    // Pan
    private boolean panning;
    private float lastMouseX;
    private float lastMouseY;

    // Base \\

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (window.getWidth() <= 0 || window.getHeight() <= 0)
            return;

        ImageDocumentInstance image = worldEditorManager.getWorldImage();

        if (!fitted) {
            centerX = image.getWidth() * 0.5;
            centerY = image.getHeight() * 0.5;
            imagePixelsPerScreenPixel = resolveFitZoom(image, window);
            fitted = true;
        }

        handleZoom(image, window);
        handlePan();

        centerX = Math.max(0.0, Math.min(image.getWidth(), centerX));
        centerY = Math.max(0.0, Math.min(image.getHeight(), centerY));
    }

    private double resolveFitZoom(ImageDocumentInstance image, WindowInstance window) {
        return Math.max(
                image.getWidth() / (double) window.getWidth(),
                image.getHeight() / (double) window.getHeight());
    }

    private double clampZoom(ImageDocumentInstance image, WindowInstance window, double zoom) {
        return Math.max(
                WorldEditorSetting.MIN_IMAGE_PIXELS_PER_SCREEN_PIXEL,
                Math.min(resolveFitZoom(image, window) * WorldEditorSetting.MAX_FIT_MULTIPLE, zoom));
    }

    // Input \\

    private void handleZoom(ImageDocumentInstance image, WindowInstance window) {

        if (!editorInputSystem.isPointerActive())
            return;

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float scroll = rawInput.getScrollY();

        if (scroll == 0f)
            return;

        double zoomed = clampZoom(
                image, window, imagePixelsPerScreenPixel * Math.pow(WorldEditorSetting.ZOOM_STEP, -scroll));
        double pointerX = screenToImageX(rawInput.getMouseX());
        double pointerY = screenToImageY(rawInput.getMouseY());

        centerX = pointerX + (centerX - pointerX) * zoomed / imagePixelsPerScreenPixel;
        centerY = pointerY + (centerY - pointerY) * zoomed / imagePixelsPerScreenPixel;
        imagePixelsPerScreenPixel = zoomed;
    }

    private void handlePan() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (editorInputSystem.isClicked(WorldEditorSetting.BUTTON_PAN))
            panning = true;

        if (!rawInput.isButtonHeld(WorldEditorSetting.BUTTON_PAN))
            panning = false;

        if (panning) {
            centerX -= (mouseX - lastMouseX) * imagePixelsPerScreenPixel;
            centerY += (mouseY - lastMouseY) * imagePixelsPerScreenPixel;
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    // Conversion — window pixels, y up, to image pixels, y down, and back \\

    public double screenToImageX(float screenX) {
        return centerX + (screenX - context.getWindow().getWidth() * 0.5) * imagePixelsPerScreenPixel;
    }

    public double screenToImageY(float screenY) {
        return centerY - (screenY - context.getWindow().getHeight() * 0.5) * imagePixelsPerScreenPixel;
    }

    public float imageToScreenX(double imageX) {
        return (float) ((imageX - centerX) / imagePixelsPerScreenPixel + context.getWindow().getWidth() * 0.5);
    }

    public float imageToScreenY(double imageY) {
        return (float) (context.getWindow().getHeight() * 0.5 - (imageY - centerY) / imagePixelsPerScreenPixel);
    }

    // Accessible \\

    public boolean isPanning() {
        return panning;
    }
}
