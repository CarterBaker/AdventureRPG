package editor.runtime.worldeditor.view;

import application.bootstrap.worldpipeline.util.GlobeUtility;
import application.bootstrap.worldpipeline.util.GlobeViewStruct;
import application.bootstrap.worldpipeline.world.GlobeTexelStruct;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.worldeditor.WorldEditorSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3Double;

public class WorldEditorViewSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * Where the world editor looks. A torus world's image is flat: the whole
     * image fits the window when the tab first opens; scrolling zooms about
     * the pointer, from many window pixels per image pixel out to the image
     * shown small, and dragging the pan button, or the apply button while the
     * Hand is the tool, moves it, keeping the view's centre on the image. A
     * globe's image is shown on the globe it maps, a ball of unit radius the
     * window's shared view camera orbits and closes on from its surface, down
     * to a single pixel filling much of the window; a window position is the
     * image position its pick ray strikes on the ball, read by the standard
     * cube map rule, through the window's GlobeViewStruct, which the globe's
     * shader casts the very same rays from. Image positions are pixels with y
     * down, window positions pixels with y up.
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
    private int panButton;
    private float lastMouseX;
    private float lastMouseY;

    // Located — the image position a window position falls on, and on a globe the direction its ray strikes
    private double locatedImageX;
    private double locatedImageY;
    private boolean located;
    private final Vector3Double locatedDirection = new Vector3Double();

    // Globe
    private final GlobeViewStruct globeView = new GlobeViewStruct();
    private final GlobeTexelStruct texelScratch = new GlobeTexelStruct();
    private final Vector3Double locatedScratch = new Vector3Double();
    private float projectedX;
    private float projectedY;

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

        if (isShown()) {
            globeView.resolveCamera(window.getActiveCamera());
            return;
        }

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

        if (!editorInputSystem.isPointerOver())
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

        if (editorInputSystem.isViewClicked(WorldEditorSetting.BUTTON_PAN))
            beginPan(WorldEditorSetting.BUTTON_PAN);
        else if (editorInputSystem.isHandHeld() && editorInputSystem.isViewClicked(WorldEditorSetting.BUTTON_APPLY))
            beginPan(WorldEditorSetting.BUTTON_APPLY);

        if (panning && !rawInput.isButtonHeld(panButton))
            panning = false;

        if (panning) {
            centerX -= (mouseX - lastMouseX) * imagePixelsPerScreenPixel;
            centerY += (mouseY - lastMouseY) * imagePixelsPerScreenPixel;
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    // A pan held for as long as the button pressed stays down
    private void beginPan(int button) {
        this.panning = true;
        this.panButton = button;
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

    // Locate \\

    // The image position under a window position, kept with whether anything of the image lies there
    public void locate(float screenX, float screenY) {

        if (!isShown()) {
            this.locatedImageX = screenToImageX(screenX);
            this.locatedImageY = screenToImageY(screenY);
            this.located = true;
            return;
        }

        WindowInstance window = context.getWindow();

        this.located = globeView.strike(
                window.getActiveCamera(),
                screenX / window.getWidth() * 2f - 1f,
                screenY / window.getHeight() * 2f - 1f,
                locatedDirection);

        if (!located)
            return;

        int faceTexels = worldEditorManager.getWorldImage().getHeight();

        GlobeUtility.locateTexel(locatedDirection.x, locatedDirection.y, locatedDirection.z, faceTexels, texelScratch);

        this.locatedImageX = texelScratch.getFace() * (double) faceTexels + texelScratch.getX();
        this.locatedImageY = texelScratch.getY();
    }

    // Image pixels to one window pixel at the position last located: at a globe face's middle a pixel spans two
    // of the face's plane units over its side, so it spans that much of the ball's surface
    public double resolveImagePixelsPerScreenPixel() {

        if (!isShown())
            return imagePixelsPerScreenPixel;

        double surfacePerScreenPixel = globeView.resolveSpanPerPixel(
                globeView.distanceTo(locatedDirection.x, locatedDirection.y, locatedDirection.z),
                context.getWindow().getHeight());

        return surfacePerScreenPixel * worldEditorManager.getWorldImage().getHeight() * 0.5;
    }

    // Where an image position is drawn in the window, false on a globe for one on the ball's far side
    public boolean project(double imageX, double imageY) {

        if (!isShown()) {
            this.projectedX = imageToScreenX(imageX);
            this.projectedY = imageToScreenY(imageY);
            return true;
        }

        WindowInstance window = context.getWindow();

        GlobeUtility.resolveImageDirection(worldEditorManager.getWorldImage().getHeight(), imageX, imageY,
                locatedScratch);
        locatedScratch.normalize();

        if (!globeView.projectSurface(
                locatedScratch.x, locatedScratch.y, locatedScratch.z, window.getWidth(), window.getHeight()))
            return false;

        this.projectedX = globeView.getProjectedX();
        this.projectedY = globeView.getProjectedY();

        return true;
    }

    // View Camera Subject \\

    // The globe the view camera orbits: only a globe world's image is shown on one
    @Override
    public boolean isShown() {
        return worldEditorManager.getWorldHandle().isGlobe();
    }

    @Override
    public float getCenter(int axis) {
        return 0f;
    }

    @Override
    public float getBoundingRadius() {
        return WorldEditorSetting.GLOBE_RADIUS;
    }

    @Override
    public float getSurfaceRadius() {
        return WorldEditorSetting.GLOBE_RADIUS;
    }

    // Accessible \\

    public boolean isPanning() {
        return panning;
    }

    public double getLocatedImageX() {
        return locatedImageX;
    }

    public double getLocatedImageY() {
        return locatedImageY;
    }

    public boolean isLocated() {
        return located;
    }

    // The globe-frame direction the last window position located strikes the ball at
    public Vector3Double getLocatedDirection() {
        return locatedDirection;
    }

    public float getProjectedX() {
        return projectedX;
    }

    public float getProjectedY() {
        return projectedY;
    }

    // The camera the globe is seen through this frame
    public GlobeViewStruct getGlobeView() {
        return globeView;
    }
}
