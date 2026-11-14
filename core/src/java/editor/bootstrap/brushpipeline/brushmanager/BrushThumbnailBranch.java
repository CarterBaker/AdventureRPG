package editor.bootstrap.brushpipeline.brushmanager;

import java.nio.ByteBuffer;
import java.util.Arrays;

import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.brushpipeline.util.BrushField;
import editor.bootstrap.brushpipeline.util.BrushUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.memory.BufferUtility;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class BrushThumbnailBranch extends BranchPackage {

    /*
     * Main thread — draws each brush's thumbnail the way a paint program's
     * brush docker shows its presets: one stroke of the tip along a single
     * wave across a strip, stamped at the tip's own spacing, every texel
     * holding the most any stamp covers it, density thinning the stamps with
     * a fixed seed so a brush always looks the same. Texels are white with
     * the coverage as alpha, so the row's colour tints them. A texture is
     * made the first time a row asks for one, drawn again only after its
     * brush is rebuilt, and deleted with its brush; rows are bottom first,
     * as every sprite's are.
     */

    // Internal
    private TextureManager textureManager;

    // Thumbnails — one texture per brush ID, 0 until first drawn, and whether it waits to be drawn again
    private IntArrayList brushID2Texture;
    private BooleanArrayList brushID2Stale;

    // Scratch
    private float[] coverage;
    private ByteBuffer texels;
    private int randomState;

    // Base \\

    @Override
    protected void create() {

        int texelCount = EditorSetting.BRUSH_THUMBNAIL_WIDTH * EditorSetting.BRUSH_THUMBNAIL_HEIGHT;

        // Thumbnails
        this.brushID2Texture = new IntArrayList();
        this.brushID2Stale = new BooleanArrayList();

        // Scratch
        this.coverage = new float[texelCount];
        this.texels = BufferUtility.newByteBuffer(texelCount * EngineSetting.COLOR_CHANNEL_COUNT);
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void dispose() {

        for (int i = 0; i < brushID2Texture.size(); i++)
            if (brushID2Texture.getInt(i) != 0)
                textureManager.deleteTexture2D(brushID2Texture.getInt(i));

        brushID2Texture.clear();
        brushID2Stale.clear();
    }

    // Management \\

    // A rebuilt brush is drawn again the next time a row shows it
    void invalidate(int brushID) {

        reserve(brushID);
        brushID2Stale.set(brushID, true);
    }

    void release(int brushID) {

        if (brushID >= brushID2Texture.size() || brushID2Texture.getInt(brushID) == 0)
            return;

        textureManager.deleteTexture2D(brushID2Texture.getInt(brushID));
        brushID2Texture.set(brushID, 0);
        brushID2Stale.set(brushID, true);
    }

    private void reserve(int brushID) {

        while (brushID2Texture.size() <= brushID) {
            brushID2Texture.add(0);
            brushID2Stale.add(true);
        }
    }

    // Accessible \\

    // The brush's thumbnail texture, made or drawn again first when it is missing or stale
    int getThumbnail(BrushHandle brush) {

        int brushID = brush.getBrushID();

        reserve(brushID);

        if (brushID2Texture.getInt(brushID) == 0)
            brushID2Texture.set(brushID, textureManager.createTexture2D(
                    EditorSetting.BRUSH_THUMBNAIL_WIDTH,
                    EditorSetting.BRUSH_THUMBNAIL_HEIGHT,
                    EngineSetting.GL_CLAMP_TO_EDGE,
                    EngineSetting.GL_LINEAR));

        if (brushID2Stale.getBoolean(brushID)) {
            draw(brush, brushID2Texture.getInt(brushID));
            brushID2Stale.set(brushID, false);
        }

        return brushID2Texture.getInt(brushID);
    }

    // Draw \\

    // The tip stamped every spacing along the wave, by distance travelled, then the strip uploaded whole
    private void draw(BrushHandle brush, int texture) {

        int width = EditorSetting.BRUSH_THUMBNAIL_WIDTH;
        int height = EditorSetting.BRUSH_THUMBNAIL_HEIGHT;
        float radius = EditorSetting.BRUSH_THUMBNAIL_RADIUS;
        float startX = radius + 1f;
        float endX = width - radius - 1f;
        float spacing = BrushUtility.resolveSpacing(brush, radius);
        float travelled = spacing;
        float lastX = startX;
        float lastY = resolveWaveY(startX, startX, endX);

        Arrays.fill(coverage, 0f);
        this.randomState = EditorSetting.BRUSH_THUMBNAIL_SEED;

        for (float x = startX; x <= endX; x += EditorSetting.BRUSH_THUMBNAIL_PATH_STEP) {

            float y = resolveWaveY(x, startX, endX);

            travelled += (float) Math.hypot(x - lastX, y - lastY);
            lastX = x;
            lastY = y;

            if (travelled < spacing)
                continue;

            travelled = 0f;
            stamp(brush, x, y, radius);
        }

        upload(texture);
    }

    private float resolveWaveY(float x, float startX, float endX) {

        float height = EditorSetting.BRUSH_THUMBNAIL_HEIGHT;
        double phase = (x - startX) / (endX - startX) * Math.PI * 2.0;

        return height * 0.5f + (float) Math.sin(phase) * height * EditorSetting.BRUSH_THUMBNAIL_WAVE;
    }

    private void stamp(BrushHandle brush, float centerX, float centerY, float radius) {

        int width = EditorSetting.BRUSH_THUMBNAIL_WIDTH;
        int height = EditorSetting.BRUSH_THUMBNAIL_HEIGHT;
        float density = brush.getValue(BrushField.DENSITY);
        int minX = Math.max(0, (int) Math.floor(centerX - radius - 1f));
        int maxX = Math.min(width - 1, (int) Math.ceil(centerX + radius + 1f));
        int minY = Math.max(0, (int) Math.floor(centerY - radius - 1f));
        int maxY = Math.min(height - 1, (int) Math.ceil(centerY + radius + 1f));

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {

                float covered = BrushUtility.resolveCoverage(brush, x + 0.5f - centerX, y + 0.5f - centerY, radius);

                if (covered <= 0f || nextRandom() >= density)
                    continue;

                int index = y * width + x;

                coverage[index] = Math.max(coverage[index], covered);
            }
        }
    }

    private void upload(int texture) {

        int channelMax = EngineSetting.PACKED_COLOR_CHANNEL_MASK;

        texels.clear();

        for (int i = 0; i < coverage.length; i++)
            texels.put((byte) channelMax)
                    .put((byte) channelMax)
                    .put((byte) channelMax)
                    .put((byte) Math.round(coverage[i] * channelMax));

        texels.flip();
        textureManager.updateTexture2D(
                texture, 0, 0, EditorSetting.BRUSH_THUMBNAIL_WIDTH, EditorSetting.BRUSH_THUMBNAIL_HEIGHT, texels);
    }

    // Random \\

    // A xorshift step, uniform in [0, 1), the same sequence for every drawing so a thumbnail never flickers
    private float nextRandom() {

        randomState ^= randomState << 13;
        randomState ^= randomState >>> 17;
        randomState ^= randomState << 5;

        return (randomState >>> Byte.SIZE) * EditorSetting.IMAGE_BRUSH_RANDOM_SCALE;
    }
}
