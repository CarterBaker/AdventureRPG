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
     * setText(), and screen-space instance data rebuilt by prepareComposite()
     * only when scale or position change.
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

        if (text == null || text.isEmpty()) {
            textDirty = true;
            return;
        }

        int maxGlyphs = text.length();
        if (glyphLayout.length < maxGlyphs * FLOATS_PER_GLYPH)
            glyphLayout = new float[maxGlyphs * FLOATS_PER_GLYPH];

        float rasterPixelSize = handle.getRasterPixelSize();
        float letterSpacing = rasterPixelSize * EngineSetting.FONT_LETTER_SPACING_RATIO;
        float cursorX = 0f;
        float textTop = 0f;

        for (int i = 0; i < text.length();) {

            int codepoint = text.codePointAt(i);
            i += Character.charCount(codepoint);

            if (codepoint == ' ') {
                GlyphMetricStruct space = handle.getGlyph(codepoint);
                cursorX += (space != null ? space.advance : rasterPixelSize * EngineSetting.FONT_SPACE_ADVANCE_RATIO)
                        + letterSpacing;
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

            cursorX += metric.advance + letterSpacing;

            float glyphBottom = metric.bearingY - metric.height;

            if (glyphCount == 0 || glyphBottom < textBottom)
                textBottom = glyphBottom;
            if (glyphCount == 0 || metric.bearingY > textTop)
                textTop = metric.bearingY;

            glyphCount++;
        }

        textHeight = glyphCount > 0 ? textTop - textBottom : 0f;

        if (cursorX > 0f)
            cursorX -= letterSpacing;
        textWidth = cursorX;
        textDirty = true;
        lastScale = Float.NaN;
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