package engine.graphics.color;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class Color extends EngineUtility {

    /*
     * Mutable RGBA color with float channels, named constants, and packing to
     * and from integer formats.
     */

    // Static Colors \\

    public static final Color WHITE = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    public static final Color BLACK = new Color(0.0f, 0.0f, 0.0f, 1.0f);
    public static final Color RED = new Color(1.0f, 0.0f, 0.0f, 1.0f);
    public static final Color GREEN = new Color(0.0f, 1.0f, 0.0f, 1.0f);
    public static final Color BLUE = new Color(0.0f, 0.0f, 1.0f, 1.0f);
    public static final Color YELLOW = new Color(1.0f, 1.0f, 0.0f, 1.0f);
    public static final Color CYAN = new Color(0.0f, 1.0f, 1.0f, 1.0f);
    public static final Color MAGENTA = new Color(1.0f, 0.0f, 1.0f, 1.0f);
    public static final Color GRAY = new Color(0.5f, 0.5f, 0.5f, 1.0f);
    public static final Color CLEAR = new Color(0.0f, 0.0f, 0.0f, 0.0f);

    // Data
    public float r, g, b, a;

    // Constructors \\

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public Color(float r, float g, float b) {
        this(r, g, b, 1.0f);
    }

    public Color() {
        this(0, 0, 0, 1.0f);
    }

    public Color(float gray) {
        this(gray, gray, gray, 1.0f);
    }

    public Color(Color other) {
        this(other.r, other.g, other.b, other.a);
    }

    // Conversion \\

    public float[] toArray() {
        return new float[] { r, g, b, a };
    }

    // Set \\

    public Color set(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    public Color set(float r, float g, float b) {
        return set(r, g, b, 1.0f);
    }

    public Color set(float gray) {
        return set(gray, gray, gray, 1.0f);
    }

    public Color set(Color other) {
        return set(other.r, other.g, other.b, other.a);
    }

    // Addition \\

    public Color add(float r, float g, float b, float a) {
        this.r += r;
        this.g += g;
        this.b += b;
        this.a += a;
        return this;
    }

    public Color add(float value) {
        return add(value, value, value, value);
    }

    public Color add(Color other) {
        return add(other.r, other.g, other.b, other.a);
    }

    // Subtraction \\

    public Color subtract(float r, float g, float b, float a) {
        this.r -= r;
        this.g -= g;
        this.b -= b;
        this.a -= a;
        return this;
    }

    public Color subtract(float value) {
        return subtract(value, value, value, value);
    }

    public Color subtract(Color other) {
        return subtract(other.r, other.g, other.b, other.a);
    }

    // Multiplication \\

    public Color multiply(float r, float g, float b, float a) {
        this.r *= r;
        this.g *= g;
        this.b *= b;
        this.a *= a;
        return this;
    }

    public Color multiply(float scalar) {
        return multiply(scalar, scalar, scalar, scalar);
    }

    public Color multiply(Color other) {
        return multiply(other.r, other.g, other.b, other.a);
    }

    // Division \\

    public Color divide(float r, float g, float b, float a) {
        if (r == 0 || g == 0 || b == 0 || a == 0)
            throwException("Division by zero");
        this.r /= r;
        this.g /= g;
        this.b /= b;
        this.a /= a;
        return this;
    }

    public Color divide(float scalar) {
        return divide(scalar, scalar, scalar, scalar);
    }

    public Color divide(Color other) {
        return divide(other.r, other.g, other.b, other.a);
    }

    // Color Operations \\

    public Color clamp() {
        this.r = Math.max(0.0f, Math.min(1.0f, r));
        this.g = Math.max(0.0f, Math.min(1.0f, g));
        this.b = Math.max(0.0f, Math.min(1.0f, b));
        this.a = Math.max(0.0f, Math.min(1.0f, a));
        return this;
    }

    public Color lerp(Color target, float t) {
        this.r += t * (target.r - this.r);
        this.g += t * (target.g - this.g);
        this.b += t * (target.b - this.b);
        this.a += t * (target.a - this.a);
        return this;
    }

    // Hue, Saturation, Brightness \\

    // The hue as a share of the colour wheel, 0 to 1, red at 0; 0 for a grey
    public float getHue() {

        float max = Math.max(r, Math.max(g, b));
        float range = max - Math.min(r, Math.min(g, b));

        if (range <= 0f)
            return 0f;

        float sector = max == r ? (g - b) / range : max == g ? (b - r) / range + 2f : (r - g) / range + 4f;
        float hue = sector / 6f;

        return hue < 0f ? hue + 1f : hue;
    }

    public float getSaturation() {
        float max = Math.max(r, Math.max(g, b));
        return max <= 0f ? 0f : (max - Math.min(r, Math.min(g, b))) / max;
    }

    public float getBrightness() {
        return Math.max(r, Math.max(g, b));
    }

    // Sets the colour, opaque, from a hue share of the wheel, a saturation and a brightness, each 0 to 1
    public Color setHSB(float hue, float saturation, float brightness) {

        float sector = (hue - (float) Math.floor(hue)) * 6f;
        int index = (int) sector;
        float part = sector - index;
        float low = brightness * (1f - saturation);
        float falling = brightness * (1f - saturation * part);
        float rising = brightness * (1f - saturation * (1f - part));

        return switch (index) {
            case 0 -> set(brightness, rising, low);
            case 1 -> set(falling, brightness, low);
            case 2 -> set(low, brightness, rising);
            case 3 -> set(low, falling, brightness);
            case 4 -> set(rising, low, brightness);
            default -> set(brightness, low, falling);
        };
    }

    // Packed Color \\

    public float toPackedFloat() {
        int r = (int) (this.r * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int g = (int) (this.g * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int b = (int) (this.b * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int a = (int) (this.a * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int packed = (a << 24) | (b << 16) | (g << 8) | r;
        return Float.intBitsToFloat(packed & 0xfeffffff);
    }

    public Color fromPackedFloat(float packed) {
        int intBits = Float.floatToRawIntBits(packed);
        this.r = (intBits & 0xff) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        this.g = ((intBits >>> 8) & 0xff) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        this.b = ((intBits >>> 16) & 0xff) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        this.a = ((intBits >>> 24) & 0xff) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        return this;
    }

    // Conversion \\

    public static float rgba8888(Color color) {
        return color.toPackedFloat();
    }

    public static float rgba8888(float r, float g, float b, float a) {
        int ri = (int) (r * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int gi = (int) (g * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int bi = (int) (b * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int ai = (int) (a * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int packed = (ai << 24) | (bi << 16) | (gi << 8) | ri;
        return Float.intBitsToFloat(packed & 0xfeffffff);
    }

    // Utility \\

    public boolean hasValues() {
        return r != 0 || g != 0 || b != 0 || a != 1.0f;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Color c)
            return this.r == c.r && this.g == c.g && this.b == c.b && this.a == c.a;
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + Float.hashCode(r);
        result = 31 * result + Float.hashCode(g);
        result = 31 * result + Float.hashCode(b);
        result = 31 * result + Float.hashCode(a);
        return result;
    }

    @Override
    public String toString() {
        return "Color(" + r + ", " + g + ", " + b + ", " + a + ")";
    }
}