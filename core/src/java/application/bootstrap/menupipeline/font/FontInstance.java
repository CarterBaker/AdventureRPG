package application.bootstrap.menupipeline.font;

import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector4;

public class FontInstance extends InstancePackage {

    /*
     * Per-label font state: its color material, a glyph layout rebuilt by
     * setText() and cut back to a width behind an ellipsis by fitWidth(), and
     * screen-space instance data rebuilt by prepareComposite() only when
     * scale or position change.
     */

    static final int FLOATS_PER_GLYPH = 8; // screenX,Y,W,H, u0,v0,uw,vh

    // Internal
    private FontHandle handle;
    private MaterialInstance material;

    // Glyph layout — atlas-pixel space, rebuilt by setText
    // Per glyph: cursorX, baselineOffsetY, w, h, u0, v0, uw, vh
    private float[] glyphLayout = new float[0];
    private int glyphCount;

    // Instance data — screen-pixel space, rebuilt by prepareComposite
    private float[] instanceData = new float[0];
    private int instanceDataVersion;

    // Transform cache
    private float lastScreenX = Float.NaN;
    private float lastScreenY = Float.NaN;
    private float lastScale = Float.NaN;
    private boolean textDirty = false;

    // Metrics
    private float textWidth;
    private float textHeight;
    private float textBottom;
    private float textTop;

    // Color
    private final Vector4 color = new Vector4(1f, 1f, 1f, 1f);

    // Font size
    private float fontSize;

    // Constructor \\

    public void constructor(FontHandle handle, MaterialInstance material) {

        this.handle = handle;
        this.material = material;
        this.fontSize = EngineSetting.FONT_RASTER_SIZE;

        UniformStruct<?> colorUniform = material.getUniform(EngineSetting.SPRITE_COLOR_UNIFORM);

        if (colorUniform != null) {
            Object val = colorUniform.attribute().getValue();
            if (val instanceof Vector4 v)
                color.set(v.x, v.y, v.z, v.w);
        }
    }

    // Text \\

    public void setText(String text) {

        glyphCount = 0;
        textWidth = 0f;
        textHeight = 0f;
        textBottom = 0f;
        textTop = 0f;

        if (text == null || text.isEmpty()) {
            textDirty = true;
            return;
        }

        int maxGlyphs = text.length() + EngineSetting.FONT_ELLIPSIS.length();
        if (glyphLayout.length < maxGlyphs * FLOATS_PER_GLYPH)
            glyphLayout = new float[maxGlyphs * FLOATS_PER_GLYPH];

        float cursorX = appendText(text, 0f);

        if (cursorX > 0f)
            cursorX -= letterSpacing();
        textWidth = cursorX;
        textDirty = true;
        lastScale = Float.NaN;
    }

    // Cuts the laid-out text back to the last glyph that leaves room for the ellipsis within the width, in atlas
    // pixels, and lays the ellipsis after it; text that already fits is left whole
    public void fitWidth(float maxWidth) {

        if (textWidth <= maxWidth)
            return;

        float letterSpacing = letterSpacing();
        float ellipsisWidth = measureText(EngineSetting.FONT_ELLIPSIS, letterSpacing);
        int kept = glyphCount;
        float cursorX = 0f;

        while (kept > 0) {

            int base = (kept - 1) * FLOATS_PER_GLYPH;
            cursorX = glyphLayout[base] + glyphLayout[base + 2] + letterSpacing;

            if (cursorX + ellipsisWidth <= maxWidth)
                break;

            kept--;
            cursorX = 0f;
        }

        glyphCount = kept;
        textTop = 0f;
        textBottom = 0f;

        for (int i = 0; i < glyphCount; i++)
            spanGlyph(i);

        cursorX = appendText(EngineSetting.FONT_ELLIPSIS, cursorX);

        textWidth = cursorX > 0f ? cursorX - letterSpacing : 0f;
        textDirty = true;
        lastScale = Float.NaN;
    }

    // Lays each glyph of the text out from the cursor and returns the cursor after it, one letter space past the last
    private float appendText(String text, float cursorX) {

        float letterSpacing = letterSpacing();

        for (int i = 0; i < text.length();) {

            int codepoint = text.codePointAt(i);
            i += Character.charCount(codepoint);

            if (codepoint == ' ') {
                cursorX += spaceAdvance() + letterSpacing;
                continue;
            }

            GlyphMetricStruct metric = handle.getGlyph(codepoint);
            TextureHandle glyphHandle = handle.getGlyphHandle(codepoint);

            if (metric == null || glyphHandle == null || metric.width <= 0 || metric.height <= 0)
                continue;

            int base = glyphCount * FLOATS_PER_GLYPH;
            glyphLayout[base] = cursorX + metric.bearingX;
            glyphLayout[base + 1] = metric.bearingY - metric.height;
            glyphLayout[base + 2] = metric.width;
            glyphLayout[base + 3] = metric.height;
            glyphLayout[base + 4] = glyphHandle.getU0();
            glyphLayout[base + 5] = glyphHandle.getV0();
            glyphLayout[base + 6] = glyphHandle.getU1() - glyphHandle.getU0();
            glyphLayout[base + 7] = glyphHandle.getV1() - glyphHandle.getV0();

            spanGlyph(glyphCount);
            cursorX += metric.advance + letterSpacing;
            glyphCount++;
        }

        textHeight = glyphCount > 0 ? textTop - textBottom : 0f;

        return cursorX;
    }

    // The width the text takes laid out, in atlas pixels, one letter space past its last glyph
    private float measureText(String text, float letterSpacing) {

        float width = 0f;

        for (int i = 0; i < text.length();) {

            int codepoint = text.codePointAt(i);
            i += Character.charCount(codepoint);

            GlyphMetricStruct metric = handle.getGlyph(codepoint);

            if (codepoint == ' ')
                width += spaceAdvance() + letterSpacing;
            else if (metric != null && handle.getGlyphHandle(codepoint) != null)
                width += metric.advance + letterSpacing;
        }

        return width;
    }

    // The text's vertical span widened to take in one laid-out glyph
    private void spanGlyph(int glyph) {

        int base = glyph * FLOATS_PER_GLYPH;
        float glyphBottom = glyphLayout[base + 1];
        float glyphTop = glyphBottom + glyphLayout[base + 3];

        if (glyph == 0 || glyphBottom < textBottom)
            textBottom = glyphBottom;
        if (glyph == 0 || glyphTop > textTop)
            textTop = glyphTop;
    }

    private float letterSpacing() {
        return handle.getRasterPixelSize() * EngineSetting.FONT_LETTER_SPACING_RATIO;
    }

    private float spaceAdvance() {

        GlyphMetricStruct space = handle.getGlyph(' ');

        return space != null ? space.advance : handle.getRasterPixelSize() * EngineSetting.FONT_SPACE_ADVANCE_RATIO;
    }

    // Composite Preparation \\

    public void prepareComposite(float screenX, float screenY, float scale) {

        if (!textDirty && screenX == lastScreenX && screenY == lastScreenY && scale == lastScale)
            return;

        if (instanceData.length < glyphCount * FLOATS_PER_GLYPH)
            instanceData = new float[glyphCount * FLOATS_PER_GLYPH];

        for (int i = 0; i < glyphCount; i++) {

            int idx = i * FLOATS_PER_GLYPH;

            instanceData[idx] = screenX + glyphLayout[idx] * scale;
            instanceData[idx + 1] = screenY + glyphLayout[idx + 1] * scale;
            instanceData[idx + 2] = glyphLayout[idx + 2] * scale;
            instanceData[idx + 3] = glyphLayout[idx + 3] * scale;
            instanceData[idx + 4] = glyphLayout[idx + 4]; // u0
            instanceData[idx + 5] = glyphLayout[idx + 5]; // v0
            instanceData[idx + 6] = glyphLayout[idx + 6]; // uw
            instanceData[idx + 7] = glyphLayout[idx + 7]; // vh
        }

        lastScreenX = screenX;
        lastScreenY = screenY;
        lastScale = scale;
        textDirty = false;
        instanceDataVersion++;
    }

    // Color \\

    public void setColor(float r, float g, float b, float a) {
        color.set(r, g, b, a);
        material.setUniform(EngineSetting.SPRITE_COLOR_UNIFORM, color);
    }

    public Vector4 getColor() {
        return color;
    }

    // Font Size \\

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public float getFontSize() {
        return fontSize;
    }

    // Accessible \\

    public FontHandle getHandle() {
        return handle;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public float[] getInstanceData() {
        return instanceData;
    }

    public int getGlyphCount() {
        return glyphCount;
    }

    public int getInstanceDataVersion() {
        return instanceDataVersion;
    }

    public float getTextWidth() {
        return textWidth;
    }

    public float getTextHeight() {
        return textHeight;
    }

    public float getTextBottom() {
        return textBottom;
    }

    public boolean hasGlyphs() {
        return glyphCount > 0;
    }
}