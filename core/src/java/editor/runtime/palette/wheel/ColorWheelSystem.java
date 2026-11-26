package editor.runtime.palette.wheel;

import java.nio.ByteBuffer;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.runtime.EditorSetting;
import editor.runtime.palette.PaletteSetting;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.memory.BufferUtility;

public class ColorWheelSystem extends SystemPackage {

    /*
     * The Palette tab's colour wheel, kept in step with the paint colour while
     * a wheel element is bound to it: the wheel image drawn again only when
     * the colour moves (a hue ring,
     * inside it a triangle turned so its first corner holds the hue at full
     * strength, its others white and black, and a marker on each where the
     * colour stands); the colour's sliders are the Properties tab's. A press
     * on the ring turns
     * the hue as far round as the pointer is; a press in the triangle sets
     * saturation and brightness where the pointer is, held to the triangle
     * while the drag goes on. Image rows run bottom first, as the layout's y
     * does, and the image is never allocated again after it opens.
     */

    // Internal
    private TextureManager textureManager;
    private InputManager inputManager;
    private CharacterEditorManager characterEditorManager;

    // Wheel — the element showing it, its texture, the colour it shows, and its texels
    private ElementInstance wheelElement;
    private int wheelTexture;
    private float shownHue;
    private float shownSaturation;
    private float shownBrightness;
    private ByteBuffer texels;

    // Drag — whether a press is held, and what it began on: none, the ring or the triangle
    private boolean dragging;
    private int dragPart;

    // Scratch
    private Color colorScratch;
    private float[] corners;
    private float[] weights;

    // Base \\

    @Override
    protected void create() {

        int size = PaletteSetting.WHEEL_SIZE;

        // Wheel
        this.texels = BufferUtility.newByteBuffer(size * size * EngineSetting.COLOR_CHANNEL_COUNT);
        this.shownHue = Float.NaN;

        // Scratch
        this.colorScratch = new Color();
        this.corners = new float[PaletteSetting.WHEEL_CORNER_COUNT * 2];
        this.weights = new float[PaletteSetting.WHEEL_CORNER_COUNT];
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.inputManager = get(InputManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    @Override
    protected void awake() {

        int size = PaletteSetting.WHEEL_SIZE;

        this.wheelTexture = textureManager.createTexture2D(
                size, size, EngineSetting.GL_CLAMP_TO_EDGE, EngineSetting.GL_LINEAR);
    }

    @Override
    protected void dispose() {

        if (wheelTexture != 0)
            textureManager.deleteTexture2D(wheelTexture);

        this.wheelTexture = 0;
    }

    // Update \\

    @Override
    protected void update() {

        if (wheelElement == null)
            return;

        if (dragging && !inputManager.getRawInput(context.getWindow()).isMouseDown(EditorSetting.COMMAND_DRAG_BUTTON))
            this.dragging = false;

        refreshWheel();
    }

    // Management \\

    // The element the wheel is shown on, given the wheel's image; null once that element is taken away
    public void bind(ElementInstance wheel) {

        this.wheelElement = wheel;
        this.dragging = false;

        if (wheel != null && wheel.hasSprite())
            wheel.getSpriteInstance().setTexture(wheelTexture);
    }

    private void refreshWheel() {

        float hue = characterEditorManager.getPaintHSB(0);
        float saturation = characterEditorManager.getPaintHSB(1);
        float brightness = characterEditorManager.getPaintHSB(2);

        if (hue == shownHue && saturation == shownSaturation && brightness == shownBrightness)
            return;

        this.shownHue = hue;
        this.shownSaturation = saturation;
        this.shownBrightness = brightness;
        draw(hue, saturation, brightness);
    }

    // Drag \\

    // The press's part chosen on its first frame, then the colour set from the pointer every frame it stays down
    public void drag(ElementInstance wheel) {

        WindowInstance window = context.getWindow();
        float half = PaletteSetting.WHEEL_SIZE * 0.5f;
        float scale = PaletteSetting.WHEEL_SIZE / Math.max(1f, wheel.getComputedW());
        float x = (inputManager.getHoverMouseX(window) - wheel.getComputedLeft()) * scale - half;
        float y = (inputManager.getHoverMouseY(window) - wheel.getComputedTop()) * scale - half;

        resolveCorners(shownHue);

        if (!dragging) {
            this.dragging = true;
            this.dragPart = resolvePart(x, y);
        }

        if (dragPart == PaletteSetting.PART_RING)
            turnHue(x, y);
        else if (dragPart == PaletteSetting.PART_TRIANGLE)
            pickInTriangle(x, y);
    }

    private int resolvePart(float x, float y) {

        float half = PaletteSetting.WHEEL_SIZE * 0.5f;
        float distance = (float) Math.hypot(x, y);

        if (distance >= PaletteSetting.WHEEL_RING_INNER * half
                && distance <= PaletteSetting.WHEEL_RING_OUTER * half)
            return PaletteSetting.PART_RING;

        resolveWeights(x, y);

        return weights[0] >= 0f && weights[1] >= 0f && weights[2] >= 0f
                ? PaletteSetting.PART_TRIANGLE
                : PaletteSetting.PART_NONE;
    }

    private void turnHue(float x, float y) {

        float turn = (float) (Math.atan2(y, x) / (Math.PI * 2.0));

        characterEditorManager.setPaintHSB(turn - (float) Math.floor(turn), shownSaturation, shownBrightness);
    }

    // The pointer held to the triangle, then read as saturation (the hue corner's share of the colour) and
    // brightness (all but the black corner's)
    private void pickInTriangle(float x, float y) {

        resolveWeights(x, y);

        float hueWeight = Math.max(0f, weights[0]);
        float whiteWeight = Math.max(0f, weights[1]);
        float blackWeight = Math.max(0f, weights[2]);
        float total = hueWeight + whiteWeight + blackWeight;

        if (total <= 0f)
            return;

        hueWeight /= total;
        whiteWeight /= total;

        float brightness = hueWeight + whiteWeight;
        float saturation = brightness > 0f ? hueWeight / brightness : 0f;

        characterEditorManager.setPaintHSB(shownHue, saturation, brightness);
    }

    // Draw \\

    // Every texel of the ring, the triangle and the markers, then the image uploaded whole
    private void draw(float hue, float saturation, float brightness) {

        int size = PaletteSetting.WHEEL_SIZE;
        float half = size * 0.5f;
        float outer = PaletteSetting.WHEEL_RING_OUTER * half;
        float inner = PaletteSetting.WHEEL_RING_INNER * half;
        float soft = PaletteSetting.WHEEL_EDGE_SOFTNESS;

        resolveCorners(hue);

        float hueWeight = saturation * brightness;
        float whiteWeight = brightness - hueWeight;
        float blackWeight = 1f - brightness;
        float markX = corners[0] * hueWeight + corners[2] * whiteWeight + corners[4] * blackWeight;
        float markY = corners[1] * hueWeight + corners[3] * whiteWeight + corners[5] * blackWeight;
        float ringMiddle = (outer + inner) * 0.5f;
        double hueAngle = hue * Math.PI * 2.0;
        float hueX = (float) Math.cos(hueAngle) * ringMiddle;
        float hueY = (float) Math.sin(hueAngle) * ringMiddle;
        boolean darkMark = brightness > PaletteSetting.WHEEL_MARKER_DARK_ABOVE
                && saturation < PaletteSetting.WHEEL_MARKER_DARK_SATURATION;

        texels.clear();

        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {

                float x = column + 0.5f - half;
                float y = row + 0.5f - half;
                float distance = (float) Math.hypot(x, y);
                float r = 0f;
                float g = 0f;
                float b = 0f;
                float a = 0f;

                float ring = Math.min(edge(outer - distance, soft), edge(distance - inner, soft));

                if (ring > 0f) {
                    float turn = (float) (Math.atan2(y, x) / (Math.PI * 2.0));
                    colorScratch.setHSB(turn - (float) Math.floor(turn), 1f, 1f);
                    r = colorScratch.r;
                    g = colorScratch.g;
                    b = colorScratch.b;
                    a = ring;
                }

                resolveWeights(x, y);
                float inside = edge(Math.min(weights[0], Math.min(weights[1], weights[2])) * getTriangleHeight(), soft);

                if (inside > 0f) {
                    colorScratch.setHSB(hue, 1f, 1f);
                    r = colorScratch.r * weights[0] + weights[1];
                    g = colorScratch.g * weights[0] + weights[1];
                    b = colorScratch.b * weights[0] + weights[1];
                    a = Math.max(a, inside);
                }

                float marker = Math.max(
                        ring(x - hueX, y - hueY),
                        ring(x - markX, y - markY));

                if (marker > 0f) {
                    float tone = darkMark && isNear(x - markX, y - markY) ? 0f : 1f;
                    r = r + (tone - r) * marker;
                    g = g + (tone - g) * marker;
                    b = b + (tone - b) * marker;
                    a = Math.max(a, marker);
                }

                texels.put(toByte(r)).put(toByte(g)).put(toByte(b)).put(toByte(a));
            }
        }

        texels.flip();
        textureManager.updateTexture2D(wheelTexture, 0, 0, size, size, texels);
    }

    // A marker's outline, as strong as the texel lies on it
    private float ring(float dx, float dy) {
        float distance = Math.abs((float) Math.hypot(dx, dy) - PaletteSetting.WHEEL_MARKER_RADIUS);
        return edge(PaletteSetting.WHEEL_MARKER_WIDTH - distance, PaletteSetting.WHEEL_EDGE_SOFTNESS);
    }

    private boolean isNear(float dx, float dy) {
        float reach = PaletteSetting.WHEEL_MARKER_RADIUS + PaletteSetting.WHEEL_MARKER_WIDTH * 2f;
        return dx * dx + dy * dy <= reach * reach;
    }

    // Triangle \\

    // The triangle's corners about the middle: the hue, then white and black a third and two thirds of a turn on
    private void resolveCorners(float hue) {

        float radius = PaletteSetting.WHEEL_TRIANGLE_RADIUS * PaletteSetting.WHEEL_SIZE * 0.5f;

        setCorner(0, hue, radius);
        setCorner(1, hue + PaletteSetting.WHEEL_WHITE_TURN, radius);
        setCorner(2, hue + PaletteSetting.WHEEL_BLACK_TURN, radius);
    }

    private void setCorner(int corner, float turn, float radius) {

        double angle = turn * Math.PI * 2.0;

        corners[corner * 2] = (float) Math.cos(angle) * radius;
        corners[corner * 2 + 1] = (float) Math.sin(angle) * radius;
    }

    // A point's barycentric weights on the hue, white and black corners
    private void resolveWeights(float x, float y) {

        float ax = corners[0];
        float ay = corners[1];
        float bx = corners[2];
        float by = corners[3];
        float cx = corners[4];
        float cy = corners[5];
        float area = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy);

        weights[0] = ((by - cy) * (x - cx) + (cx - bx) * (y - cy)) / area;
        weights[1] = ((cy - ay) * (x - cx) + (ax - cx) * (y - cy)) / area;
        weights[2] = 1f - weights[0] - weights[1];
    }

    // The triangle's height in texels, turning a weight into a distance from the edge
    private float getTriangleHeight() {
        return PaletteSetting.WHEEL_TRIANGLE_RADIUS * PaletteSetting.WHEEL_SIZE * 0.5f
                * PaletteSetting.WHEEL_TRIANGLE_HEIGHT_SHARE;
    }

    // Utility \\

    // How far inside an edge a texel is, faded over the softness
    private float edge(float distance, float softness) {
        return Math.clamp(distance / softness + 0.5f, 0f, 1f);
    }

    private byte toByte(float value) {
        return (byte) Math.round(Math.clamp(value, 0f, 1f) * EngineSetting.PACKED_COLOR_CHANNEL_MASK);
    }
}
