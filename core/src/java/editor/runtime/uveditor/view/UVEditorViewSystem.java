package editor.runtime.uveditor.view;

import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.uveditor.UVEditorSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;

public class UVEditorViewSystem extends SystemPackage {

    /*
     * Where the UV Editor looks. The unit square of UV space is fitted to
     * the window when the tab first has a size; the wheel zooms about the
     * pointer and dragging the pan button moves the view, unless the tool
     * system took the press. UV runs up and right, as window pixels do, so
     * a UV and a window position map by one scale and offset, handed to the
     * shaders as one vector.
     */

    // Internal
    private EditorInputSystem editorInputSystem;

    // View — the UV at the window's centre and how many window pixels a unit of UV spans
    private double centerU;
    private double centerV;
    private double pixelsPerUnit;
    private boolean fitted;

    // Pan
    private boolean panning;
    private float lastMouseX;
    private float lastMouseY;

    // Mapping
    private Vector4 uvToClip;

    // Base \\

    @Override
    protected void create() {
        this.uvToClip = new Vector4();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        if (window.getWidth() <= 0 || window.getHeight() <= 0)
            return;

        if (!fitted) {
            this.centerU = 0.5;
            this.centerV = 0.5;
            this.pixelsPerUnit = Math.min(window.getWidth(), window.getHeight()) * UVEditorSetting.FIT_SHARE;
            this.fitted = true;
        }

        handleZoom();
        handlePan();
        resolveMapping(window);
    }

    // Input \\

    private void handleZoom() {

        if (!editorInputSystem.isPointerOver())
            return;

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float scroll = rawInput.getScrollY();

        if (scroll == 0f)
            return;

        double zoomed = Math.max(
                UVEditorSetting.MIN_PIXELS_PER_UNIT,
                Math.min(UVEditorSetting.MAX_PIXELS_PER_UNIT,
                        pixelsPerUnit * Math.pow(UVEditorSetting.ZOOM_STEP, scroll)));
        double pointerU = screenToU(rawInput.getMouseX());
        double pointerV = screenToV(rawInput.getMouseY());

        this.centerU = pointerU + (centerU - pointerU) * pixelsPerUnit / zoomed;
        this.centerV = pointerV + (centerV - pointerV) * pixelsPerUnit / zoomed;
        this.pixelsPerUnit = zoomed;
    }

    private void handlePan() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (editorInputSystem.isClicked(UVEditorSetting.BUTTON_PAN)
                && !editorInputSystem.isClaimed(UVEditorSetting.BUTTON_PAN))
            this.panning = true;

        if (!rawInput.isButtonHeld(UVEditorSetting.BUTTON_PAN))
            this.panning = false;

        if (panning) {
            this.centerU -= (mouseX - lastMouseX) / pixelsPerUnit;
            this.centerV -= (mouseY - lastMouseY) / pixelsPerUnit;
        }

        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    // The scale and offset taking a UV to normalized device coordinates, as the shaders read it
    private void resolveMapping(WindowInstance window) {

        float scaleX = (float) (pixelsPerUnit * 2.0 / window.getWidth());
        float scaleY = (float) (pixelsPerUnit * 2.0 / window.getHeight());

        uvToClip.set(scaleX, scaleY, (float) -centerU * scaleX, (float) -centerV * scaleY);
    }

    // Conversion — window pixels, y up, to UV and back \\

    public double screenToU(float screenX) {
        return centerU + (screenX - context.getWindow().getWidth() * 0.5) / pixelsPerUnit;
    }

    public double screenToV(float screenY) {
        return centerV + (screenY - context.getWindow().getHeight() * 0.5) / pixelsPerUnit;
    }

    public float uToScreen(double u) {
        return (float) ((u - centerU) * pixelsPerUnit + context.getWindow().getWidth() * 0.5);
    }

    public float vToScreen(double v) {
        return (float) ((v - centerV) * pixelsPerUnit + context.getWindow().getHeight() * 0.5);
    }

    // Accessible \\

    public Vector4 getUVToClip() {
        return uvToClip;
    }

    public double getPixelsPerUnit() {
        return pixelsPerUnit;
    }

    public boolean isPanning() {
        return panning;
    }
}
