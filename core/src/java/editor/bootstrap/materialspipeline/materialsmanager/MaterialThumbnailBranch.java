package editor.bootstrap.materialspipeline.materialsmanager;

import java.nio.ByteBuffer;
import java.util.Arrays;

import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.memory.BufferUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

class MaterialThumbnailBranch extends BranchPackage {

    /*
     * Makes the thumbnail every material of the Materials tab is listed with,
     * as Substance and Blender show a material library: the material's albedo
     * image read from its pack once, shrunk by averaging every texel that
     * falls in each thumbnail texel, and uploaded as an editor texture kept
     * for as long as the editor runs. The image itself is let go at once, so
     * only the small textures stay. Thumbnails are made one call at a time,
     * so the tab builds them a few a frame instead of reading every pack
     * image in one stall.
     */

    // Internal
    private TextureManager textureManager;
    private MaterialSampleBranch materialSampleBranch;

    // Thumbnails — one texture per material name, made the first time it is asked for
    private Object2IntOpenHashMap<String> materialName2Thumbnail;

    // Scratch
    private ByteBuffer texels;
    private long[] sums;

    // Base \\

    @Override
    protected void create() {

        int texelCount = EditorSetting.MATERIALS_THUMBNAIL_SIZE * EditorSetting.MATERIALS_THUMBNAIL_SIZE;

        // Thumbnails
        this.materialName2Thumbnail = new Object2IntOpenHashMap<>();

        // Scratch
        this.texels = BufferUtility.newByteBuffer(texelCount * EngineSetting.COLOR_CHANNEL_COUNT);
        this.sums = new long[texelCount * (EngineSetting.COLOR_CHANNEL_COUNT + 1)];
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.materialSampleBranch = get(MaterialSampleBranch.class);
    }

    @Override
    protected void dispose() {

        for (int texture : materialName2Thumbnail.values())
            textureManager.deleteTexture2D(texture);

        materialName2Thumbnail.clear();
    }

    // Accessible \\

    boolean hasThumbnail(String materialName) {
        return materialName2Thumbnail.containsKey(materialName);
    }

    // The material's thumbnail texture, made from its albedo image the first time it is asked for
    int resolveThumbnail(String materialName) {

        if (materialName2Thumbnail.containsKey(materialName))
            return materialName2Thumbnail.getInt(materialName);

        int size = EditorSetting.MATERIALS_THUMBNAIL_SIZE;
        int texture = textureManager.createTexture2D(
                size, size, EngineSetting.GL_CLAMP_TO_EDGE, EngineSetting.GL_LINEAR);

        shrink(materialSampleBranch.readMap(materialName, EditorSetting.MATERIALS_THUMBNAIL_MAP));
        textureManager.updateTexture2D(texture, 0, 0, size, size, texels);
        materialName2Thumbnail.put(materialName, texture);

        return texture;
    }

    // Draw \\

    // Every texel of the image averaged into the thumbnail texel it falls in, written into texels, rows bottom first
    private void shrink(Pixmap image) {

        int size = EditorSetting.MATERIALS_THUMBNAIL_SIZE;
        int channels = EngineSetting.COLOR_CHANNEL_COUNT;
        int stride = channels + 1;
        int width = image.getWidth();
        int height = image.getHeight();

        Arrays.fill(sums, 0L);

        for (int y = 0; y < Math.max(height, size); y++)
            for (int x = 0; x < Math.max(width, size); x++) {

                int rgba = image.getPixel(x * width / Math.max(width, size), y * height / Math.max(height, size));
                int cell = ((size - 1 - y * size / Math.max(height, size)) * size + x * size / Math.max(width, size))
                        * stride;

                sums[cell] += rgba >>> 24;
                sums[cell + 1] += (rgba >>> 16) & EngineSetting.PACKED_COLOR_CHANNEL_MASK;
                sums[cell + 2] += (rgba >>> 8) & EngineSetting.PACKED_COLOR_CHANNEL_MASK;
                sums[cell + 3] += rgba & EngineSetting.PACKED_COLOR_CHANNEL_MASK;
                sums[cell + channels]++;
            }

        image.dispose();
        texels.clear();

        for (int cell = 0; cell < size * size; cell++) {

            long count = Math.max(1L, sums[cell * stride + channels]);

            for (int c = 0; c < channels; c++)
                texels.put((byte) (sums[cell * stride + c] / count));
        }

        texels.flip();
    }
}
