package com.internal.core.util.mathematics.Extras;

public class Color {

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

    public com.badlogic.gdx.graphics.Color toGdx() {
        return new com.badlogic.gdx.graphics.Color(r, g, b, a);
    }

    public Color fromGDX(com.badlogic.gdx.graphics.Color other) {
        return set(other.r, other.g, other.b, other.a);
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

        if (r == 0 || g == 0 || b == 0 || a == 0) // TODO: make my own error
            throw new ArithmeticException("Division by zero");

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

    // Packed Color \\

    public float toPackedFloat() {
        int r = (int) (this.r * 255.0f);
        int g = (int) (this.g * 255.0f);
        int b = (int) (this.b * 255.0f);
        int a = (int) (this.a * 255.0f);

        int packed = (a << 24) | (b << 16) | (g << 8) | r;
        return Float.intBitsToFloat(packed & 0xfeffffff);
    }

    public Color fromPackedFloat(float packed) {
        int intBits = Float.floatToRawIntBits(packed);

        this.r = (intBits & 0xff) / 255.0f;
        this.g = ((intBits >>> 8) & 0xff) / 255.0f;
        this.b = ((intBits >>> 16) & 0xff) / 255.0f;
        this.a = ((intBits >>> 24) & 0xff) / 255.0f;

        return this;
    }

    // Conversion \\

    public static float rgba8888(Color color) {
        return color.toPackedFloat();
    }

    public static float rgba8888(float r, float g, float b, float a) {
        int ri = (int) (r * 255.0f);
        int gi = (int) (g * 255.0f);
        int bi = (int) (b * 255.0f);
        int ai = (int) (a * 255.0f);

        int packed = (ai << 24) | (bi << 16) | (gi << 8) | ri;
        return Float.intBitsToFloat(packed & 0xfeffffff);
    }

    // Utility \\

    public boolean hasValues() {
        return r != 0 ||
                g != 0 ||
                b != 0 ||
                a != 1.0f;
    }

    // Java \\

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Color) {

            Color c = (Color) obj;
            return this.r == c.r &&
                    this.g == c.g &&
                    this.b == c.b &&
                    this.a == c.a;
        }

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